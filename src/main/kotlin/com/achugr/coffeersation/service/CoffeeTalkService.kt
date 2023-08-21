package com.achugr.coffeersation.service

import com.achugr.coffeersation.model.CoffeeTalkStateModel.Helper.fromEntity
import com.achugr.coffeersation.model.IntroFrequency.*
import com.achugr.coffeersation.entity.CoffeeTalkStateEntity
import com.achugr.coffeersation.model.CoffeeTalkStateModel
import com.achugr.coffeersation.model.Participant
import com.achugr.coffeersation.model.TalkPair
import com.achugr.coffeersation.slack.Member
import com.achugr.coffeersation.slack.SlackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*


class CoffeeTalkService(
    private val slackService: SlackService,
    private val schedulerHelper: IntroSchedulerService,
    private val asyncTaskService: AsyncTaskService
) {
    private val log = LoggerFactory.getLogger(CoffeeTalkService::class.java)

    private val humanReadableDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
        .withLocale(Locale.ENGLISH)
        .withZone(ZoneId.systemDefault())

    suspend fun initTalk(initTalk: InitCoffeeTalk) {
        val coffeeTalk = upsert(initTalk)
        if (coffeeTalk.introFrequency != PAUSED) {
            scheduleNext(coffeeTalk)
        }
    }

    suspend fun postInfo(requestInfo: RequestInfo) {
        val coffeeTalk = get(requestInfo.channel)
        slackService.postMessage(requestInfo.channel, getStatus(coffeeTalk))
    }

    suspend fun triggerRound(trigger: TriggerTalkRound) {
        val coffeeTalk = get(trigger.channel)
        if (coffeeTalk.introFrequency == PAUSED) {
            log.info("Introduction for ${coffeeTalk.channel} is on pause, skipping round generation.")
            return
        }
        if (coffeeTalk.version != trigger.version) {
            log.info("Trigger $trigger referencing old configuration, skipping round generation.")
            return
        }
        actualize(coffeeTalk).let { actualCoffeeTalk ->
            notify(actualCoffeeTalk.generateRound())
            log.info("Notified about new round in channel ${actualCoffeeTalk.channel}.")
            actualCoffeeTalk.copy(
                lastRun = Instant.now(),
                roundNumber = coffeeTalk.roundNumber + 1
            ).save().let { saved ->
                if (saved.introFrequency != NOW_ONCE) {
                    scheduleNext(saved)
                }
            }
        }
    }

    private suspend fun actualize(coffeeTalk: CoffeeTalkStateModel) =
        coffeeTalk.actualize(slackService.getMembers(coffeeTalk.channel).map { Participant(it.id) })

    private fun getNextIntroDateInfo(coffeeTalk: CoffeeTalkStateModel) = coffeeTalk.nextRun?.let {
        "Next intro date is ${humanReadableDateFormatter.format(it.truncatedTo(ChronoUnit.DAYS))}"
    } ?: ""

    private fun getStatus(coffeeTalk: CoffeeTalkStateModel): String {
        return when (coffeeTalk.introFrequency) {
            NOW_ONCE -> ":information_source: Coffee talk was scheduled to run once, feel free to schedule it again via /start command."
            PAUSED -> ":information_source: Coffee talk for channel is paused."
            MONDAY_ONCE_A_WEEK -> ":information_source: Coffee talk is scheduled to run once a week. ${
                getNextIntroDateInfo(
                    coffeeTalk
                )
            }"

            MONDAY_ONCE_TWO_WEEKS -> ":information_source: Coffee talk is scheduled to run once in two weeks. ${
                getNextIntroDateInfo(
                    coffeeTalk
                )
            }"

            else -> throw IllegalArgumentException("Unknown frequency")
        }
    }

    private suspend fun notify(round: List<TalkPair>) {
        withContext(Dispatchers.IO) {
            round.map { pair ->
                async {
                    val message =
                        if (pair.isFakePair()) {
                            "Hey! Sorry, you don't get pair this round :("
                        } else {
                            "Hey people, you were randomly chosen to hava a coffee-talk!"
                        }
                    slackService.openConversation(pair.getParticipants().map { Member(it.id) }, message)
                }
            }.awaitAll()
        }
    }

    private suspend fun scheduleNext(coffeeTalk: CoffeeTalkStateModel) {
        val nextRun = schedulerHelper.getNextRun(coffeeTalk.introFrequency, coffeeTalk.lastRun)
        coffeeTalk.copy(nextRun = nextRun).save().let {
            val trigger = TriggerTalkRound(it.channel, it.version)
            asyncTaskService.execute(trigger, nextRun)
        }
    }

    private fun upsert(initTalk: InitCoffeeTalk): CoffeeTalkStateModel =
        CoffeeTalkStateEntity.find(initTalk.channel)
            ?.let { fromEntity(it.copy(lastRun = null, nextRun = null, schedule = initTalk.freq)).save() }
            ?: CoffeeTalkStateModel.new(initTalk.channel, initTalk.freq).save()


    private fun get(channel: String) = CoffeeTalkStateEntity.find(channel)
        ?.let { fromEntity(it) }
        ?: throw IllegalArgumentException("Coffee talk in $channel not initialized")
}
