package de.datlag.k2k.connect

import de.datlag.k2k.Host
import de.datlag.k2k.Dispatcher
import de.datlag.k2k.discover.Discovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class Connection private constructor(
    private val peers: StateFlow<Set<Host>>,
    private val port: Int,
    private val scope: CoroutineScope
) {

    val receiveData: Flow<Pair<Host, ByteArray>> = flow {
        ConnectionServer.receiveData.collect {
            if (it != null) {
                val peer = peers.value.firstOrNull { p -> p.hostAddress == it.first }
                peer?.let { p -> emit(Pair(p, it.second)) }
            }
        }
    }.flowOn(Dispatcher.IO)

    fun send(bytes: ByteArray, peer: Host) = ConnectionClient.send(bytes, peer, port, scope)

    fun startReceiving() {
        ConnectionServer.startServer(port, scope)
    }

    fun stopReceiving() {
        ConnectionServer.stopServer()
    }

    class Builder(private var scope: CoroutineScope = CoroutineScope(Dispatcher.IO)) {
        private var peerFlow: MutableStateFlow<Set<Host>> = MutableStateFlow(setOf())
        private var port by Delegates.notNull<Int>()

        fun fromDiscovery(discovery: Discovery) = apply {
            scope.launch(Dispatcher.IO) {
                peerFlow.emitAll(discovery.peersFlow)
            }
        }

        fun forPeers(peers: Set<Host>) = apply {
            scope.launch(Dispatcher.IO) {
                peerFlow.emit(peers)
            }
        }

        fun forPeer(peer: Host) = forPeers(setOf(peer))

        fun setPort(port: Int) = apply {
            this.port = port
        }

        fun setScope(scope: CoroutineScope) = apply {
            this.scope = scope
        }

        fun build() = Connection(peerFlow.asStateFlow(), port, scope)
    }
}

fun CoroutineScope.connection(builder: Connection.Builder.() -> Unit) = Connection.Builder(this).apply(builder).build()