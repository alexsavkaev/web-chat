package chat.web.database

import chat.web.auth.GuestIdentity
import java.util.UUID
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import kotlin.test.assertEquals

@Tag("integration")
class PostgresGuestIdentityRepositoryTest {
    private val dataSource = PGSimpleDataSource().apply {
        setURL(System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/chatdb_test")
        user = System.getenv("DATABASE_USER") ?: "chat"
        password = System.getenv("DATABASE_PASSWORD") ?: "chat"
    }

    @Test
    fun `save persists guest identity in postgres`() {
        migrateDatabase(dataSource)
        val repository = PostgresGuestIdentityRepository(dataSource::getConnection)
        val identity = GuestIdentity(UUID.randomUUID().toString(), "TestGuest")

        repository.save(identity)
        val persistedName = dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT display_name FROM webchat.guest_identities WHERE id = ?").use { statement ->
                statement.setObject(1, UUID.fromString(identity.id))
                statement.executeQuery().use { result ->
                    if (result.next()) result.getString("display_name") else null
                }
            }
        }

        assertEquals("TestGuest", persistedName)
        dataSource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM webchat.guest_identities WHERE id = ?").use { statement ->
                statement.setObject(1, UUID.fromString(identity.id))
                statement.executeUpdate()
            }
        }
    }

    @Test
    fun `migration creates guest identities table`() {
        migrateDatabase(dataSource)

        val exists = dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT to_regclass('webchat.guest_identities') IS NOT NULL").use { statement ->
                statement.executeQuery().use { result ->
                    result.next() && result.getBoolean(1)
                }
            }
        }
        assertEquals(true, exists)
    }
}
