@file:OptIn(ExperimentalEncodingApi::class)

package work.socialhub.kgrpc.rpc

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readString
import work.socialhub.kgrpc.CallInterceptor
import work.socialhub.kgrpc.Channel
import work.socialhub.kgrpc.Code
import work.socialhub.kgrpc.MethodDescriptor
import work.socialhub.kgrpc.Status
import work.socialhub.kgrpc.StatusException
import work.socialhub.kgrpc.message.Message
import work.socialhub.kgrpc.message.MessageCompanion
import work.socialhub.kgrpc.metadata.Entry
import work.socialhub.kgrpc.metadata.Key
import work.socialhub.kgrpc.metadata.Metadata
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val base64 = Base64.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

suspend fun <REQ : Message, RESP : Message> unaryRpc(
    channel: Channel,
    path: String,
    request: REQ,
    responseDeserializer: MessageCompanion<RESP>,
    headers: Metadata,
    deadlineAfter: Duration? = null
): RESP {
    val methodDescriptor = MethodDescriptor(
        fullMethodName = path,
        methodType = MethodDescriptor.MethodType.UNARY
    )

    return unaryResponseCallBaseImplementation(channel) {
        grpcImplementation(channel, path, request, responseDeserializer, headers, deadlineAfter, methodDescriptor)
            .singleOrStatus()
    }
}

fun <REQ : Message, RESP : Message> serverStreamingRpc(
    channel: Channel,
    path: String,
    request: REQ,
    responseDeserializer: MessageCompanion<RESP>,
    headers: Metadata,
    deadlineAfter: Duration? = null
): Flow<RESP> {
    val methodDescriptor = MethodDescriptor(
        fullMethodName = path,
        methodType = MethodDescriptor.MethodType.SERVER_STREAMING
    )

    return streamingResponseCallBaseImplementation(
        channel = channel,
        responseFlow = grpcImplementation(channel, path, request, responseDeserializer, headers, deadlineAfter, methodDescriptor)
    )
}

fun <REQ : Message, RESP : Message> clientStreamingRpc(
    channel: Channel,
    path: String,
    request: REQ,
    responseDeserializer: MessageCompanion<RESP>,
    headers: Metadata,
    deadlineAfter: Duration? = null
): Nothing {
    throw StatusException(
        status = Status(Code.UNIMPLEMENTED, "Client streaming is not supported on JS platform."),
        cause = null
    )
}

fun <REQ : Message, RESP : Message> bidiStreamingRpc(
    channel: Channel,
    path: String,
    request: REQ,
    responseDeserializer: MessageCompanion<RESP>,
    headers: Metadata,
    deadlineAfter: Duration? = null
): Nothing {
    throw StatusException(
        status = Status(Code.UNIMPLEMENTED, "Bidirectional streaming is not supported on JS platform."),
        cause = null
    )
}

// -- Base implementations with shutdown handling --

private suspend fun <RESP> unaryResponseCallBaseImplementation(
    channel: Channel,
    performCall: suspend () -> RESP
): RESP {
    if (channel.isShutdownState.value) throw StatusException.UnavailableDueToShutdown

    return coroutineScope {
        val waitForCancellationJob = launch {
            channel.isShutdownImmediately.first { it }
        }

        val responseDeferred = async {
            if (channel.isShutdownState.value) throw StatusException.UnavailableDueToShutdown
            performCall()
        }

        select {
            waitForCancellationJob.onJoin {
                responseDeferred.cancel("Cancelled due to channel shutdown")
                throw StatusException.CancelledDueToShutdown
            }

            responseDeferred.onAwait { result ->
                waitForCancellationJob.cancel()
                result
            }
        }
    }
}

private fun <RESP> streamingResponseCallBaseImplementation(
    channel: Channel,
    responseFlow: Flow<RESP>
): Flow<RESP> {
    return channelFlow {
        val emitJob = launch {
            if (channel.isShutdownState.value) throw StatusException.UnavailableDueToShutdown
            responseFlow.collect(::send)
        }

        val awaitShutdownJob = launch {
            channel.isShutdownImmediately.first { it }
        }

        select {
            emitJob.onJoin {
                awaitShutdownJob.cancel()
            }

            awaitShutdownJob.onJoin {
                emitJob.cancel()
                throw StatusException.CancelledDueToShutdown
            }
        }
    }
}

// -- gRPC-Web protocol implementation --

