package dev.datlag.k2k

import kotlinx.coroutines.CoroutineDispatcher

internal expect object Dispatcher {
    val IO: CoroutineDispatcher
}