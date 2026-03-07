package work.socialhub.kgrpc

import work.socialhub.kgrpc.util.bytesAsPemContent
import work.socialhub.kgrpc.util.parseBytesFromPemContent

class PrivateKey internal constructor(internal val data: ByteArray) {

    internal val asPem: String get() = bytesAsPemContent(data, PEM_CONTAINER)

    companion object {
        private const val PEM_CONTAINER = "PRIVATE KEY"

        fun fromPem(content: String): PrivateKey {
            return PrivateKey(parseBytesFromPemContent(content, PEM_CONTAINER))
        }

        fun fromByteArray(byteArray: ByteArray): PrivateKey {
            return PrivateKey(byteArray)
        }
    }
}
