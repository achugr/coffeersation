package com.achugr.coffeersation.slack

import com.slack.api.methods.AsyncMethodsClient
import com.slack.api.methods.response.conversations.ConversationsMembersResponse
import com.slack.api.methods.response.users.UsersInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

data class Member(val id: String)
class SlackService(private val client: AsyncMethodsClient) {

    /**
     * Get participants of a channel excluding bots.
     * Technically, just ids are needed, but to filter out the bots, it's necessary to get the user info.
     * If it's going to become slowly performing, it's possible to cache the user info, or just filter the bot itself,
     * and tell the user to remove other bots from the channel, but this is not really convenient.
     */
    suspend fun getMembers(channel: String): List<Member> =
        withContext(Dispatchers.IO) {
            getAllMembers(channel)
                .map { it.members.map { async { getUserInfo(it) } }.toList() }
                .toList()
                .flatten()
                .awaitAll()
                .filter { !it.user.isBot }
                .map { Member(it.user.id) }
                .toList()
        }

    suspend fun openConversation(members: List<Member>, message: String) {
        val conversation = client
            .conversationsOpen { it.users(members.map { p -> p.id }) }
            .await()
        client.chatPostMessage {
            it.channel(conversation.channel.id).text(message)
        }.await()
    }

    suspend fun postMessage(channel: String, message: String) {
        client.chatPostMessage {
            it.channel(channel).text(message)
        }.await()
    }

    private suspend fun getUserInfo(it: String?): UsersInfoResponse =
        client
            .usersInfo { builder -> builder.user(it) }
            .await()

    private suspend fun getAllMembers(channel: String): Flow<ConversationsMembersResponse> =
        flow {
            var response = getMembers(channel, null)
            do {
                emit(response)
                val nextCursor = response.responseMetadata.nextCursor
                val hasNextPage = nextCursor.isNotEmpty()
                if (hasNextPage) {
                    response = getMembers(channel, nextCursor)
                }
            } while (hasNextPage)
        }

    private suspend fun getMembers(channel: String, cursor: String? = null): ConversationsMembersResponse {
        return client.conversationsMembers { builder ->
            builder.channel(channel).limit(20).cursor(cursor)
        }.await()
    }
}