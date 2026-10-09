package chat.web.messages

import chat.web.rooms.CreateRoomCommand
import chat.web.rooms.InMemoryRoomService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `history cursor and limit return bounded older messages`() {
        val rooms = InMemoryRoomService()
        val room = rooms.create(CreateRoomCommand("Main", "owner"))
        val messages = InMemoryMessageService(rooms)
        val sent = (1..3).map { messages.send(SendMessageCommand(room.id, "owner", "message $it"))!! }

        assertEquals(listOf(sent[0].sequence, sent[1].sequence), messages.history(room.id, "owner", sent[2].sequence, 50).map { it.sequence })
        assertEquals(listOf(sent[1].sequence, sent[2].sequence), messages.history(room.id, "owner", limit = 2).map { it.sequence })
        assertTrue(messages.history(room.id, "owner", limit = 0).isEmpty())
        assertTrue(messages.history(room.id, " ").isEmpty())
    }

    @Test
    fun `message body validation rejects empty and oversized content`() {
        val rooms = InMemoryRoomService()
        val room = rooms.create(CreateRoomCommand("Main", "owner"))
        val messages = InMemoryMessageService(rooms)
        assertNull(messages.send(SendMessageCommand(room.id, "owner", " ")))
        assertNull(messages.send(SendMessageCommand(room.id, "owner", "x".repeat(4001))))
        assertNull(messages.send(SendMessageCommand(room.id, " ", "valid")))
        assertFalse(messages.history(room.id, "missing").isNotEmpty())
    }
}
