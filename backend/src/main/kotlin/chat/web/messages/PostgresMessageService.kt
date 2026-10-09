package chat.web.messages

import chat.web.rooms.RoomContract
import java.time.Instant
import java.util.UUID

class PostgresMessageService(private val rooms: RoomContract, private val repository: MessageRepository) : MessageContract {
    override fun send(command: SendMessageCommand): ChatMessage? {
        val body = command.body.trim()
        if (command.authorId.isBlank() || body.isEmpty() || body.length > 4000 || !rooms.isMember(command.roomId, command.authorId)) return null
        return repository.save(ChatMessage(UUID.randomUUID().toString(), command.roomId, command.authorId, body, Instant.now(), 0))
    }

    override fun history(roomId: String, requesterId: String, beforeSequence: Long?, limit: Int): List<ChatMessage> {
        if (requesterId.isBlank() || limit !in 1..100 || !rooms.isMember(roomId, requesterId)) return emptyList()
        return repository.history(roomId, beforeSequence, limit)
    }
}
