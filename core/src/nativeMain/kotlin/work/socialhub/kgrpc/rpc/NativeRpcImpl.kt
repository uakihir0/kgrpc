package work.socialhub.kgrpc.rpc

import cnames.structs.RustMetadata
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import work.socialhub.kgrpc.*
import work.socialhub.kgrpc.message.Message
import work.socialhub.kgrpc.message.MessageCompanion
import work.socialhub.kgrpc.metadata.Entry
import work.socialhub.kgrpc.metadata.Key
import work.socialhub.kgrpc.metadata.Metadata
import work.socialhub.kgrpc.native.*
import kotlin.time.Duration
import kotlinx.coroutines.channels.Channel as CoroutineChannel

suspend fun <REQ : Message, RESP : Message> unaryRpc(
    channel: Channel,
    path: String,
    request: REQ,
    responseDeserializer: MessageCompanion<RESP>,
    headers: Metadata,
    deadlineAfter: Duration? = null
): RESP {
    return unaryResponseCallBaseImplementation(channel) {
        rpcImplementation(
            channel = channel,
            methodType = MethodDescriptor.MethodType.UNARY,
            path = path,
            requests = flowOf(request),
            responseDeserializer = responseDeserializer,
            headers = headers,
            deadlineAfter = deadlineAfter
        ).singleOrStatus()
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
    return streamingResponseCallBaseImplementation(
        channel = channel,
        responseFlow = rpcImplementation(
            channel = channel,
            methodType = MethodDescriptor.MethodType.SERVER_STREAMING,
            path = path,
            requests = flowOf(request),
            responseDeserializer = responseDeserializer,
            headers = headers,
            deadlineAfter = deadlineAfter
        )
    )
}

suspend fun <REQ : Message, RESP : Message> clientStreamingRpc(
    channel: Channel,
    path: String,
    requests: Flow<REQ>,
    responseDeserializer: MessageCompanion<RESP>,
    headers: Metadata,
    deadlineAfter: Duration? = null
): RESP {
    return unaryResponseCallBaseImplementation(channel) {
        rpcImplementation(
            channel = channel,
            methodType = MethodDescriptor.MethodType.CLIENT_STREAMING,
            path = path,
            requests = requests,
            responseDeserializer = responseDeserializer,
            headers = headers,
            deadlineAfter = deadlineAfter
        ).singleOrStatus()
    }
}

fun <REQ : Message, RESP : Message> bidiStreamingRpc(
    channel: Channel,
    path: String,
    requests: Flow<REQ>,
    responseDeserializer: MessageCompanion<RESP>,
    headers: Metadata,
    deadlineAfter: Duration? = null
): Flow<RESP> {
    return streamingResponseCallBaseImplementation(
        channel = channel,
        responseFlow = rpcImplementation(
            channel = channel,
            methodType = MethodDescriptor.MethodType.BIDI_STREAMING,
            path = path,
            requests = requests,
            responseDeserializer = responseDeserializer,
            headers = headers,
            deadlineAfter = deadlineAfter
        )
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

// -- RPC implementation delegating to Rust/Tonic --

@OptIn(ExperimentalCoroutinesApi::class)
private fun <REQ : Message, RESP : Message> rpcImplementation(
    channel: Channel,
    methodType: MethodDescriptor.MethodType,
    path: String,
    requests: Flow<REQ>,
    responseDeserializer: MessageCompanion<RESP>,
    headers: Metadata,
    deadlineAfter: Duration?
): Flow<RESP> {
    val methodDescriptor = MethodDescriptor(fullMethodName = path, methodType = methodType)

    val rpcFlow = channelFlow {
        if (channel.isShutdownState.value) throw StatusException.CancelledDueToShutdown

        channel.registerRpc()

        val actualMetadata = channel.interceptors.foldRight(headers) { interceptor, currentMetadata ->
            interceptor.onStart(methodDescriptor, currentMetadata)
        }

        val contextData = CallContextData(responseDeserializer, channel)
        val requestChannel = request_channel_create()
        val callMetadata = createRustMetadata(actualMetadata)

        val callContextData = StableRef.create(contextData)

        val sendJob = launch {
            try {
                requests
                    .map { req ->
                        channel.interceptors.foldRight(req) { interceptor, msg ->
                            interceptor.onSendMessage(methodDescriptor, msg)
                        }
                    }
                    .collect { req ->
                        while (true) {
                            val sendResult =
                                request_channel_send(requestChannel, StableRef.create(req).asCPointer())

                            @Suppress("REDUNDANT_ELSE_IN_WHEN")
                            when (sendResult) {
                                Ok -> break
                                Closed, NoSender -> {
                                    return@collect
                                }
                                Full -> {
                                    delay(5)
                                }
                                else -> throw IllegalStateException("Unknown send result $sendResult")
                            }
                        }
                    }
            } finally {
                request_channel_signal_end(requestChannel)
                request_channel_free(requestChannel)
            }
        }

        val receiveMessagesJob = launch {
            contextData
                .messageReceiveChannel
                .receiveAsFlow()
                .map { msg ->
                    channel.interceptors.fold(msg) { currentMsg, interceptor ->
                        interceptor.onReceiveMessage(methodDescriptor, currentMsg)
                    }
                }
                .collect {
                    send(it)
                }
        }

        val waitForInitialMetadataDeferred = async {
            val initialMetadata = contextData.initialMetadataCompletable.await()

            val metadata = channel.interceptors.fold(initialMetadata) { currentMetadata, interceptor ->
                interceptor.onReceiveHeaders(methodDescriptor, currentMetadata)
            }

            extractStatusFromMetadataAndVerify(metadata)

            metadata
        }

        val waitForDoneJob = launch {
            val resultStatus = contextData.callStatusCompletable.await()

            if (resultStatus.status != null && resultStatus.status.code != Code.OK) {
                throw StatusException(resultStatus.status, null)
            }

            try {
                val initialMetadata = try {
                    waitForInitialMetadataDeferred.getCompleted()
                } catch (_: IllegalStateException) {
                    Metadata.empty()
                }

                val finalMetadata = initialMetadata + resultStatus.trailers
                extractStatusFromMetadataAndVerify(finalMetadata) { statusFromMetadata ->
                    channel.interceptors
                        .fold(Pair(statusFromMetadata, finalMetadata)) { (currentStatus, currentMetadata), interceptor ->
                            interceptor.onClose(methodDescriptor, currentStatus, currentMetadata)
                        }
                        .first
                }
            } finally {
                close()
            }
        }

        val taskHandle = rpc_implementation(
            channel = channel.channel,
            path = path,
            metadata = callMetadata,
            request_channel = requestChannel,
            user_data = callContextData.asCPointer(),
            serialize_request = staticCFunction { message ->
                val messageRef = message!!.asStableRef<Message>()
                try {
                    val msg = messageRef.get()

                    if (msg.requiredSize == 0) {
                        c_byte_array_create(
                            data = null,
                            ptr = null,
                            len = 0uL,
                            free = staticCFunction { _ -> }
                        )
                    } else {
                        val array = msg.serialize().toUByteArray()
                        val pinnedArray = array.pin()

                        c_byte_array_create(
                            data = StableRef.create(pinnedArray).asCPointer(),
                            ptr = pinnedArray.addressOf(0),
                            len = array.size.toULong(),
                            free = staticCFunction { data ->
                                val ref = data!!.asStableRef<Pinned<UByteArray>>()
                                ref.get().unpin()
                                ref.dispose()
                            }
                        )
                    }
                } finally {
                    messageRef.dispose()
                }
            },
            deserialize_response = staticCFunction { data, ptr, length ->
                if (data == null) return@staticCFunction null

                val context = data.asStableRef<CallContextData<*>>().get()

                @Suppress("UNCHECKED_CAST")
                val deserializer = context.deserializer as MessageCompanion<Message>

                val message = if (length > 0uL && ptr != null) {
                    val bytes = ByteArray(length.toInt()) { i -> ptr[i].toByte() }
                    deserializer.deserialize(bytes)
                } else {
                    deserializer.deserialize(byteArrayOf())
                }

                StableRef.create(message).asCPointer()
            },
            on_message_received = staticCFunction { data, message ->
                if (data == null) return@staticCFunction

                val callData = data.asStableRef<CallContextData<*>>().get()
                val msg = message!!.asStableRef<Message>()

                try {
                    @Suppress("UNCHECKED_CAST")
                    (callData.messageReceiveChannel as CoroutineChannel<Message>).trySend(msg.get())
                } finally {
                    msg.dispose()
                }
            },
            on_initial_metadata_received = staticCFunction { data, initialMetadata ->
                if (data == null) return@staticCFunction

                try {
                    val callData = data.asStableRef<CallContextData<*>>().get()
                    callData.initialMetadataCompletable.complete(convertRustMetadata(initialMetadata))
                } finally {
                    metadata_free(initialMetadata)
                }
            },
            on_done = staticCFunction { data, code, message, metadata, trailers ->
                if (data == null) return@staticCFunction
                val dataRef = data.asStableRef<CallContextData<*>>()

                try {
                    val status: Status? = if (code == -1) {
                        null
                    } else {
                        Status(
                            code = Code.getCodeForValue(code),
                            statusMessage = message?.toKString().orEmpty()
                        )
                    }

                    dataRef.get().callStatusCompletable.complete(
                        CallCompletionData(
                            status = status,
                            trailers = convertRustMetadata(trailers)
                        )
                    )
                } finally {
                    string_free(message)
                    metadata_free(metadata)
                    metadata_free(trailers)

                    try {
                        dataRef.get().channel.unregisterRpc()
                    } finally {
                        dataRef.dispose()
                    }
                }
            }
        )

        awaitClose {
            val closeMessage = "The call has been finalized"

            contextData.close()
            sendJob.cancel(closeMessage)
            receiveMessagesJob.cancel(closeMessage)
            waitForDoneJob.cancel(closeMessage)
            waitForInitialMetadataDeferred.cancel(closeMessage)

            rpc_task_abort(taskHandle)
        }
    }.flowOn(channel.context)

    return flow {
        if (deadlineAfter != null) {
            try {
                withTimeout(deadlineAfter) {
                    emitAll(rpcFlow)
                }
            } catch (e: TimeoutCancellationException) {
                throw StatusException.requestTimeout(deadlineAfter, e)
            }
        } else {
            emitAll(rpcFlow)
        }
    }
}

// -- Helper data classes --

private data class CallContextData<RESP : Message>(
    val deserializer: MessageCompanion<RESP>,
    val channel: Channel,
    val messageReceiveChannel: CoroutineChannel<RESP> = CoroutineChannel(capacity = CoroutineChannel.UNLIMITED),
    val callStatusCompletable: CompletableDeferred<CallCompletionData> = CompletableDeferred(),
    val initialMetadataCompletable: CompletableDeferred<Metadata> = CompletableDeferred(),
) {
    fun close() {
        messageReceiveChannel.close()
    }
}

private data class CallCompletionData(
    val status: Status?,
    val trailers: Metadata
)

// -- Metadata conversion --

private fun createRustMetadata(metadata: Metadata): CPointer<RustMetadata>? {
    return memScoped {
        val asciiCStrings = metadata.entries
            .filterIsInstance<Entry.Ascii>()
            .flatMap { (key, values) ->
                values.flatMap { value -> listOf(key.name, value) }
            }
            .map { it.cstr.ptr } + null

        val binaryEntries = metadata.entries
            .filterIsInstance<Entry.Binary>()

        val rustMetadata = if (binaryEntries.isNotEmpty()) {
            val binaryCStrings = binaryEntries
                .flatMap { entry -> entry.values.map { entry.key.name } }
                .map { it.cstr.ptr } + null

            val binaryByteArrays: List<Pinned<UByteArray>> = binaryEntries
                .flatMap { entry -> entry.values.map { it.toUByteArray().pin() } }

            val binaryPointers = binaryByteArrays.map {
                if (it.get().isEmpty()) null else it.addressOf(0)
            }.toCValues()

            val binarySizesPinned =
                binaryByteArrays.map { it.get().size.toULong() }.toULongArray().pin()
            val binarySizes = binarySizesPinned.addressOf(0)

            val result = metadata_create(
                ascii_entries = asciiCStrings.toCValues(),
                binary_keys = binaryCStrings.toCValues(),
                binary_ptrs = binaryPointers,
                binary_sizes = binarySizes
            )

            binaryByteArrays.forEach { it.unpin() }
            binarySizesPinned.unpin()

            result
        } else {
            metadata_create(asciiCStrings.toCValues(), null, null, null)
        }

        rustMetadata
    }
}

private fun convertRustMetadata(rustMetadata: CPointer<RustMetadata>?): Metadata {
    val entries = buildList {
        val ref: StableRef<MutableList<Entry<*>>> = StableRef.create(this)

        try {
            metadata_iterate(
                metadata = rustMetadata,
                data = ref.asCPointer(),
                block_ascii = staticCFunction { data, key, value ->
                    try {
                        val keyString = key?.toKString() ?: return@staticCFunction
                        val valueString = value?.toKString() ?: return@staticCFunction

                        val list = data!!.asStableRef<MutableList<Entry<*>>>().get()
                        list += Entry.Ascii(Key.AsciiKey(keyString), setOf(valueString))
                    } finally {
                        string_free(key)
                        string_free(value)
                    }
                },
                block_binary = staticCFunction { data, key, valuePtr, valueSize ->
                    try {
                        val keyString = key?.toKString() ?: return@staticCFunction
                        val array = if (valueSize == 0uL || valuePtr == null) {
                            byteArrayOf()
                        } else {
                            ByteArray(valueSize.toInt()) { i ->
                                valuePtr[i].toByte()
                            }
                        }

                        val list = data!!.asStableRef<MutableList<Entry<*>>>().get()
                        list += Entry.Binary(Key.BinaryKey(keyString), setOf(array))
                    } finally {
                        string_free(key)
                    }
                }
            )
        } finally {
            ref.dispose()
        }
    }

    return Metadata.of(entries)
}
