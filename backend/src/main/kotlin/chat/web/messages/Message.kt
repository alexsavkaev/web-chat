package chat.web.messages

import chat.web.rooms.RoomContract
import java.time.Instant
import java.util.UUID

/** In-memory room-message contract. Access checks are performed before persistence. */
data class ChatMessage(
    val id: String,
    val roomId: String,
    val authorId: String,
    val body: String,
    val createdAt: Instant,
    val sequence: Long
)

data class SendMessageCommand(val roomId: String, val authorId: String, val body: String)

interface MessageContract {
    fun send(command: SendMessageCommand): ChatMessage?
    fun history(roomId: String, requesterId: String, beforeSequence: Long? = null, limit: Int = 50): List<ChatMessage>
}

interface MessageRepository {
    fun save(message: ChatMessage): ChatMessage
    fun history(roomId: String, beforeSequence: Long?, limit: Int): List<ChatMessage>
}

class InMemoryMessageService(private val rooms: RoomContract) : MessageContract {
    private val messages = linkedMapOf<String, MutableList<ChatMessage>>()
    private val lock = Any()
    private var nextSequence = 1L

    override fun send(command: SendMessageCommand): ChatMessage? = synchronized(lock) {
        if (command.authorId.isBlank()) return null
        if (!rooms.isMember(command.roomId, command.authorId)) return null
        val body = command.body.trim()
        if (body.isEmpty() || body.length > 4000) return null
        val message = ChatMessage(
            UUID.randomUUID().toString(), command.roomId, command.authorId, body,
            Instant.now(), nextSequence++
        )
        messages.getOrPut(command.roomId) { mutableListOf() }.add(message)
        message
    }

    override fun history(
        roomId: String,
        requesterId: String,
        beforeSequence: Long?,
        limit: Int
    ): List<ChatMessage> = synchronized(lock) {
        if (requesterId.isBlank()) return emptyList()
        if (!rooms.isMember(roomId, requesterId) || limit !in 1..100) return emptyList()
        messages[roomId].orEmpty()
            .asSequence()
            .filter { beforeSequence == null || it.sequence < beforeSequence }
            .toList()
            .takeLast(limit)
    }
}
