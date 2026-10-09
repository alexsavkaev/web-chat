package chat.web.rooms

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

/** PostgreSQL-backed room and membership service. Domain IDs are UUID strings. */
class PostgresRoomService(private val dataSource: DataSource) : RoomContract {
    override fun create(command: CreateRoomCommand): Room {
        val name = command.name.trim()
        require(name.isNotEmpty() && name.length <= 100) { "Room name must be 1-100 characters" }
        val ownerId = uuid(command.ownerId, "Owner")
        val password = command.password?.takeIf { it.isNotBlank() }
        val roomId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(
                    "INSERT INTO webchat.rooms (id, name, owner_id, password_hash) VALUES (?, ?, ?, ?)"
                ).use { statement ->
                    statement.setObject(1, roomId)
                    statement.setString(2, name)
                    statement.setObject(3, ownerId)
                    statement.setString(4, password?.let(::hash))
                    statement.executeUpdate()
                }
                addMember(connection, roomId, ownerId, RoomRole.OWNER)
                connection.commit()
            } catch (failure: Throwable) {
                connection.rollback()
                throw failure
            }
        }
        return Room(roomId.toString(), name, ownerId.toString(), password != null)
    }

    override fun discover(): List<Room> = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT id, name, owner_id, password_hash FROM webchat.rooms ORDER BY created_at").use { statement ->
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) add(room(result))
                }
            }
        }
    }

    override fun join(roomId: String, userId: String, password: String?): RoomMember? {
        val room = uuid(roomId, "Room")
        val user = uuid(userId, "User")
        return dataSource.connection.use { connection ->
            val allowed = connection.prepareStatement("SELECT password_hash FROM webchat.rooms WHERE id = ?").use { statement ->
                statement.setObject(1, room)
                statement.executeQuery().use { result ->
                    if (!result.next()) false
                    else {
                        val expected = result.getString("password_hash")
                        expected == null || expected == hash(password ?: "")
                    }
                }
            }
            if (!allowed) return@use null
            connection.prepareStatement(
                "INSERT INTO webchat.room_members (room_id, user_id, role) VALUES (?, ?, 'MEMBER') " +
                    "ON CONFLICT (room_id, user_id) DO NOTHING"
            ).use { statement ->
                statement.setObject(1, room)
                statement.setObject(2, user)
                statement.executeUpdate()
            }
            member(connection, room, user)
        }
    }

    override fun leave(roomId: String, userId: String): Boolean {
        val room = uuid(roomId, "Room")
        val user = uuid(userId, "User")
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM webchat.room_members WHERE room_id = ? AND user_id = ? " +
                    "AND role <> 'OWNER'"
            ).use { statement ->
                statement.setObject(1, room)
                statement.setObject(2, user)
                statement.executeUpdate() > 0
            }
        }
    }

    override fun members(roomId: String): List<RoomMember> {
        val room = uuid(roomId, "Room")
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT user_id, role FROM webchat.room_members WHERE room_id = ? ORDER BY joined_at").use { statement ->
                statement.setObject(1, room)
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) add(RoomMember(room.toString(), result.getObject("user_id", UUID::class.java).toString(), RoomRole.valueOf(result.getString("role"))))
                    }
                }
            }
        }
    }

    override fun isMember(roomId: String, userId: String): Boolean {
        val room = uuid(roomId, "Room")
        val user = uuid(userId, "User")
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT 1 FROM webchat.room_members WHERE room_id = ? AND user_id = ?").use { statement ->
                statement.setObject(1, room)
                statement.setObject(2, user)
                statement.executeQuery().use { it.next() }
            }
        }
    }

    override fun appointModerator(roomId: String, ownerId: String, userId: String): Boolean {
        val room = uuid(roomId, "Room")
        val owner = uuid(ownerId, "Owner")
        val user = uuid(userId, "User")
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE webchat.room_members SET role = 'MODERATOR' WHERE room_id = ? AND user_id = ? " +
                    "AND user_id <> ? AND role <> 'OWNER' AND EXISTS (SELECT 1 FROM webchat.rooms WHERE id = ? AND owner_id = ?)"
            ).use { statement ->
                statement.setObject(1, room)
                statement.setObject(2, user)
                statement.setObject(3, owner)
                statement.setObject(4, room)
                statement.setObject(5, owner)
                statement.executeUpdate() > 0
            }
        }
    }

    private fun addMember(connection: Connection, room: UUID, user: UUID, role: RoomRole) {
        connection.prepareStatement("INSERT INTO webchat.room_members (room_id, user_id, role) VALUES (?, ?, ?)").use { statement ->
            statement.setObject(1, room)
            statement.setObject(2, user)
            statement.setString(3, role.name)
            statement.executeUpdate()
        }
    }

    private fun member(connection: Connection, room: UUID, user: UUID): RoomMember? {
        connection.prepareStatement("SELECT role FROM webchat.room_members WHERE room_id = ? AND user_id = ?").use { statement ->
            statement.setObject(1, room)
            statement.setObject(2, user)
            statement.executeQuery().use { result ->
                return if (result.next()) RoomMember(room.toString(), user.toString(), RoomRole.valueOf(result.getString("role"))) else null
            }
        }
    }

    private fun room(result: java.sql.ResultSet) = Room(
        result.getObject("id", UUID::class.java).toString(), result.getString("name"),
        result.getObject("owner_id", UUID::class.java).toString(), result.getString("password_hash") != null
    )

    private fun uuid(value: String, label: String): UUID = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("$label must be a UUID")
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
