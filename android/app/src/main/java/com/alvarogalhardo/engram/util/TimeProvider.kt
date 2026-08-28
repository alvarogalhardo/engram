package com.alvarogalhardo.engram.util

import java.time.ZoneId

interface TimeProvider {
    fun nowMillis(): Long
    fun zone(): ZoneId
}

class SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
