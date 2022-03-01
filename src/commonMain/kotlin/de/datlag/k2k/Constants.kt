package de.datlag.k2k

import kotlinx.serialization.json.Json

internal object Constants {
    internal val json: Json = Json {
        isLenient = true
        explicitNulls
    }
    const val BROADCAST_ADDRESS = "255.255.255.255"
    const val BROADCAST_SOCKET = "0.0.0.0"
}