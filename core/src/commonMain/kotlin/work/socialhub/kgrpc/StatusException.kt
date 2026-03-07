package work.socialhub.kgrpc

import kotlin.time.Duration

class StatusException internal constructor(
    val status: Status,
    override val cause: Throwable?
) : RuntimeException(cause) {

    override val message: String
        get() = status.toString()

    companion object {
        val UnavailableDueToShutdown = StatusException(
            status = Status(code = Code.UNAVAILABLE, statusMessage = "The channel is shutdown."),
            cause = null
        )

        val CancelledDueToShutdown = StatusException(
            status = Status(code = Code.CANCELLED, statusMessage = "Call was cancelled due to channel shutdown."),
            cause = null
        )

        val InternalOnlyExpectedOneElement = StatusException(
            status = Status(code = Code.INTERNAL, statusMessage = "Expected call to only yield one element, but received more than 1."),
            cause = null
        )

        val InternalExpectedAtLeastOneElement = StatusException(
            status = Status(code = Code.INTERNAL, statusMessage = "Expected call to yield exactly one element, but received none."),
            cause = null
        )

        fun requestTimeout(duration: Duration, cause: Throwable?) = StatusException(
            status = Status(
                code = Code.DEADLINE_EXCEEDED,
                statusMessage = "RPC request timeout exceeded after set deadline of $duration"
            ),
            cause = cause
        )

        fun internal(message: String, cause: Throwable? = null) = StatusException(
            status = Status(
                code = Code.INTERNAL,
                statusMessage = message
            ),
            cause = cause
        )
    }
}
