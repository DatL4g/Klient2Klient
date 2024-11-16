package dev.datlag.k2k

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.collections.immutable.toImmutableSet
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.NI_MAXHOST
import platform.posix.NI_NUMERICHOST
import platform.posix.getnameinfo
import platform.posix.sockaddr
import platform.posix.socklen_t

actual object NetInterface {

    @OptIn(ExperimentalForeignApi::class)
    actual fun getAddresses(): ImmutableSet<String> {
        val addresses = mutableSetOf<String>()

        memScoped {
            val ifaddrs = allocPointerTo<ifaddrs>()
            var ifaPtr: CPointer<ifaddrs>? = ifaddrs.value

            if (getifaddrs(ifaddrs.ptr) == 0) {
                while (ifaPtr != null) {
                    val family = ifaPtr.pointed.ifa_addr?.pointed?.sa_family?.toInt()
                    if (family == AF_INET || family == AF_INET6) {
                        val host = allocArray<ByteVar>(NI_MAXHOST)
                        val res = getnameinfo(
                            ifaPtr.pointed.ifa_addr?.reinterpret(),
                            (ifaPtr.pointed.ifa_addr?.pointed?.sa_len ?: 0) as socklen_t,
                            host,
                            NI_MAXHOST.toUInt(),
                            null,
                            0u,
                            NI_NUMERICHOST
                        )
                        if (res == 0) {
                            addresses.add(host.toKString())
                        }
                    }
                    ifaPtr = ifaPtr.pointed.ifa_next?.reinterpret()
                }
            }
            freeifaddrs(ifaPtr)
        }
        return addresses.toImmutableSet()
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun getLocalAddress(): String {
        val ifa: ifaddrs = memScoped {
            val ifap = allocPointerTo<ifaddrs>()
            getifaddrs(ifap.ptr)
            ifap.pointed!!
        }

        var address: String? = null
        var ifaPtr: CPointer<ifaddrs>? = ifa.ptr
        while (ifaPtr != null) {
            val loopIfa: ifaddrs = ifaPtr.pointed
            val sa: sockaddr = loopIfa.ifa_addr!!.pointed
            val family = sa.sa_family.toInt()

            if (family == AF_INET || family == AF_INET6) {
                val hostBuffer = ByteArray(NI_MAXHOST)
                getnameinfo(
                    sa.ptr.reinterpret(),
                    sa.sa_len.toUInt(),
                    hostBuffer.refTo(0),
                    hostBuffer.size.toUInt(),
                    null,
                    0u,
                    NI_NUMERICHOST
                )
                address = hostBuffer.toKString()
                break
            }
            ifaPtr = loopIfa.ifa_next
        }
        freeifaddrs(ifa.ptr)
        return address ?: ""
    }

    actual fun resolve(name: String): String = name
}