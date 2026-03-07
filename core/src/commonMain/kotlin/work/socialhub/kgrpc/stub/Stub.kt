package work.socialhub.kgrpc.stub

import kotlin.time.Duration

abstract class Stub<S : Stub<S>> {
    abstract fun withDeadlineAfter(duration: Duration): S
}
