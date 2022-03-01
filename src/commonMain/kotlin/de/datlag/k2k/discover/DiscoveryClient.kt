package de.datlag.k2k.discover

import kotlinx.coroutines.CoroutineScope

expect object DiscoveryClient {

    internal fun startBroadcasting(port: Int, ping: Long, data: ByteArray, scope: CoroutineScope)

    internal fun stopBroadcasting()

}