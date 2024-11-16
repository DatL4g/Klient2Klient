package dev.datlag.k2k

import dev.datlag.tooling.scopeCatching
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

data class NetAddress internal constructor(
    val address: String?,
    val isLoopback: Boolean,
    val isVirtual: Boolean,
    val isUp: Boolean,
    val type: Type
) {
    val isIPv4: Boolean
        get() = this.type is Type.IPv4

    val isIPv6: Boolean
        get() = this.type is Type.IPv6

    val addressIsLocalHost: Boolean
        get() = this.address.isNullOrBlank()
                || this.address.equals("127.0.0.1", ignoreCase = true)
                || this.address.equals("::1", ignoreCase = true)

    constructor(
        inetAddress: InetAddress,
        loopback: Boolean?,
        virtual: Boolean,
        up: Boolean
    ) : this(
        address = scopeCatching {
            scopeCatching {
                InetAddress.getByName(inetAddress.hostAddress?.trim())
            }.getOrNull()?.hostAddress?.ifBlank { null } ?: inetAddress.hostAddress?.trim()
        }.getOrNull()?.ifBlank { null },
        isLoopback = loopback ?: scopeCatching {
            inetAddress.isLoopbackAddress
        }.getOrNull() ?: false,
        isVirtual = virtual,
        isUp = up,
        type = Type.fromInet(inetAddress)
    )

    fun nonLocalHost(): NetAddress? = if (addressIsLocalHost) {
        null
    } else {
        this
    }

    sealed interface Type {
        data object IPv4 : Type
        data object IPv6 : Type

        companion object {
            fun fromInet(inetAddress: InetAddress) = if (inetAddress is Inet4Address) {
                IPv4
            } else {
                IPv6
            }
        }
    }

    companion object {
        fun getAll(): ImmutableSet<NetAddress> {
            val interfaces = scopeCatching {
                NetworkInterface.getNetworkInterfaces()
            }.getOrNull() ?: return persistentSetOf()

            return interfaces.asSequence().toSet().filterNotNull().mapNotNull { inter ->
                val interLoopback = scopeCatching {
                    inter.isLoopback
                }.getOrNull()

                val interVirtual = scopeCatching {
                    inter.isVirtual
                }.getOrNull()

                val interUp = scopeCatching {
                    inter.isUp
                }.getOrNull()

                val addresses = scopeCatching {
                    inter.inetAddresses
                }.getOrNull() ?: return@mapNotNull null

                addresses.asSequence().toSet().filterNotNull().map { inet ->
                    NetAddress(
                        inetAddress = inet,
                        loopback = interLoopback,
                        virtual = interVirtual ?: scopeCatching {
                            inter.isVirtual
                        }.getOrNull() ?: false,
                        up = interUp ?: scopeCatching {
                            inter.isUp
                        }.getOrNull() ?: true
                    )
                }
            }.flatten().toImmutableSet()
        }

        fun localHost(): NetAddress? = scopeCatching {
            InetAddress.getLocalHost()
        }.getOrNull()?.let {
            NetAddress(
                inetAddress = it,
                loopback = null,
                virtual = false,
                up = true
            )
        }
    }
}

internal fun Collection<NetAddress>.localNetworkAddresses() = this.filter {
    it.isUp && !it.isVirtual && !it.isLoopback && it.isIPv4 && !it.addressIsLocalHost
}.ifEmpty {
    this.filter {
        it.isUp && !it.isLoopback && it.isIPv4 && !it.addressIsLocalHost
    }.ifEmpty {
        this.filter {
            it.isUp && !it.isVirtual && !it.isLoopback && !it.addressIsLocalHost
        }.ifEmpty {
            this.filter {
                it.isUp && !it.isLoopback && !it.addressIsLocalHost
            }
        }
    }
}