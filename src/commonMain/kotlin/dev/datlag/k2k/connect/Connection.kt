package dev.datlag.k2k.connect

import kotlinx.coroutines.CoroutineScope
import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.Host
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class Connection private constructor(
    private val port: Int,
    private val immediate: Boolean,
    private val scope: CoroutineScope
) : AutoCloseable {

    private val client = ConnectionClient(immediate)
    private val server = ConnectionServer(immediate)

    private var sendJob: Job? = null

    suspend fun sendNow(byteArray: ByteArray, peer: Host) = client.send(byteArray, peer, port)

    fun send(byteArray: ByteArray, peer: Host) {
        sendJob?.cancel()

        sendJob = scope.launch(Dispatcher.IO) {
            sendNow(byteArray, peer)
        }
    }

    fun receive(listener: suspend (ByteArray) -> Unit) {
        server.receive(port, scope, listener)
    }

    fun stopSending() {
        sendJob?.cancel()
        sendJob = null

        client.close()
    }

    fun stopReceiving() {
        server.close()
    }

    override fun close() {
        stopSending()
        stopReceiving()
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

fun CoroutineScope.connection(builder: Connection.Builder.() -> Unit) = Connection.Builder(this).apply(builder).build()