private fun <REQ : Message, RESP : Message> grpcImplementation(
    channel: Channel,
    path: String,
    request: REQ,
    deserializer: MessageCompanion<RESP>,
    metadata: Metadata,
    deadlineAfter: Duration?,
    methodDescriptor: MethodDescriptor
): Flow<RESP> {
    return flow {
        channel.registerRpc()

        try {
            val actualHeaders = channel.interceptors.foldRight(metadata) { interceptor, currentMetadata ->
                interceptor.onStart(methodDescriptor, currentMetadata)
            }

            val actualRequest = channel.interceptors.foldRight(request) { interceptor, currentRequest ->
                interceptor.onSendMessage(methodDescriptor, currentRequest)
            }

            channel.client
                .preparePost(channel.connectionString + path) {
                    header(HttpHeaders.ContentType, "application/grpc-web+proto")
                    header("X-Grpc-Web", "1")

                    actualHeaders.entries.forEach { entry ->
                        when (entry) {
                            is Entry.Ascii -> {
                                val value = entry.values.joinToString()
                                header(entry.key.name, value)
                            }
                            is Entry.Binary -> {
                                entry.values.forEach {
                                    header(entry.key.name, base64.encode(it))
                                }
                            }
                        }
                    }

                    if (deadlineAfter != null) {
                        timeout {
                            requestTimeoutMillis = deadlineAfter.toLong(DurationUnit.MILLISECONDS)
                        }
                    }

                    setBody(encodeMessageFrame(actualRequest))
                }
                .execute { response ->
                    val headers = decodeHeaders(response.headers)

                    if (!response.status.isSuccess()) {
                        throw StatusException(
                            Status(Code.UNAVAILABLE, "Unsuccessful http request. HTTP-Code=${response.status}"),
                            null
                        )
                    }

                    val finalHeaders = channel.interceptors.fold(headers) { currentHeaders, interceptor ->
                        interceptor.onReceiveHeaders(methodDescriptor, currentHeaders)
                    }

                    extractStatusFromMetadataAndVerify(metadata = finalHeaders)

                    readResponse(
                        channel = response.bodyAsChannel(),
                        methodDescriptor = methodDescriptor,
                        deserializer = deserializer,
                        interceptors = channel.interceptors
                    )
                }
        } catch (e: StatusException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            if (deadlineAfter != null) {
                throw StatusException.requestTimeout(deadlineAfter, e)
            } else {
                throw StatusException.internal("Unexpected timeout exception caught.", e)
            }
        } catch (t: Throwable) {
            throw StatusException(
                status = Status(
                    code = Code.UNAVAILABLE,
                    statusMessage = "Could not create rpc."
                ),
                cause = t
            )
        } finally {
            channel.unregisterRpc()
        }
    }
}

private suspend fun <T : Message> FlowCollector<T>.readResponse(
    channel: ByteReadChannel,
    methodDescriptor: MethodDescriptor,
    deserializer: MessageCompanion<T>,
    interceptors: List<CallInterceptor>
) {
    while (!channel.exhausted()) {
        val frame = channel.readBuffer(5)

        if (frame.size == 0L) break

        val flag = frame.readByte().toUByte()
        val length = frame.readInt()

        val payload = channel.readBuffer(length)

        if (flag == 0.toUByte()) {
            val receivedMessage = deserializer.deserialize(payload.readByteArray())

            val actualMessage = interceptors.fold(receivedMessage) { currentMessage, interceptor ->
                interceptor.onReceiveMessage(methodDescriptor, currentMessage)
            }

            emit(actualMessage)
        } else if (flag == 0x80.toUByte()) {
            val headers = decodeHeadersFrame(payload)

            extractStatusFromMetadataAndVerify(
                metadata = headers,
                runInterceptors = { status ->
                    interceptors
                        .fold(Pair(status, headers)) { (currentStatus, currentMetadata), interceptor ->
                            interceptor.onClose(methodDescriptor, currentStatus, currentMetadata)
                        }
                        .first
                }
            )
        }
    }
}

// -- gRPC-Web framing --

private fun encodeMessageFrame(message: Message): ByteArray {
    val sink = Buffer()

    val data = message.serialize()
    val size = data.size
    sink.writeByte(0)
    sink.writeInt(size)
    sink.write(data)

    return sink.readByteArray()
}

private fun decodeHeaders(headers: Headers): Metadata {
    val entries = headers.entries().map { (key, values) ->
        val valuesSplit = values.flatMap { it.split(", ") }
        when (val k = Key.fromName(key)) {
            is Key.AsciiKey -> {
                Entry.Ascii(k, valuesSplit.toSet())
            }
            is Key.BinaryKey -> {
                Entry.Binary(k, valuesSplit.map { base64.decode(it) }.toSet())
            }
        }
    }

    return Metadata.of(entries)
}

private fun decodeHeadersFrame(source: Source): Metadata {
    val metadataString = source.readString()

    val entries = metadataString
        .split("\r\n")
        .filter { entry -> entry.isNotBlank() && entry.count { it == ':' } == 1 }
        .map { metadataEntry ->
            val keyName = metadataEntry.substringBefore(':')
            val value = metadataEntry.substringAfter(':')

            when (val k = Key.fromName(keyName)) {
                is Key.AsciiKey -> {
                    Entry.Ascii(k, setOf(value))
                }
                is Key.BinaryKey -> {
                    val decodedValue = Base64.decode(value)
                    Entry.Binary(k, setOf(decodedValue))
                }
            }
        }

    return Metadata.of(entries)
}
