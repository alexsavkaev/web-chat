package chat.web.database

import chat.web.configureDependencies
import chat.web.messages.SendMessageCommand
import chat.web.rooms.CreateRoomCommand
import chat.web.rooms.PostgresRoomService

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("integration")
@Testcontainers
class PostgresRoomMessageIntegrationTest {
    @Test
    fun `persists room membership and message history with foreign keys`() {
        val source = dataSource()
        migrateDatabase(source)
        val dependencies = configureDependencies(dataSourceOverride = source)
        val owner = dependencies.authService.register("owner@example.com", "Owner", "owner-password")
        val guestSession = dependencies.authService.register("guest@example.com", "Guest", "guest-password")
        assertNotNull(owner)
        assertNotNull(guestSession)
        val authenticated = dependencies.authService.authenticate("guest@example.com", "guest-password")
        assertNotNull(authenticated)
        val resolved = dependencies.authorization.authenticate(authenticated.rawToken)
        assertNotNull(resolved)
        assertEquals(guestSession.id, resolved.identity.id)

        val room = dependencies.roomService.create(CreateRoomCommand("Durable room", owner.id))
        val joined = dependencies.roomService.join(room.id, resolved.identity.id)
        assertNotNull(joined)
        assertTrue(dependencies.roomService.isMember(room.id, resolved.identity.id))

        val sent = dependencies.messageService.send(SendMessageCommand(room.id, resolved.identity.id, "Persisted message"))
        assertNotNull(sent)
        assertTrue(sent.sequence > 0)

        val history = dependencies.messageService.history(room.id, resolved.identity.id)
        assertEquals(listOf("Persisted message"), history.map { it.body })
        assertEquals(sent.id, history.single().id)

        source.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM webchat.messages WHERE id = ?").use { statement ->
                statement.setObject(1, UUID.fromString(sent.id))
                statement.executeQuery().use { result ->
                    assertTrue(result.next() && result.getInt(1) == 1)
                }
            }
        }
    }

    @Test
    fun `rejects non UUID domain identifiers at the PostgreSQL boundary`() {
        val source = dataSource()
        migrateDatabase(source)
        val rooms = PostgresRoomService(source)

        assertThrows(IllegalArgumentException::class.java) {
            rooms.join("not-a-uuid", UUID.randomUUID().toString())
        }
    }

    private fun dataSource(): PGSimpleDataSource = PGSimpleDataSource().apply {
        setURL(postgres.getJdbcUrl())
        user = postgres.getUsername()
        password = postgres.getPassword()
    }

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("webchat")
            withPassword("webchat")
        }
    }
}