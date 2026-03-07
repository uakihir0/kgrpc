package work.socialhub.kgrpc.message

interface MessageCompanion<T : Message> {
    val fullName: String
    fun deserialize(data: ByteArray): T
}
