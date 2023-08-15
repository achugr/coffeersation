package com.achugr.coffeersation.model

import com.achugr.coffeersation.entity.CoffeeTalkStateEntity
import com.achugr.coffeersation.entity.ParticipantEntity
import java.time.Instant
import java.util.LinkedList
import kotlin.collections.LinkedHashSet


data class Participant(val id: String) {
    companion object {
        val FAKE_PARTICIPANT = Participant("FAKE_PARTICIPANT")
    }
}

data class TalkPair(val p1: Participant, val p2: Participant) {
    fun isFakePair(): Boolean {
        return p1 == Participant.FAKE_PARTICIPANT || p2 == Participant.FAKE_PARTICIPANT
    }

    fun getParticipants(): List<Participant> {
        return if (isFakePair()) {
            listOf(if (p1 != Participant.FAKE_PARTICIPANT) p1 else p2)
        } else {
            listOf(p1, p2)
        }
    }
}

data class CoffeeTalkStateModel(
    var participants: LinkedHashSet<Participant>,
    val channel: String,
    var roundNumber: Int,
    var lastRun: Instant?,
    var nextRun: Instant?,
    var introFrequency: IntroFrequency,
    var version: Int
) {

    /**
     * Actualize a participant list to match actual channel members.
     * Implementation makes the best effort to keep participants in the same order as they were before,
     * so that the same people don't get paired together more often than they should be.
     */
    fun actualize(actualChannelMembers: Collection<Participant>): CoffeeTalkStateModel {
        val leftChat = participants.minus(actualChannelMembers.toSet()).toMutableSet()
        val joinedChat = actualChannelMembers.minus(participants.toSet())
        val joinerQueue = LinkedList(joinedChat)
        val newParticipants = participants.mapNotNull { participant ->
            if (leftChat.remove(participant)) {
                if (joinerQueue.isNotEmpty()) {
                    joinerQueue.remove()
                } else null
            } else participant
        } + joinerQueue
        return copy(participants = LinkedHashSet(newParticipants))
    }

    fun generateRound(): List<TalkPair> {
        val roundParticipants = participants.toMutableList()
        if (roundParticipants.size.mod(2) != 0) {
            roundParticipants += Participant.FAKE_PARTICIPANT
        }
        val currentRound = mutableListOf(roundParticipants[0])
        (0 until roundParticipants.size - 1)
            .map { (it + roundNumber) % (roundParticipants.size - 1) + 1 }
            .forEach { currentRound.add(roundParticipants[it]) }

        roundNumber++
        return (0 until currentRound.size / 2).map {
            TalkPair(currentRound[it], currentRound[currentRound.size - 1 - it])
        }
    }

    fun toEntity(): CoffeeTalkStateEntity {
        return CoffeeTalkStateEntity(
            participants = participants.map { ParticipantEntity(it.id) },
            channel = channel,
            round = roundNumber,
            lastRun = lastRun,
            nextRun = nextRun,
            schedule = introFrequency,
            version = version
        )
    }

    companion object Helper {
        fun fromEntity(state: CoffeeTalkStateEntity): CoffeeTalkStateModel {
            return CoffeeTalkStateModel(
                participants = LinkedHashSet(
                    state.participants.map { Participant(it.id) }.toMutableList()
                ),
                channel = state.channel,
                roundNumber = state.round,
                lastRun = state.lastRun,
                nextRun = state.nextRun,
                introFrequency = state.schedule,
                version = state.version
            )
        }

        fun new(channel: String, frequency: IntroFrequency): CoffeeTalkStateModel {
            return CoffeeTalkStateModel(LinkedHashSet(), channel, 0, null, null, frequency, 0)
        }
    }
}