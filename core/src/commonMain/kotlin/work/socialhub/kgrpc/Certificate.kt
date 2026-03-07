package work.socialhub.kgrpc

import work.socialhub.kgrpc.util.bytesAsPemContent
import work.socialhub.kgrpc.util.parseBytesFromPemContent

class Certificate internal constructor(internal val data: ByteArray) {

    internal val asPem: String get() = bytesAsPemContent(data, PEM_CONTAINER)

    companion object {
        private const val PEM_CONTAINER = "CERTIFICATE"

        fun fromPem(content: String): Certificate {
            return Certificate(parseBytesFromPemContent(content, PEM_CONTAINER))
        }

        fun fromByteArray(byteArray: ByteArray): Certificate {
            return Certificate(byteArray)
        }
    }
}
