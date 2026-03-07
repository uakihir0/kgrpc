package work.socialhub.kgrpc.internal

import work.socialhub.kgrpc.Code
import work.socialhub.kgrpc.Status
import work.socialhub.kgrpc.metadata.Entry
import work.socialhub.kgrpc.metadata.Key
import work.socialhub.kgrpc.metadata.Key.Companion.BINARY_KEY_SUFFIX
import work.socialhub.kgrpc.metadata.Metadata

typealias JvmMetadata = io.grpc.Metadata

internal val Metadata.jvmMetadata: JvmMetadata
    get() = JvmMetadata().apply {
        entries.forEach { entry ->
            when (entry) {
                is Entry.Ascii -> {
                    val metadataKey = io.grpc.Metadata.Key.of(entry.key.name, JvmMetadata.ASCII_STRING_MARSHALLER)
                    entry.values.forEach { put(metadataKey, it) }
                }
                is Entry.Binary -> {
                    val metadataKey = io.grpc.Metadata.Key.of(entry.key.name, JvmMetadata.BINARY_BYTE_MARSHALLER)
                    entry.values.forEach { put(metadataKey, it) }
                }
            }
        }
    }

internal val JvmMetadata.kmMetadata: Metadata
    get() {
        val entries: List<Entry<*>> = keys().mapNotNull { keyName ->
            if (keyName.startsWith(':')) return@mapNotNull null

            if (keyName.endsWith(BINARY_KEY_SUFFIX)) {
                val key = io.grpc.Metadata.Key.of(keyName, JvmMetadata.BINARY_BYTE_MARSHALLER)
                val values = this@kmMetadata.getAll(key)?.toSet().orEmpty()
                Entry.Binary(Key.BinaryKey(keyName), values)
            } else {
                val key = io.grpc.Metadata.Key.of(keyName, JvmMetadata.ASCII_STRING_MARSHALLER)
                val value = this@kmMetadata.getAll(key)?.toSet().orEmpty()
                Entry.Ascii(Key.AsciiKey(keyName), value)
            }
        }
        return Metadata.of(entries)
    }

internal val io.grpc.StatusException.asKGrpcStatusException: work.socialhub.kgrpc.StatusException
    get() = work.socialhub.kgrpc.StatusException(
        status = Status(
            code = Code.getCodeForValue(status.code.value()),
            statusMessage = status.description.orEmpty()
        ),
        cause = this
    )

internal val io.grpc.StatusRuntimeException.asKGrpcStatusException: work.socialhub.kgrpc.StatusException
    get() = work.socialhub.kgrpc.StatusException(
        status = Status(
            code = Code.getCodeForValue(status.code.value()),
            statusMessage = status.description.orEmpty()
        ),
        cause = this
    )
