package de.datlag.k2k.discover

import de.datlag.k2k.Host
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

expect object DiscoveryServer {

    internal val hosts: MutableStateFlow<Map<Host, Long>>

    internal fun startListening(port: Int, ping: Long, puffer: Long, hostFilter: Regex, hostIsClient: Boolean, scope: CoroutineScope)

    internal fun stopListening()
}