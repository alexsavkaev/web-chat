package chat.web.rooms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
}
