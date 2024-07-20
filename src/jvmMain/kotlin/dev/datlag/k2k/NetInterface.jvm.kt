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
        val allAvailable = mutableSetOf<String>()
        val interfaces = scopeCatching {
            NetworkInterface.getNetworkInterfaces()
        }.getOrNull() ?: return InetAddress.getLocalHost().hostAddress

        for (inter in interfaces) {
            if (inter == null) {
                continue
            }

            for (inet in inter.inetAddresses) {
                if (inet == null) {
                    continue
                }

                if (!inet.isLoopbackAddress && inet is Inet4Address) {
                    inet.hostAddress?.ifBlank { null }?.let(allAvailable::add)
                }
            }
        }

        if (allAvailable.isEmpty()) {
            return InetAddress.getLocalHost().hostAddress
        }

        return allAvailable.singleOrNull() ?: scopeCatching {
            InetAddress.getLocalHost().hostAddress?.ifBlank { null }
        }.getOrNull() ?: allAvailable.first()
    }
}