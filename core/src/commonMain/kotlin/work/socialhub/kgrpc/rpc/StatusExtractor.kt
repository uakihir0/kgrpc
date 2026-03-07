package work.socialhub.kgrpc.rpc

import work.socialhub.kgrpc.Code
import work.socialhub.kgrpc.Status
import work.socialhub.kgrpc.StatusException
import work.socialhub.kgrpc.metadata.Key
import work.socialhub.kgrpc.metadata.Metadata

private val grpcStatusKey = Key.AsciiKey("grpc-status")
private val grpcMessageKey = Key.AsciiKey("grpc-message")

internal fun extractStatusFromMetadataAndVerify(metadata: Metadata) {
    extractStatusFromMetadataAndVerify(metadata) { it }
}

internal fun extractStatusFromMetadataAndVerify(metadata: Metadata, runInterceptors: (Status) -> Status) {
    val status = extractStatusFromMetadata(metadata)
    if (status != null) {
        val finalStatus = runInterceptors(status)

        if (finalStatus.code != Code.OK) {
            throw StatusException(
                status = finalStatus,
                cause = null
            )
        }
    }
}

private fun extractStatusFromMetadata(metadata: Metadata): Status? {
    val rawStatus = metadata[grpcStatusKey]

    return if (rawStatus != null && rawStatus.toIntOrNull() != null) {
        val code = Code.getCodeForValue(rawStatus.toInt())
        Status(
            code = code,
            statusMessage = metadata[grpcMessageKey].orEmpty()
        )
    } else null
}
