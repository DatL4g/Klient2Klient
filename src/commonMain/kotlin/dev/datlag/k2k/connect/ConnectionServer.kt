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
import kotlin.coroutines.cancellation.CancellationException

internal class ConnectionServer : AutoCloseable {
    private var receiveJob: Job? = null
    private val selectorManager = SelectorManager(Dispatcher.IO)
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

    private suspend fun receive(
        port: Int,
        listener: suspend (ByteArray) -> Unit
    ) {
        close()

        val socketAddress = InetSocketAddress(NetInterface.getLocalAddress(), port)
        serverSocket = aSocket(selectorManager).tcp().bind(socketAddress) {
            reuseAddress = true
        }

        try {
            while (currentCoroutineContext().isActive) {
                connectedSocket?.close()

                connectedSocket = serverSocket?.accept()
                connectedSocket?.use { socket ->
                    val readChannel = socket.openReadChannel()

                    while (currentCoroutineContext().isActive) {
                        val buffer = ByteArray(4096)
                        val bytesRead = readChannel.readAvailable(buffer)
                        if (bytesRead <= 0) {
                            break
                        }

                        listener(buffer.copyOf(bytesRead))
                    }
                }
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
        } finally {
            close()
        }
    }

    override fun close() {
        receiveJob?.cancel()
        receiveJob = null

        serverSocket?.close()
        serverSocket = null

        connectedSocket?.close()
        connectedSocket = null

        selectorManager.close()
    }
}