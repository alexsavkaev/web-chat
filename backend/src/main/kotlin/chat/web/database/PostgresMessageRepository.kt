package chat.web.database

import chat.web.messages.ChatMessage
import chat.web.messages.MessageRepository
import java.util.UUID
import javax.sql.DataSource

class PostgresMessageRepository(private val dataSource: DataSource) : MessageRepository {
    override fun save(message: ChatMessage): ChatMessage = dataSource.connection.use { connection ->
        connection.prepareStatement(
            "INSERT INTO webchat.messages (id, room_id, author_id, body) VALUES (?, ?, ?, ?) RETURNING sequence, created_at"
        ).use { statement ->
            statement.setObject(1, UUID.fromString(message.id))
            statement.setObject(2, UUID.fromString(message.roomId))
            statement.setObject(3, UUID.fromString(message.authorId))
            statement.setString(4, message.body)
            statement.executeQuery().use { result ->
                check(result.next()) { "Message insert returned no row" }
                message.copy(sequence = result.getLong("sequence"), createdAt = result.getTimestamp("created_at").toInstant())
            }
        }
    }

    override fun history(roomId: String, beforeSequence: Long?, limit: Int): List<ChatMessage> = dataSource.connection.use { connection ->
        val cursor = if (beforeSequence == null) "" else " AND sequence < ?"
        connection.prepareStatement(
            "SELECT id, room_id, author_id, body, created_at, sequence FROM webchat.messages WHERE room_id = ?$cursor ORDER BY sequence DESC LIMIT ?"
        ).use { statement ->
            statement.setObject(1, UUID.fromString(roomId))
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
}
