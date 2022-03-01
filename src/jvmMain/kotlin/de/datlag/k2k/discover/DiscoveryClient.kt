package de.datlag.k2k.discover

import de.datlag.k2k.Constants
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

actual object DiscoveryClient {

    private var socket: DatagramSocket = DatagramSocket().apply {
        broadcast = true
    }
    private var broadcastJob: Job = Job()

    internal actual fun startBroadcasting(
        port: Int,
        ping: Long,
        data: ByteArray,
        scope: CoroutineScope
    ) {
        broadcastJob.cancel()
        broadcastJob = scope.launch(Dispatchers.IO) {
            while (true) {
                send(port, data)
                delay(ping)
            }
        }
    }

    internal actual fun stopBroadcasting() {
        broadcastJob.cancel()
        socket.close()
        socket = DatagramSocket().apply {
            broadcast = true
        }
    }

    private suspend fun send(port: Int, data: ByteArray) {
        val packet = DatagramPacket(data, data.size, InetAddress.getByName(Constants.BROADCAST_ADDRESS), port)
        try {
            socket.send(packet)
        } catch (ignored: Exception) {
            // UDP sending failed to broadcast address
        }

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                for (address in networkInterface.interfaceAddresses) {
                    val broadcastAddress = address.broadcast ?: continue

                    try {
                        val sendPacket = DatagramPacket(data, data.size, broadcastAddress, port)
                        socket.send(sendPacket)
                    } catch (ignored: Exception) {
                        // broadcast address invalid
                        // or
                        // UDP sending failed to broadcast address
                    }
                }
            }
        } catch (ignored: Exception) {
            // no network interfaces configured
        }
    }
}