package dev.gleroy.ivanachess.api.broker

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.*

/**
 * Join message type.
 */
private const val JoinMessageType = "join"

/**
 * Leave message type.
 */
private const val LeaveMessageType = "leave"

/**
 * Matchmaking message.
 */
@JsonTypeInfo(
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = MatchmakingMessage.Join::class, name = JoinMessageType),
    JsonSubTypes.Type(value = MatchmakingMessage.Leave::class, name = LeaveMessageType),
)
internal sealed class MatchmakingMessage {
    /**
     * Join message.
     *
     * @param userId ID of user to add in matchmaking queue.
     */
    data class Join(
        val userId: UUID
    ) : MatchmakingMessage() {
        override val type get() = JoinMessageType
    }

    /**
     * Leave message.
     *
     * @param userId ID of user to remove from matchmaking queue.
     */
    data class Leave(
        val userId: UUID
    ) : MatchmakingMessage() {
        override val type get() = LeaveMessageType
    }

    /**
     * Message type.
     */
    abstract val type: String
}
