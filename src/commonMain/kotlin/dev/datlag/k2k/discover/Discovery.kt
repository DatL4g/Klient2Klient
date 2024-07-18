package dev.datlag.k2k.discover

import dev.datlag.k2k.Constants
import kotlinx.coroutines.CoroutineScope
import dev.datlag.k2k.Dispatcher
import dev.datlag.k2k.Host
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlin.jvm.JvmOverloads
import kotlin.properties.Delegates
import kotlin.time.Duration

class Discovery private constructor(
    private val discoveryTimeout: Long,
    private val discoveryTimeoutListener: (suspend () -> Unit)?,
    private val discoverableTimeout: Long,
    private val discoverableTimeoutListener: (suspend () -> Unit)?,
    private val discoverPing: Long,
    private val port: Int,
    private val hostFilter: Regex,
    private val hostIsClient: Boolean,
    private val scope: CoroutineScope
) : AutoCloseable {

    private val client = DiscoveryClient()
    private val server = DiscoveryServer()

    private var discoverableTimer: Job? = null
    private var discoveryTimer: Job? = null

    val peers: StateFlow<ImmutableSet<Host>> = server.hosts

    @OptIn(ExperimentalSerializationApi::class)
    fun show(host: Host) {
        val sendingData = Constants.protobuf.encodeToByteArray(host)
        client.broadcast(
            port = port,
            ping = discoverPing,
            data = sendingData,
            scope = scope
        )
        discoverableTimer?.cancel()

        if (discoverableTimeout > 0L) {
            discoverableTimer = scope.launch(Dispatchers.Default) {
                delay(discoverableTimeout)
                discoverableTimeoutListener?.invoke()

            }
        }
    }

    @JvmOverloads
    fun show(
        name: String,
        filterMatch: String = ""
    ) = show(Host(name = name, filterMatch = filterMatch))

    fun hide() {
        client.close()
        discoverableTimer?.cancel()
        discoverableTimer = null
    }

    @JvmOverloads
    fun search(
        hostIsClient: Boolean = this.hostIsClient,
        filter: Regex = this.hostFilter
    ) {
        server.listen(
            port = port,
            ping = discoverPing,
            filter = filter,
            hostIsClient = hostIsClient,
            scope = scope
        )
        discoveryTimer?.cancel()

        if (discoveryTimeout > 0L) {
            discoveryTimer = scope.launch(Dispatchers.Default) {
                delay(discoveryTimeout)
                discoveryTimeoutListener?.invoke()
                lose()
            }
        }
    }

    fun lose() {
        server.close()
        discoveryTimer?.cancel()
        discoveryTimer = null
    }

    override fun close() {
        hide()
        lose()
    }

    class Builder(private var scope: CoroutineScope = CoroutineScope(Dispatcher.IO)) {
        private var discoveryTimeout: Long = 1L
        private var discoveryTimeoutListener: (suspend () -> Unit)? = null
        private var discoverableTimeout: Long = 1L
        private var discoverableTimeoutListener: (suspend () -> Unit)? = null
        private var discoverPing: Long = 1000L
        private var port by Delegates.notNull<Int>()
        private var hostFilter: Regex = Regex("^$")
        private var hostIsClientToo = false

        fun setSearchTimeout(timeoutMilli: Long) = apply {
            this.discoveryTimeout = timeoutMilli
        }

        fun setSearchTimeout(duration: Duration) = apply {
            this.discoveryTimeout = duration.inWholeMilliseconds
        }

        fun setSearchTimeoutListener(listener: suspend () -> Unit) = apply {
            this.discoveryTimeoutListener = listener
        }

        fun setShowTimeout(timeoutMilli: Long) = apply {
            this.discoverableTimeout = timeoutMilli
        }

        fun setShowTimeout(duration: Duration) = apply {
            this.discoverableTimeout = duration.inWholeMilliseconds
        }

        fun setShowTimeoutListener(listener: suspend () -> Unit) = apply {
            this.discoverableTimeoutListener = listener
        }

        fun setPing(intervalMilli: Long) = apply {
            this.discoverPing = intervalMilli
        }

        fun setPing(duration: Duration) = apply {
            this.discoverPing = duration.inWholeMilliseconds
        }

        fun setPort(port: Int) = apply {
            this.port = port
        }

        fun setHostFilter(filter: Regex) = apply {
            this.hostFilter = filter
        }

        fun setScope(scope: CoroutineScope) = apply {
            this.scope = scope
        }

        fun setHostIsClient(`is`: Boolean) = apply {
            this.hostIsClientToo = `is`
        }

        fun build() = Discovery(
            discoveryTimeout,
            discoveryTimeoutListener,
            discoverableTimeout,
            discoverableTimeoutListener,
            discoverPing,
            port,
            hostFilter,
            hostIsClientToo,
            scope
        )
    }
}

fun CoroutineScope.discovery(builder: Discovery.Builder.() -> Unit) = Discovery.Builder(this).apply(builder).build()