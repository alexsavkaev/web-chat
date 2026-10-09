package chat.web.database

import chat.web.auth.RegisteredIdentity
import chat.web.auth.Role
import chat.web.messages.PostgresMessageService
import chat.web.messages.SendMessageCommand
import chat.web.rooms.CreateRoomCommand
import chat.web.rooms.PostgresRoomService
import java.util.UUID
import javax.sql.DataSource
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
        val identities = PostgresIdentityRepository(source)
        val owner = RegisteredIdentity(UUID.randomUUID().toString(), "owner@example.com", "Owner", Role.USER)
        val guest = RegisteredIdentity(UUID.randomUUID().toString(), "guest@example.com", "Guest", Role.USER)
        identities.save(owner, "hash")
        identities.save(guest, "hash")

        val rooms = PostgresRoomService(source)
        val room = rooms.create(CreateRoomCommand("Durable room", owner.id))
        val joined = rooms.join(room.id, guest.id)
        assertNotNull(joined)
        assertTrue(rooms.isMember(room.id, guest.id))

        val messages = PostgresMessageService(rooms, PostgresMessageRepository(source))
        val sent = messages.send(SendMessageCommand(room.id, guest.id, "Persisted message"))
        assertNotNull(sent)
        assertTrue(sent.sequence > 0)

        val history = messages.history(room.id, guest.id)
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