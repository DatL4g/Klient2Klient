package dev.datlag.k2k.connect

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.Host
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.tcpNoDelay
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully

internal class ConnectionClient(
    private val immediate: Boolean
) : AutoCloseable {

    private var socket = aSocket(SelectorManager(Dispatcher.IO)).let {
        if (immediate) {
            it.tcpNoDelay().tcp()
        } else {
            it.tcp()
        }
    }

    private var connectedSocket: Socket? = null

    suspend fun send(
        byteArray: ByteArray,
        host: Host,
        port: Int
    ) {
        val socketAddress = InetSocketAddress(host.hostAddress, port)
        connectedSocket = socket.connect(socketAddress) {
            reuseAddress = true
        }.also {
            val channel = it.openWriteChannel(autoFlush = true)
            channel.writeFully(byteArray, 0, byteArray.size)
            channel.flush()
            channel.close()
        }
    }

    override fun close() {
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