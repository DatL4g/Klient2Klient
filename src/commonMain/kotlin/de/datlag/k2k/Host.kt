package de.datlag.k2k

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

@Serializable
data class Host(
    val name: String,
    val filterMatch: String = String(),
    val optionalInfo: JsonElement? = null
) {
    @Transient
    var hostAddress: String? = null
}