package work.socialhub.kgrpc.message

interface Message {
    val requiredSize: Int
    val fullName: String
    val isInitialized: Boolean

    fun serialize(): ByteArray
}
