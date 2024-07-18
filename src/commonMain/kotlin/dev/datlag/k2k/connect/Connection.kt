package dev.datlag.k2k.connect

import kotlinx.coroutines.CoroutineScope
import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.Host
import dev.datlag.k2k.connect.ConnectionClient
import dev.datlag.k2k.connect.ConnectionServer
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class Connection private constructor(
    private val port: Int,
    private val immediate: Boolean,
    private val scope: CoroutineScope
) : AutoCloseable {

    private val client = ConnectionClient(immediate)
    private val server = ConnectionServer(immediate)

    suspend fun sendNow(byteArray: ByteArray, peer: Host) = client.send(byteArray, peer, port)

    fun send(byteArray: ByteArray, peer: Host) = scope.launch(Dispatcher.IO) {
        sendNow(byteArray, peer)
    }

    fun receive(listener: suspend (ByteArray) -> Unit) {
        server.receive(port, scope, listener)
    }

    override fun close() {
        client.close()
        server.close()
    }

    class Builder(private var scope: CoroutineScope = CoroutineScope(Dispatcher.IO)) {
        private var port by Delegates.notNull<Int>()
        private var immediate: Boolean = false

        fun setPort(port: Int) = apply {
            this.port = port
        }

        /**
         * Set TCP_NODELAY socket option to disable the Nagle algorithm.
         */
        fun noDelay() = apply {
            this.immediate = true
        }

        /**
         * Set TCP_DELAY socket option to enable the Nagle algorithm.
         */
        fun delay() = apply {
            this.immediate = false
        }

        fun setScope(scope: CoroutineScope) = apply {
            this.scope = scope
        }

        fun build() = Connection(port, immediate, scope)
    }
}