package dev.datlag.k2k

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Host(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val filterMatch: String = ""
) {
    @Transient
    lateinit var hostAddress: String
}