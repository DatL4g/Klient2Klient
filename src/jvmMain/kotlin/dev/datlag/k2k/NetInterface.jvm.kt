package dev.datlag.k2k

import dev.datlag.tooling.scopeCatching
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import java.net.InetAddress
import java.net.NetworkInterface

actual object NetInterface {
    actual fun getAddresses(): ImmutableSet<String> {
        return NetworkInterface.getNetworkInterfaces().toList().filterNotNull().mapNotNull {
            scopeCatching {
                if (it.isLoopback || !it.isUp) {
                    return@scopeCatching null
                }

                it.interfaceAddresses.mapNotNull { all ->
                    all?.broadcast?.hostAddress?.ifBlank { null }
                }
            }.getOrNull()
        }.flatten().toImmutableSet()
    }

    actual fun getLocalAddress(): String {
        return InetAddress.getLocalHost().hostAddress
    }
}