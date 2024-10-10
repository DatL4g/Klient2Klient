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
import io.ktor.utils.io.writeFully

internal class ConnectionClient : AutoCloseable {

    private var socket = scopeCatching {
        aSocket(SelectorManager(Dispatcher.IO)).tcp()
    }.getOrNull()

    private var connectedSocket: Socket? = null

    suspend fun send(
        byteArray: ByteArray,
        host: Host,
        port: Int
    ) = suspendCatching {
        val socketAddress = InetSocketAddress(host.hostAddress, port)
        val useSocket = socket ?: suspendCatching {
            aSocket(SelectorManager(Dispatcher.IO)).tcp()
        }.getOrNull()?.also { socket = it } ?: return@suspendCatching

        connectedSocket = useSocket.connect(socketAddress) {
            reuseAddress = true
        }.also {
            val channel = it.openWriteChannel(autoFlush = true)
            channel.writeFully(byteArray, 0, byteArray.size)
            channel.flushAndClose()
        }
    }

    override fun close() {
        connectedSocket?.close()
        connectedSocket = null

        socket = scopeCatching {
            aSocket(SelectorManager(Dispatcher.IO)).tcp()
        }.getOrNull()
    }
}