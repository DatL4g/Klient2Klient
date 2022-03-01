package de.datlag.k2k

import kotlinx.coroutines.Dispatchers

internal actual object Dispatcher {
    actual val IO = Dispatchers.IO
}