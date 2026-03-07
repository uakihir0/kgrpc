package work.socialhub.kgrpc.rpc

import work.socialhub.kgrpc.StatusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

internal fun <T> Flow<T>.singleOrStatusFlow(): Flow<T> = flow {
    var found = false
    collect {
        if (!found) {
            found = true
            emit(it)
        } else {
            throw StatusException.InternalOnlyExpectedOneElement
        }
    }
    if (!found) {
        throw StatusException.InternalExpectedAtLeastOneElement
    }
}

internal suspend fun <T> Flow<T>.singleOrStatus(): T = singleOrStatusFlow().single()
