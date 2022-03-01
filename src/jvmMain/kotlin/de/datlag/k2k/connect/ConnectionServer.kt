package de.datlag.k2k.connect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

actual object ConnectionServer {

    private var serverSocket: ServerSocket = ServerSocket()
        get() {
            return if (field.isClosed) {
                ServerSocket()
            } else {
                field
            }
        }

    private var receiveJob: Job = Job()
    internal actual val receiveData: MutableStateFlow<Pair<String?, ByteArray>?> = MutableStateFlow(null)

    internal actual fun startServer(port: Int, scope: CoroutineScope) {
        receiveJob.cancel()
        receiveJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket.apply {
                    reuseAddress = true
                    soTimeout = 0
                    bind(InetSocketAddress(port))
                }

                while (true) {
                    try {
                        val connectionSocket = serverSocket.accept()
                        connectionSocket?.let { onReceive(connectionSocket) }
                    } catch (ignored: Exception) {
                        // Socket is closed
                        // or
                        // Socket is unbound
                    }
                }
            } catch (ignored: Exception) {
                // Socket port value out of range
                // or
                // Socket closed
                // or
                // Socket already bound
                // or
                // Address unsupported
            } finally {
                serverSocket.close()
            }
        }
    }

    internal actual fun stopServer() {
        receiveJob.cancel()
        serverSocket.close()
    }

    private suspend fun onReceive(connectionSocket: Socket) {
        try {
            val dataInputStream = DataInputStream(connectionSocket.getInputStream())
            val length = dataInputStream.readInt()
            if (length > 0) {
                val bytes = ByteArray(length)
                dataInputStream.readFully(bytes, 0, bytes.size)
                receiveData.emit(Pair(connectionSocket.inetAddress.hostAddress, bytes))
            }
        } catch (ignored: Exception) {
            // Socket closed
            // or
            // Socket not connected
            // or
            // Socket output shutdown
        }
    }

}