package de.datlag.k2k.connect

import de.datlag.k2k.Host
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket

actual object ConnectionClient {

    internal actual fun send(
        bytes: ByteArray,
        host: Host,
        port: Int,
        scope: CoroutineScope
    ): Job = scope.launch(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            val destinationAddress = InetAddress.getByName(host.hostAddress)
            socket = Socket(destinationAddress, port)
            val output = DataOutputStream(socket.getOutputStream())
            output.writeInt(bytes.size)
            output.write(bytes)
        } catch (ignored: Exception) {
            // Host is unknown
            // or
            // Socket closed
            // or
            // Socket not connected
            // or
            // Socket output shutdown
        } finally {
            socket?.close()
        }
    }
}