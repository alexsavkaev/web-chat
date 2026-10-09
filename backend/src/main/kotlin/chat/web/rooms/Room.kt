package chat.web.rooms

import java.util.UUID

/** Public room and membership contracts. IDs are opaque UUID strings at the API boundary. */
data class Room(val id: String, val name: String, val ownerId: String, val passwordProtected: Boolean)
data class RoomMember(val roomId: String, val userId: String, val role: RoomRole)
enum class RoomRole { OWNER, MODERATOR, MEMBER }

data class CreateRoomCommand(val name: String, val ownerId: String, val password: String? = null)

interface RoomContract {
    fun create(command: CreateRoomCommand): Room
    fun discover(): List<Room>
    fun join(roomId: String, userId: String, password: String? = null): RoomMember?
    fun leave(roomId: String, userId: String): Boolean
    fun members(roomId: String): List<RoomMember>
    fun isMember(roomId: String, userId: String): Boolean
    fun appointModerator(roomId: String, ownerId: String, userId: String): Boolean
}

class InMemoryRoomService : RoomContract {
    private val rooms = linkedMapOf<String, RoomRecord>()
    private val lock = Any()

    override fun create(command: CreateRoomCommand): Room = synchronized(lock) {
        val name = command.name.trim()
        require(name.isNotEmpty() && name.length <= 100) { "Room name must be 1-100 characters" }
        require(command.ownerId.isNotBlank()) { "Owner is required" }
        val password = command.password?.takeIf { it.isNotBlank() }
        val id = UUID.randomUUID().toString()
        val room = Room(id, name, command.ownerId, password != null)
        rooms[id] = RoomRecord(room, password, linkedMapOf(command.ownerId to RoomRole.OWNER))
        room
    }

    override fun discover(): List<Room> = synchronized(lock) { rooms.values.map { it.room } }

    override fun join(roomId: String, userId: String, password: String?): RoomMember? = synchronized(lock) {
        require(userId.isNotBlank()) { "User is required" }
        val record = rooms[roomId] ?: return null
        if (record.password != null && record.password != password) return null
        val role = record.members[userId] ?: RoomRole.MEMBER
        record.members[userId] = role
        RoomMember(roomId, userId, role)
    }

    override fun leave(roomId: String, userId: String): Boolean = synchronized(lock) {
        require(userId.isNotBlank()) { "User is required" }
        val record = rooms[roomId] ?: return false
        if (record.room.ownerId == userId) return false
        record.members.remove(userId) != null
    }

    override fun members(roomId: String): List<RoomMember> = synchronized(lock) {
        rooms[roomId]?.members?.map { RoomMember(roomId, it.key, it.value) } ?: emptyList()
    }

    override fun isMember(roomId: String, userId: String): Boolean = synchronized(lock) {
        require(userId.isNotBlank()) { "User is required" }
        rooms[roomId]?.members?.containsKey(userId) == true
    }

    override fun appointModerator(roomId: String, ownerId: String, userId: String): Boolean = synchronized(lock) {
        require(ownerId.isNotBlank() && userId.isNotBlank()) { "Users are required" }
        val record = rooms[roomId] ?: return false
        if (record.room.ownerId != ownerId || userId == ownerId || !record.members.containsKey(userId)) return false
        record.members[userId] = RoomRole.MODERATOR
        true
    }

    private data class RoomRecord(val room: Room, val password: String?, val members: LinkedHashMap<String, RoomRole>)
}
