package work.socialhub.kgrpc.util

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val pemRegex =
    "-----BEGIN ([A-Z ]+)-----[\\s\\r\\n]*([A-Za-z0-9+/=\\r\\n]+?)[\\s\\r\\n]*-----END \\1-----".toRegex()

@OptIn(ExperimentalEncodingApi::class)
internal fun parseBytesFromPemContent(pemContent: String, expectedContainer: String): ByteArray {
    val matchResult = pemRegex.matchEntire(pemContent)
        ?: throw IllegalArgumentException("Input does not contain a valid PEM-encoded certificate.")
    val container = matchResult.groups[1]!!.value
    if (container != expectedContainer) throw IllegalArgumentException("Received PEM file with header $container but expected $expectedContainer.")

    val pemBody = (matchResult.groups[2]?.value!!).replace("\n", "")
    return Base64.decode(pemBody)
}

@OptIn(ExperimentalEncodingApi::class)
internal fun bytesAsPemContent(bytes: ByteArray, container: String): String {
    return buildString {
        append("-----BEGIN ")
        append(container)
        append("-----\n")

        Base64
            .encode(bytes)
            .chunkedSequence(64)
            .forEach { chunk ->
                append(chunk)
                append("\n")
            }

        append("-----END ")
        append(container)
        append("-----\n")
    }
}
