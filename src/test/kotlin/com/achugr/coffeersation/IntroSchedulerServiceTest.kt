package com.achugr.coffeersation

import com.achugr.coffeersation.model.IntroFrequency
import com.achugr.coffeersation.service.IntroSchedulerService
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class IntroSchedulerServiceTest : ShouldSpec({
    var service: IntroSchedulerService? = null

    beforeTest {
        service = IntroSchedulerService() // Assuming IntroSchedulerService() has getNextRun(IntroFrequency, Instant?)
    }

    should("Return current time for NOW_ONCE frequency") {
        val nextRun = service?.getNextRun(IntroFrequency.NOW_ONCE, null)
        Duration.between(Instant.now(), nextRun) shouldBeLessThan Duration.ofMillis(500)
    }

    should("Return approximately 1 minute later time for EVERY_MINUTE_TESTING") {
        val now = Instant.now()
        val nextRun = service?.getNextRun(IntroFrequency.EVERY_MINUTE_TESTING, now)
        val expectedTime = now.plusSeconds(60)
        Duration.between(nextRun, expectedTime) shouldBeLessThan Duration.ofMillis(500)
    }

    should("Return the next Monday's time for MONDAY_ONCE_A_WEEK frequency and null lastRun") {
        val now = OffsetDateTime.now()
        val expectedNextDate = now
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .truncatedTo(ChronoUnit.HOURS)
            .withHour(12)
            .toInstant()

        val nextRun = service?.getNextRun(IntroFrequency.MONDAY_ONCE_A_WEEK, null)

        nextRun shouldBe expectedNextDate
    }

    should("Return the next Monday's time for MONDAY_ONCE_A_WEEK frequency and not null lastRun") {
        val lastRun = OffsetDateTime.now().minusWeeks(2).toInstant()
        val expectedNextDate = OffsetDateTime.ofInstant(lastRun, ZoneOffset.UTC).plusWeeks(1).toInstant()

        val nextRun = service?.getNextRun(IntroFrequency.MONDAY_ONCE_A_WEEK, lastRun)

        nextRun shouldBe expectedNextDate
    }

})