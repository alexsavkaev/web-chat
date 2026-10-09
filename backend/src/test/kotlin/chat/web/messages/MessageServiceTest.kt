package chat.web.messages

import chat.web.rooms.CreateRoomCommand
import chat.web.rooms.InMemoryRoomService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MessageServiceTest {
    @Test
    fun `only members can send and read ordered history`() {
        val rooms = InMemoryRoomService()
        val room = rooms.create(CreateRoomCommand("Main", "owner"))
        val messages = InMemoryMessageService(rooms)
        assertNull(messages.send(SendMessageCommand(room.id, "guest", "no access")))
        rooms.join(room.id, "guest")
        val first = messages.send(SendMessageCommand(room.id, "guest", " first "))
        val second = messages.send(SendMessageCommand(room.id, "owner", "second"))
        assertNotNull(first)
        assertNotNull(second)
        assertEquals("first", first.body)
        assertEquals(listOf(first.sequence, second.sequence), messages.history(room.id, "guest").map { it.sequence })
    }
}
