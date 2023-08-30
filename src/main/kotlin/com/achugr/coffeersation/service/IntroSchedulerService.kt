package com.achugr.coffeersation.service

import com.achugr.coffeersation.model.IntroFrequency
import com.achugr.coffeersation.model.IntroFrequency.*
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class IntroSchedulerService {

    fun getNextRun(introFrequency: IntroFrequency, lastRun: Instant?): Instant {
        return when (introFrequency) {
            NOW_ONCE -> Instant.now()
            EVERY_MINUTE_TESTING -> lastRun?.plus(Duration.ofMinutes(1)) ?: Instant.now()
            MONDAY_ONCE_A_WEEK, MONDAY_ONCE_TWO_WEEKS -> getNextMonday(introFrequency, lastRun)
            else -> throw IllegalArgumentException("Unknown frequency $introFrequency.")
        }
    }

    private fun getNextMonday(introFrequency: IntroFrequency, lastRun: Instant?): Instant {
        val nextMondayTime = OffsetDateTime.now()
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .truncatedTo(ChronoUnit.HOURS)
            .withHour(12)
        val plusWeeks = when (introFrequency) {
            MONDAY_ONCE_A_WEEK -> 1L
            MONDAY_ONCE_TWO_WEEKS -> 2L
            else -> throw IllegalArgumentException("Unsupported frequency $introFrequency for this method.")
        }
        return lastRun
            ?.let { OffsetDateTime.ofInstant(it, ZoneId.of("UTC")).plusWeeks(plusWeeks).toInstant() }
            ?: nextMondayTime.toInstant()
    }
}