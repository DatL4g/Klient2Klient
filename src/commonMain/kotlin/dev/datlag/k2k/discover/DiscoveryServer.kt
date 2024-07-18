package dev.datlag.k2k.discover

import dev.datlag.k2k.Constants
import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.Host
import dev.datlag.k2k.NetInterface
import dev.datlag.tooling.async.suspendCatching
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.decodeFromByteArray

internal class DiscoveryServer : AutoCloseable {
    private var listenJob: Job? = null
    private var socket = aSocket(SelectorManager(Dispatcher.IO)).udp()
    internal val hosts = MutableStateFlow<ImmutableSet<Host>>(persistentSetOf())

    fun listen(
        port: Int,
        ping: Long,
        filter: Regex,
        hostIsClient: Boolean,
        scope: CoroutineScope
    ) {
        listenJob?.cancel()
        listenJob = scope.launch(Dispatcher.IO) {
            listen(port, ping, filter, hostIsClient)
        }
    }

    suspend fun listen(
        port: Int,
        ping: Long,
        filter: Regex,
        hostIsClient: Boolean
    ) {
        val socketAddress = InetSocketAddress(Constants.BROADCAST_SOCKET, port)
        val serverSocket = socket.bind(socketAddress) {
            broadcast = true
            reuseAddress = true
        }

        while (currentCoroutineContext().isActive) {
            serverSocket.openReadChannel()
            serverSocket.incoming.consumeEach { datagram ->
                suspendCatching {
                    val receivedPacket = datagram.packet.readByteArray()
                    if (receivedPacket.isNotEmpty()) {
                        val host = Constants.protobuf.decodeFromByteArray<Host>(receivedPacket).apply {
                            val inetSocketAddress = datagram.address as InetSocketAddress

                            this.hostAddress = inetSocketAddress.hostname
                        }

                        hosts.update {
                            val mutable = it.toMutableSet()

                            if (hostIsClient || !NetInterface.getAddresses().contains(host.hostAddress)) {
                                if (host.filterMatch.matches(filter)) {
                                    mutable.add(host)
                                }
                            }

                            mutable.toImmutableSet()
                        }
                    }
                }.onFailure {
                    serverSocket.close()
                }
            }

            delay(ping)
        }
    }

    override fun close() {
        listenJob?.cancel()
        listenJob = null
        socket = aSocket(SelectorManager(Dispatcher.IO)).udp()
        hosts.update { persistentSetOf() }
    }
}