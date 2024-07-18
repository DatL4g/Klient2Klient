package dev.datlag.k2k

import kotlinx.collections.immutable.ImmutableSet

expect object NetInterface {
    // udp, no isLoopback, broadcastAddress is not null
    fun getAddresses(): ImmutableSet<String>
    fun getLocalAddress(): String
}