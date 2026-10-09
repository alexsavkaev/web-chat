package chat.web.database

import chat.web.messages.ChatMessage
import chat.web.messages.MessageRepository
import java.util.UUID
import javax.sql.DataSource

class PostgresMessageRepository(private val dataSource: DataSource) : MessageRepository {
    override fun save(message: ChatMessage): ChatMessage = dataSource.connection.use { connection ->
        val messageId = uuid(message.id, "Message")
        val roomId = uuid(message.roomId, "Room")
        val authorId = uuid(message.authorId, "Author")
        connection.prepareStatement(
            "INSERT INTO webchat.messages (id, room_id, author_id, body) VALUES (?, ?, ?, ?) RETURNING sequence, created_at"
        ).use { statement ->
            statement.setObject(1, messageId)
            statement.setObject(2, roomId)
            statement.setObject(3, authorId)
            statement.setString(4, message.body)
            statement.executeQuery().use { result ->
                check(result.next()) { "Message insert returned no row" }
                message.copy(sequence = result.getLong("sequence"), createdAt = result.getTimestamp("created_at").toInstant())
            }
        }
    }

    override fun history(roomId: String, beforeSequence: Long?, limit: Int): List<ChatMessage> = dataSource.connection.use { connection ->
        val parsedRoomId = uuid(roomId, "Room")
        val cursor = if (beforeSequence == null) "" else " AND sequence < ?"
        connection.prepareStatement(
            "SELECT id, room_id, author_id, body, created_at, sequence FROM webchat.messages WHERE room_id = ?$cursor ORDER BY sequence DESC LIMIT ?"
        ).use { statement ->
            statement.setObject(1, parsedRoomId)
            var index = 2
            if (beforeSequence != null) statement.setLong(index++, beforeSequence)
            statement.setInt(index, limit)
            statement.executeQuery().use { result ->
                generateSequence { if (result.next()) result else null }.map {
                    ChatMessage(
                        it.getObject("id", UUID::class.java).toString(),
                        it.getObject("room_id", UUID::class.java).toString(),
                        it.getObject("author_id", UUID::class.java).toString(),
                        it.getString("body"), it.getTimestamp("created_at").toInstant(), it.getLong("sequence")
                    )
                }.toList().asReversed()
            }
        }
    }

    private fun uuid(value: String, label: String): UUID = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("$label must be a UUID")
    }
}
