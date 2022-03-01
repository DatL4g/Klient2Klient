package de.datlag.k2k.connect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

expect object ConnectionServer {

    internal val receiveData: MutableStateFlow<Pair<String?, ByteArray>?>

    internal fun startServer(port: Int, scope: CoroutineScope)

    internal fun stopServer()

}