package chat.web.rooms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RoomServiceTest {
    @Test
    fun `joining requires password and creates membership`() {
        val service = InMemoryRoomService()
        val room = service.create(CreateRoomCommand("Private", "owner", "secret"))
        assertNull(service.join(room.id, "guest", "wrong"))
        val member = service.join(room.id, "guest", "secret")
        assertNotNull(member)
        assertEquals(RoomRole.MEMBER, member.role)
        assertEquals(2, service.members(room.id).size)
    }

    @Test
    fun `owner cannot leave`() {
        val service = InMemoryRoomService()
        val room = service.create(CreateRoomCommand("Main", "owner"))
        assertFalse(service.leave(room.id, "owner"))
    }

    @Test
    fun `blank password creates a public room`() {
        val service = InMemoryRoomService()
        val room = service.create(CreateRoomCommand("Main", "owner", "   "))

        assertFalse(room.passwordProtected)
        assertNotNull(service.join(room.id, "guest"))
    }

    @Test
    fun `room and member inputs are validated`() {
        val service = InMemoryRoomService()
        assertFailsWith<IllegalArgumentException> { service.create(CreateRoomCommand(" ", "owner")) }
        assertFailsWith<IllegalArgumentException> { service.create(CreateRoomCommand("Main", " ")) }
        val room = service.create(CreateRoomCommand("Main", "owner"))
        assertNull(service.join("missing", "guest"))
        assertFailsWith<IllegalArgumentException> { service.join(room.id, " ") }
        assertFailsWith<IllegalArgumentException> { service.leave(room.id, " ") }
        assertFailsWith<IllegalArgumentException> { service.isMember(room.id, " ") }
        assertTrue(service.isMember(room.id, "owner"))
    }

    @Test
    fun `only owner can appoint an existing member as moderator`() {
        val service = InMemoryRoomService()
        val room = service.create(CreateRoomCommand("Main", "owner"))
        service.join(room.id, "member")

        assertFalse(service.appointModerator(room.id, "member", "member"))
        assertTrue(service.appointModerator(room.id, "owner", "member"))
        assertEquals(RoomRole.MODERATOR, service.members(room.id).single { it.userId == "member" }.role)
        assertFalse(service.appointModerator(room.id, "owner", "owner"))
        assertFalse(service.appointModerator(room.id, "owner", "missing"))
    }
}
