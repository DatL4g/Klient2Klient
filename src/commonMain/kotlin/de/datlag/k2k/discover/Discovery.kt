package de.datlag.k2k.discover

import de.datlag.k2k.Constants
import de.datlag.k2k.Host
import de.datlag.k2k.Dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.properties.Delegates
import kotlin.time.Duration

class Discovery private constructor(
    private val discoveryTimeout: Long,
    private val discoveryTimeoutListener: (suspend () -> Unit)?,
    private val discoverableTimeout: Long,
    private val discoverableTimeoutListener: (suspend () -> Unit)?,
    private val discoverPing: Long,
    private val discoverPuffer: Long,
    private val port: Int,
    private val hostFilter: Regex,
    private val hostIsClient: Boolean,
    private val scope: CoroutineScope
) {

    private var discoverableTimer: Job = Job()
    private var discoveryTimer: Job = Job()

    val peers: Set<Host>
        get() = DiscoveryServer.hosts.value.keys

    val peersFlow: Flow<Set<Host>> = DiscoveryServer.hosts.map { it.keys }.distinctUntilChanged()

    fun makeDiscoverable(host: Host) {
        val sendDataString: String = Constants.json.encodeToString(host)
        DiscoveryClient.startBroadcasting(port, discoverPing, sendDataString.encodeToByteArray(), scope)
        discoverableTimer.cancel()
        if (discoverableTimeout > 0L) {
            discoverableTimer = scope.launch(Dispatcher.IO) {
                delay(discoverableTimeout)
                discoverableTimeoutListener?.invoke()
                stopBeingDiscoverable()
            }
        }
    }

    fun makeDiscoverable(
        hostname: String,
        filterMatch: String = String(),
        optionalInfo: JsonElement? = null
    ) = makeDiscoverable(Host(hostname, filterMatch, optionalInfo))

    fun makeDiscoverable(
        hostname: String,
        filterMatch: String = String(),
        optionalInfo: String = String()
    ) = makeDiscoverable(Host(hostname, filterMatch, Constants.json.encodeToJsonElement(optionalInfo)))

    fun stopBeingDiscoverable() {
        DiscoveryClient.stopBroadcasting()
        discoverableTimer.cancel()
    }

    fun startDiscovery(hostIsClientToo: Boolean = hostIsClient) {
        DiscoveryServer.startListening(port, discoverPing, discoverPuffer, hostFilter, hostIsClientToo, scope)
        discoveryTimer.cancel()
        if (discoveryTimeout > 0L) {
            discoveryTimer = scope.launch(Dispatcher.IO) {
                delay(discoveryTimeout)
                discoveryTimeoutListener?.invoke()
                stopDiscovery()
            }
        }
    }

    fun stopDiscovery() {
        DiscoveryServer.stopListening()
        discoveryTimer.cancel()
    }

    class Builder(private var scope: CoroutineScope = CoroutineScope(Dispatcher.IO)) {
        private var discoveryTimeout by Delegates.notNull<Long>()
        private var discoveryTimeoutListener: (suspend () -> Unit)? = null
        private var discoverableTimeout by Delegates.notNull<Long>()
        private var discoverableTimeoutListener: (suspend () -> Unit)? = null
        private var discoverPing: Long = 1000L
        private var discoverPuffer: Long = 3000L
        private var port by Delegates.notNull<Int>()
        private var hostFilter: Regex = Regex("^$")
        private var hostIsClientToo = false

        fun setDiscoveryTimeout(timeoutMilli: Long) = apply {
            this.discoveryTimeout = timeoutMilli
        }

        fun setDiscoveryTimeout(duration: Duration) = apply {
            this.discoveryTimeout = duration.inWholeMilliseconds
        }

        fun setDiscoveryTimeoutListener(listener: suspend () -> Unit) = apply {
            this.discoveryTimeoutListener = listener
        }

        fun setDiscoverableTimeout(timeoutMilli: Long) = apply {
            this.discoverableTimeout = timeoutMilli
        }

        fun setDiscoverableTimeout(duration: Duration) = apply {
            this.discoverableTimeout = duration.inWholeMilliseconds
        }

        fun setDiscoverableTimeoutListener(listener: suspend () -> Unit) = apply {
            this.discoverableTimeoutListener = listener
        }

        fun setPing(intervalMilli: Long) = apply {
            this.discoverPing = intervalMilli
        }

        fun setPing(duration: Duration) = apply {
            this.discoverPing = duration.inWholeMilliseconds
        }

        fun setPuffer(pufferMilli: Long) = apply {
            this.discoverPuffer = pufferMilli
        }

        fun setPuffer(duration: Duration) = apply {
            this.discoverPuffer = duration.inWholeMilliseconds
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
            discoverPuffer,
            port,
            hostFilter,
            hostIsClientToo,
            scope
        )
    }
}

fun CoroutineScope.discovery(builder: Discovery.Builder.() -> Unit) = Discovery.Builder(this).apply(builder).build()