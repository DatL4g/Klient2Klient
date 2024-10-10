package dev.datlag.k2k.connect

import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.NetInterface
import dev.datlag.tooling.async.scopeCatching
import dev.datlag.tooling.async.suspendCatching
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.tcpNoDelay
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.core.use
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class ConnectionServer : AutoCloseable {
    private var receiveJob: Job? = null
    private var socket = scopeCatching {
        aSocket(SelectorManager(Dispatcher.IO)).tcp()
    }.getOrNull()

    private var serverSocket: ServerSocket? = null
    private var connectedSocket: Socket? = null

    fun receive(
        port: Int,
        scope: CoroutineScope,
        listener: suspend (ByteArray) -> Unit
    ) {
        receiveJob = scope.launch(Dispatcher.IO) {
            receive(port, listener)
        }
    }

    suspend fun receive(
        port: Int,
        listener: suspend (ByteArray) -> Unit
    ) = suspendCatching {
        close()

        val socketAddress = InetSocketAddress(NetInterface.getLocalAddress(), port)
        val useSocket = socket ?: suspendCatching {
            aSocket(SelectorManager(Dispatcher.IO)).tcp()
        }.getOrNull() ?: return@suspendCatching

        serverSocket = useSocket.bind(socketAddress) {
            reuseAddress = true
        }

        while(currentCoroutineContext().isActive) {
            connectedSocket?.close()

            connectedSocket = suspendCatching {
                serverSocket?.accept()?.also {
                    it.use { boundSocket ->
                        suspendCatching {
                            val readChannel = boundSocket.openReadChannel()
                            val buffer = ByteArray(readChannel.availableForRead)
                            while (true) {
                                val bytesRead = readChannel.readAvailable(buffer)
                                if (bytesRead <= 0) {
                                    break
                                }

                                listener(buffer)
                            }
                        }.onFailure {
                            boundSocket.close()
                        }
                    }
                }
            }.getOrNull()
        }
    }

    override fun close() {
        receiveJob?.cancel()
        receiveJob = null

        serverSocket?.close()
        serverSocket = null

        connectedSocket?.close()
        connectedSocket = null

        socket = scopeCatching {
            aSocket(SelectorManager(Dispatcher.IO)).tcp()
        }.getOrNull()
    }
}