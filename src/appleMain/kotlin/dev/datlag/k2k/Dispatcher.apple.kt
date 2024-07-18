package dev.datlag.k2k

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual object Dispatcher {
    actual val IO: CoroutineDispatcher = Dispatchers.IO
}