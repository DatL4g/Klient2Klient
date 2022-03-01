package de.datlag.k2k.discover

import de.datlag.k2k.Constants
import de.datlag.k2k.Host
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

actual object DiscoveryServer {

    private var socket: DatagramSocket = DatagramSocket(null)
        get() {
            return if (field.isClosed) {
                DatagramSocket(null)
            } else {
                field
            }
        }

    private val currentHostIps: MutableStateFlow<Set<String>> = MutableStateFlow(setOf())
    internal actual val hosts: MutableStateFlow<Map<Host, Long>> = MutableStateFlow(mapOf())
    private var listenJob: Job = Job()

    internal actual fun startListening(
        port: Int,
        ping: Long,
        puffer: Long,
        hostFilter: Regex,
        hostIsClient: Boolean,
        scope: CoroutineScope
    ) {
        listenJob.cancel()
        listenJob = scope.launch(Dispatchers.IO) {
            updateCurrentDeviceIps()
            while (true) {
                listen(port, puffer, hostFilter, hostIsClient)
                delay(ping)
            }
        }
    }

    internal actual fun stopListening() {
        listenJob.cancel()
        socket.close()
    }

    private suspend fun updateCurrentDeviceIps() {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            val updatedIps: MutableSet<String> = mutableSetOf()

            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                try {
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue

                    val inetAddresses = networkInterface.inetAddresses
                    while (inetAddresses.hasMoreElements()) {
                        val inetAddress = inetAddresses.nextElement()
                        inetAddress.hostAddress?.let { updatedIps.add(it) }
                    }
                } catch (ignored: Exception) {
                    // cannot access inetAddresses
                }
            }
            currentHostIps.emit(currentHostIps.value.toMutableSet().apply {
                retainAll(updatedIps)
                addAll(updatedIps)
            })
        } catch (ignored: Exception) {
            // No network interface configured
        }
    }

    private suspend fun listen(port: Int, puffer: Long, filter: Regex, hostIsClientToo: Boolean) {
        val socketAddress = InetSocketAddress(InetAddress.getByName(Constants.BROADCAST_SOCKET), port)
        try {
            if (!socket.isBound) {
                socket.apply {
                    reuseAddress = true
                    broadcast = true
                    soTimeout = 0
                }.bind(socketAddress)
            }

            val receiveBuffer = ByteArray(14 * 1024)
            val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(packet)
            // double trim needed because of weird ByteArray format if sent data is smaller than ByteArray can hold
            val receiveString = String(packet.data).trim { it <= ' ' }.trim()

            try {
                val host = Constants.json.decodeFromString<Host>(receiveString).apply {
                    hostAddress = packet.address?.hostAddress
                }

                val keepHosts = hosts.value.filterValues { it + puffer >= System.currentTimeMillis() }.toMutableMap()
                if (hostIsClientToo || !currentHostIps.value.contains(host.hostAddress)) {
                    if (host.filterMatch.matches(filter)) {
                        keepHosts[host] = System.currentTimeMillis()
                    }
                }
                hosts.emit(keepHosts)
            } catch (ignored: Exception) {
                // json decode failed -> probably no host data
            }
        } catch (ignored: Exception) {
            // receiving failed (maybe bytearray to small -> probably not intended to receive anyway (no host data))
            socket.close()
        }
    }

}