package dev.datlag.k2k.connect

import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.NetInterface
import dev.datlag.tooling.async.suspendCatching
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
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

    private var connectedSocket: Socket? = null

    fun receive(
        port: Int,
        scope: CoroutineScope,
        listener: suspend (ByteArray) -> Unit
    ) {
        close()

        receiveJob = scope.launch(Dispatcher.IO) {
            val socketAddress = InetSocketAddress(NetInterface.getLocalAddress(), port)

            connectedSocket = socket.bind(socketAddress) {
                reuseAddress = true
            }.accept().also {
                it.use { boundSocket ->
                    suspendCatching {
                        val readChannel = boundSocket.openReadChannel()
                        val buffer = ByteArray(readChannel.availableForRead)
                        while (currentCoroutineContext().isActive) {
                            val bytesRead = readChannel.readAvailable(buffer)
                            if (bytesRead <= 0) {
                                continue
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

    private fun closeSocket() {
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

    override fun close() {
        receiveJob?.cancel()
        receiveJob = null

        closeSocket()
    }
}