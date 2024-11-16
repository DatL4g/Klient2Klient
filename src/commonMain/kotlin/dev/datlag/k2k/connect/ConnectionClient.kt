package dev.datlag.k2k.connect

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.Host
import dev.datlag.tooling.async.scopeCatching
import dev.datlag.tooling.async.suspendCatching
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.tcpNoDelay
import io.ktor.utils.io.close
import io.ktor.utils.io.core.use
import io.ktor.utils.io.writeFully
import kotlin.coroutines.cancellation.CancellationException

internal class ConnectionClient : AutoCloseable {

    private val selectorManager = SelectorManager(Dispatcher.IO)
    private var connectedSocket: Socket? = null

    suspend fun send(
        byteArray: ByteArray,
        host: Host,
        port: Int
    ) {
        val socketAddress = InetSocketAddress(host.hostAddress, port)

        try {
            connectedSocket = aSocket(selectorManager).tcp().connect(socketAddress) {
                reuseAddress = true
            }

            connectedSocket?.use { socket ->
                val writeChannel = socket.openWriteChannel(autoFlush = true)

                writeChannel.writeFully(byteArray)
                writeChannel.flushAndClose()
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
        }
    }

    override fun close() {
        connectedSocket?.close()
        connectedSocket = null

        selectorManager.close()
    }
}