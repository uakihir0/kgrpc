package work.socialhub.kgrpc.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface KeepAliveConfig {

    data object Disabled : KeepAliveConfig

    data class Enabled(
        val time: Duration,
        val timeout: Duration = 20.seconds,
        val withoutCalls: Boolean = false
    ) : KeepAliveConfig
}
