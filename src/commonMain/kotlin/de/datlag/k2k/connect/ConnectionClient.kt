package de.datlag.k2k.connect

import de.datlag.k2k.Host
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

expect object ConnectionClient {

    internal fun send(bytes: ByteArray, host: Host, port: Int, scope: CoroutineScope): Job

}