package dev.datlag.k2k.connect

import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.NetInterface
import dev.datlag.tooling.async.suspendCatching
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.tcpNoDelay
import io.ktor.utils.io.core.use
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class ConnectionServer(
    private val immediate: Boolean
) : AutoCloseable {
    private var receiveJob: Job? = null
    private var socket = aSocket(SelectorManager(Dispatcher.IO)).let {
        if (immediate) {
            it.tcpNoDelay().tcp()
        } else {
            it.tcp()
        }
    }

    private var serverSocket: ServerSocket? = null
    private var connectedSocket: Socket? = null

    fun receive(
        port: Int,
        scope: CoroutineScope,
        listener: suspend (ByteArray) -> Unit
    ) {
        close()

        receiveJob = scope.launch(Dispatcher.IO) {
            val socketAddress = InetSocketAddress(NetInterface.getLocalAddress(), port)

            serverSocket = socket.bind(socketAddress) {
                reuseAddress = true
            }

            while(currentCoroutineContext().isActive) {
                connectedSocket?.close()
                connectedSocket = serverSocket?.accept()?.also {
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
            }
        }
    }

    override fun close() {
        receiveJob?.cancel()
        receiveJob = null

        serverSocket?.close()
        serverSocket = null

        connectedSocket?.close()
        connectedSocket = null

        socket = aSocket(SelectorManager(Dispatcher.IO)).let {
            if (immediate) {
                it.tcpNoDelay().tcp()
            } else {
                it.tcp()
            }
        }
    }
}