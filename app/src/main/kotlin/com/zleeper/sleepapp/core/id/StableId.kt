package com.zleeper.sleepapp.core.id

import java.nio.charset.StandardCharsets
import java.util.UUID

fun stableId(vararg parts: String): String = UUID.nameUUIDFromBytes(parts.joinToString(":").toByteArray(StandardCharsets.UTF_8)).toString()

fun stableLong(value: String): Long {
    val bytes = UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8))
    return bytes.mostSignificantBits xor bytes.leastSignificantBits
}
