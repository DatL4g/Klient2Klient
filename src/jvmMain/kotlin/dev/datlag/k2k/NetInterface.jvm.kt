package dev.datlag.k2k

import dev.datlag.tooling.scopeCatching
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

actual object NetInterface {
    actual fun getAddresses(): ImmutableSet<String> {
        return scopeCatching {
            NetworkInterface.getNetworkInterfaces().toList().filterNotNull().mapNotNull {
                scopeCatching loop@{
                    if (it.isLoopback || !it.isUp) {
                        return@loop null
                    }

                    it.interfaceAddresses.mapNotNull { all ->
                        all?.broadcast?.hostAddress?.ifBlank { null }
                    }
                }.getOrNull()
            }.flatten()
        }.getOrNull().orEmpty().toImmutableSet()
    }

    actual fun getLocalAddress(): String {
        val candidates = NetAddress.getAll().localNetworkAddresses()

        if (candidates.isEmpty()) {
            return NetAddress.localHost()?.nonLocalHost()?.address
                ?: InetAddress.getLocalHost().hostAddress
        }

        return candidates.singleOrNull()?.address
            ?: NetAddress.localHost()?.nonLocalHost()?.address
            ?: candidates.firstNotNullOfOrNull { it.address }
            ?: InetAddress.getLocalHost().hostAddress
    }
}