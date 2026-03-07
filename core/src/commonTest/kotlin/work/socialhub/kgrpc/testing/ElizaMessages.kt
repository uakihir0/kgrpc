package work.socialhub.kgrpc.testing

import work.socialhub.kgrpc.message.Message
import work.socialhub.kgrpc.message.MessageCompanion

/**
 * connectrpc.eliza.v1.SayRequest { sentence: string = 1; }
 * Used to test against demo.connectrpc.com ElizaService.
 */
class SayRequest(val sentence: String = "") : Message {
    override val requiredSize: Int get() = serialize().size
    override val fullName: String = "connectrpc.eliza.v1.SayRequest"
    override val isInitialized: Boolean = true

    override fun serialize(): ByteArray {
        return ProtobufEncoding.encodeStringField(1, sentence)
    }

    companion object : MessageCompanion<SayRequest> {
        override val fullName: String = "connectrpc.eliza.v1.SayRequest"
        override fun deserialize(data: ByteArray): SayRequest {
            if (data.isEmpty()) return SayRequest()
            val fields = ProtobufEncoding.decodeFields(data)
            val bytes = fields[1]?.firstOrNull() as? ByteArray
            return SayRequest(bytes?.decodeToString() ?: "")
        }
    }
}

/**
 * connectrpc.eliza.v1.SayResponse { sentence: string = 1; }
 */
class SayResponse(val sentence: String = "") : Message {
    override val requiredSize: Int get() = serialize().size
    override val fullName: String = "connectrpc.eliza.v1.SayResponse"
    override val isInitialized: Boolean = true

    override fun serialize(): ByteArray {
        return ProtobufEncoding.encodeStringField(1, sentence)
    }

    companion object : MessageCompanion<SayResponse> {
        override val fullName: String = "connectrpc.eliza.v1.SayResponse"
        override fun deserialize(data: ByteArray): SayResponse {
            if (data.isEmpty()) return SayResponse()
            val fields = ProtobufEncoding.decodeFields(data)
            val bytes = fields[1]?.firstOrNull() as? ByteArray
            return SayResponse(bytes?.decodeToString() ?: "")
        }
    }
}

/**
 * connectrpc.eliza.v1.IntroduceRequest { name: string = 1; }
 */
class IntroduceRequest(val name: String = "") : Message {
    override val requiredSize: Int get() = serialize().size
    override val fullName: String = "connectrpc.eliza.v1.IntroduceRequest"
    override val isInitialized: Boolean = true

    override fun serialize(): ByteArray {
        return ProtobufEncoding.encodeStringField(1, name)
    }

    companion object : MessageCompanion<IntroduceRequest> {
        override val fullName: String = "connectrpc.eliza.v1.IntroduceRequest"
        override fun deserialize(data: ByteArray): IntroduceRequest {
            if (data.isEmpty()) return IntroduceRequest()
            val fields = ProtobufEncoding.decodeFields(data)
            val bytes = fields[1]?.firstOrNull() as? ByteArray
            return IntroduceRequest(bytes?.decodeToString() ?: "")
        }
    }
}

/**
 * connectrpc.eliza.v1.IntroduceResponse { sentence: string = 1; }
 */
class IntroduceResponse(val sentence: String = "") : Message {
    override val requiredSize: Int get() = serialize().size
    override val fullName: String = "connectrpc.eliza.v1.IntroduceResponse"
    override val isInitialized: Boolean = true

    override fun serialize(): ByteArray {
        return ProtobufEncoding.encodeStringField(1, sentence)
    }

    companion object : MessageCompanion<IntroduceResponse> {
        override val fullName: String = "connectrpc.eliza.v1.IntroduceResponse"
        override fun deserialize(data: ByteArray): IntroduceResponse {
            if (data.isEmpty()) return IntroduceResponse()
            val fields = ProtobufEncoding.decodeFields(data)
            val bytes = fields[1]?.firstOrNull() as? ByteArray
            return IntroduceResponse(bytes?.decodeToString() ?: "")
        }
    }
}
