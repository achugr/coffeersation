package com.achugr.coffeersation

import com.achugr.coffeersation.model.CoffeeTalkStateModel
import com.achugr.coffeersation.model.IntroFrequency
import com.achugr.coffeersation.model.Participant
import com.achugr.coffeersation.model.TalkPair
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CoffeeTalkStateModelTest {
    @Test
    fun testRoundGeneration() {
        var model = CoffeeTalkStateModel.new("ch1", IntroFrequency.NOW_ONCE)
        val p1 = Participant("1")
        val p2 = Participant("2")
        val p3 = Participant("3")
        val p4 = Participant("4")
        model = model.actualize(listOf(p1, p2, p3, p4))
        val round1 = model.generateRound()
        model = model.copy(roundNumber = model.roundNumber + 1)
        round1.shouldContainAll(TalkPair(p1, p4), TalkPair(p2, p3))
        val round2 = model.generateRound()
        model = model.copy(roundNumber = model.roundNumber + 1)
        round2.shouldContainAll(TalkPair(p1, p2), TalkPair(p3, p4))
        val round3 = model.generateRound()
        model = model.copy(roundNumber = model.roundNumber + 1)
        round3.shouldContainAll(TalkPair(p1, p3), TalkPair(p4, p2))
        val round4 = model.generateRound()
        round4 shouldBe round1
    }

    @Test
    fun testRoundGenerationWithPersonExcluded() {
        var model = CoffeeTalkStateModel.new("ch1", IntroFrequency.NOW_ONCE)
        val p1 = Participant("1")
        val p2 = Participant("2")
        val p3 = Participant("3")
        val p4 = Participant("4")
        model = model.actualize(listOf(p1, p2, p3, p4))
        val round1 = model.generateRound()
        model = model.copy(roundNumber = model.roundNumber + 1)
        round1.shouldContainAll(TalkPair(p1, p4), TalkPair(p2, p3))
        model = model.actualize(listOf(p1, p2, p3))
        val round2 = model.generateRound()
        model = model.copy(roundNumber = model.roundNumber + 1)
        round2.shouldContainAll(TalkPair(p1, p2), TalkPair(p3, Participant.FAKE_PARTICIPANT))
        val round3 = model.generateRound()
        model = model.copy(roundNumber = model.roundNumber + 1)
        round3.shouldContainAll(TalkPair(p1, p3), TalkPair(Participant.FAKE_PARTICIPANT, p2))
        model = model.actualize(listOf(p1, p2, p3, p4))
        val round4 = model.generateRound()
        round4 shouldBe round1
    }
}