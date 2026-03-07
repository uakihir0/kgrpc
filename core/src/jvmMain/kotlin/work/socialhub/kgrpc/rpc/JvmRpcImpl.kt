package work.socialhub.kgrpc.rpc

import work.socialhub.kgrpc.Channel
import work.socialhub.kgrpc.internal.asKGrpcStatusException
import work.socialhub.kgrpc.internal.jvmMetadata
import work.socialhub.kgrpc.metadata.Metadata
import io.grpc.CallOptions
import io.grpc.MethodDescriptor
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.kotlin.ClientCalls
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

@Throws(work.socialhub.kgrpc.StatusException::class)
suspend fun <REQ, RESP> unaryRpc(
    channel: Channel,
    method: MethodDescriptor<REQ, RESP>,
    request: REQ,
    callOptions: CallOptions,
    headers: Metadata
): RESP {
    return try {
        ClientCalls.unaryRpc(
            channel = channel.channel,
            method = method,
            request = request,
            callOptions = callOptions,
            headers = headers.jvmMetadata
        )
    } catch (e: StatusException) {
        throw e.asKGrpcStatusException
    } catch (e: StatusRuntimeException) {
        throw e.asKGrpcStatusException
    }
}

fun <REQ, RESP> serverStreamingRpc(
    channel: Channel,
    method: MethodDescriptor<REQ, RESP>,
    request: REQ,
    callOptions: CallOptions,
    headers: Metadata
): Flow<RESP> {
    return ClientCalls.serverStreamingRpc(
        channel = channel.channel,
        method = method,
        request = request,
        callOptions = callOptions,
        headers = headers.jvmMetadata
    ).catch { e ->
        when (e) {
            is StatusException -> throw e.asKGrpcStatusException
            is StatusRuntimeException -> throw e.asKGrpcStatusException
            else -> throw e
        }
    }
}

@Throws(work.socialhub.kgrpc.StatusException::class)
suspend fun <REQ, RESP> clientStreamingRpc(
    channel: Channel,
    method: MethodDescriptor<REQ, RESP>,
    requests: Flow<REQ>,
    callOptions: CallOptions,
    headers: Metadata
): RESP {
    return try {
        ClientCalls.clientStreamingRpc(
            channel.channel,
            method = method,
            requests = requests,
            callOptions = callOptions,
            headers = headers.jvmMetadata
        )
    } catch (e: StatusException) {
        throw e.asKGrpcStatusException
    } catch (e: StatusRuntimeException) {
        throw e.asKGrpcStatusException
    }
}

fun <REQ, RESP> bidiStreamingRpc(
    channel: Channel,
    method: MethodDescriptor<REQ, RESP>,
    requests: Flow<REQ>,
    callOptions: CallOptions,
    headers: Metadata
): Flow<RESP> {
    return ClientCalls.bidiStreamingRpc(
        channel.channel,
        method = method,
        requests = requests,
        callOptions = callOptions,
        headers = headers.jvmMetadata
    ).catch { e ->
        when (e) {
            is StatusException -> throw e.asKGrpcStatusException
            is StatusRuntimeException -> throw e.asKGrpcStatusException
            else -> throw e
        }
    }
}
