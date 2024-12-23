package dev.datlag.k2k.discover

import dev.datlag.k2k.Constants
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.NetInterface
import dev.datlag.tooling.async.scopeCatching
import dev.datlag.tooling.async.suspendCatching
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class DiscoveryClient : AutoCloseable {
    private var broadcastJob: Job? = null
    private var socket = scopeCatching {
        aSocket(SelectorManager(Dispatcher.IO)).udp()
    }.getOrNull()

    fun broadcast(
        port: Int,
        ping: Long,
        data: ByteArray,
        scope: CoroutineScope
    ) {
        broadcastJob?.cancel()
        broadcastJob = scope.launch(Dispatcher.IO) {
            while (currentCoroutineContext().isActive) {
                broadcast(port, data)
                delay(ping)
            }
        }
    }

    suspend fun broadcast(
        port: Int,
        data: ByteArray
    ) {
        suspend fun writeToSocket(address: String, port: Int) = suspendCatching {
            val useSocket = socket ?: suspendCatching {
                aSocket(SelectorManager(Dispatcher.IO)).udp()
            }.getOrNull()?.also { socket = it } ?: return@suspendCatching

            val socketConnection = useSocket.connect(InetSocketAddress(address, port)) {
                broadcast = true
                reuseAddress = true
            }

            socketConnection.send(
                Datagram(
                    packet = ByteReadPacket(array = data),
                    address = socketConnection.remoteAddress
                )
            )

            socketConnection.close()
        }

        writeToSocket(Constants.BROADCAST_ADDRESS, port)
        for (address in NetInterface.getAddresses()) {
            writeToSocket(address, port)
        }
    }

    override fun close() {
        broadcastJob?.cancel()
        broadcastJob = null
        socket = scopeCatching {
            aSocket(SelectorManager(Dispatcher.IO)).udp()
        }.getOrNull()
    }
}