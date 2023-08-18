package com.achugr.coffeersation.entity

import com.achugr.coffeersation.model.IntroFrequency
import com.achugr.coffeersation.datastore
import com.google.cloud.Timestamp
import com.google.cloud.datastore.*
import java.time.Instant
import java.util.*

data class ParticipantEntity(var id: String) {
    fun marshall(): EntityValue {
        val fullEntity = FullEntity.newBuilder()
            .set("id", id)
            .build()

        return EntityValue.newBuilder(fullEntity).build()
    }

    companion object Persistence {
        fun unmarshall(participantEntity: EntityValue): ParticipantEntity {
            return ParticipantEntity(participantEntity.get().getString("id"))
        }
    }
}

private const val COFFEE_TALK_STATE_ENTITY = "CoffeeTalkStateEntity"

data class CoffeeTalkStateEntity(
    var channel: String,
    var participants: List<ParticipantEntity> = listOf(),
    var round: Int,
    var lastRun: Instant?,
    var nextRun: Instant?,
    var schedule: IntroFrequency,
    var version: Int,
) {

    fun save(): CoffeeTalkStateEntity {
        val tx = datastore.newTransaction()
        try {
            tx.get(marshall().key)?.let { existing ->
                if (unmarshall(existing).version != version) {
                    throw ConcurrentModificationException("Item ${channel} was concurrently modified")
                }
            }
            version++
            tx.put(marshall())
            tx.commit()
            return this
        } finally {
            if (tx.isActive) {
                tx.rollback()
            }
        }
    }

    private fun marshall(): Entity {
        val key = datastore
            .newKeyFactory()
            .setKind(COFFEE_TALK_STATE_ENTITY)
            .newKey(channel)
        return Entity.newBuilder(key)
            .set("round", LongValue.of(round.toLong()))
            .set("lastRun", lastRun?.let { TimestampValue(Timestamp.of(Date.from(it))) } ?: NullValue())
            .set("nextRun", nextRun?.let { TimestampValue(Timestamp.of(Date.from(it))) } ?: NullValue())
            .set("schedule", StringValue.of(schedule.name))
            .set("version", LongValue.of(version.toLong()))
            .set("participants", participants.map { it.marshall() })
            .build();
    }

    companion object Persistence {

        private fun unmarshall(entity: Entity): CoffeeTalkStateEntity {
            return CoffeeTalkStateEntity(
                entity.key.name,
                entity.getList<EntityValue>("participants").map(ParticipantEntity.Persistence::unmarshall),
                entity.getLong("round").toInt(),
                entity.getTimestamp("lastRun")?.toDate()?.toInstant(),
                entity.getTimestamp("nextRun")?.toDate()?.toInstant(),
                IntroFrequency.valueOf(entity.getString("schedule")),
                entity.getLong("version").toInt()
            )
        }

        fun find(channel: String): CoffeeTalkStateEntity? {
            val key = datastore.newKeyFactory().setKind(COFFEE_TALK_STATE_ENTITY).newKey(channel)
            return datastore.get(key)?.let { unmarshall(it) }
        }
    }
}