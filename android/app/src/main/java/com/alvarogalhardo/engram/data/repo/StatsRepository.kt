package com.alvarogalhardo.engram.data.repo

import com.alvarogalhardo.engram.data.db.dao.ReviewLogDao
import com.alvarogalhardo.engram.domain.stats.ReviewSample
import com.alvarogalhardo.engram.domain.stats.StatsCalculator
import com.alvarogalhardo.engram.domain.stats.StatsResult
import com.alvarogalhardo.engram.util.TimeProvider
import java.time.Instant

class StatsRepository(
    private val reviewLogDao: ReviewLogDao,
    private val clock: TimeProvider,
) {
    suspend fun stats(): StatsResult {
        val now = clock.nowMillis()
        val zone = clock.zone()
        val yearAgo = now - 365L * 24 * 60 * 60 * 1000
        val samples = reviewLogDao.logsSince(yearAgo).map { ReviewSample(it.reviewedAt, it.grade) }
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return StatsCalculator.calculate(samples, today, zone)
    }
}
