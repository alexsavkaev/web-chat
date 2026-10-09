package chat.web.database

import chat.web.auth.GuestIdentity
import java.util.UUID
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("integration")
@Testcontainers
class PostgresGuestIdentityRepositoryTest {
    private fun dataSource(): PGSimpleDataSource = PGSimpleDataSource().apply {
        setURL(postgres.getJdbcUrl())
        user = postgres.getUsername()
        password = postgres.getPassword()
    }

    @Test
    fun `Liquibase creates schema and table on a fresh PostgreSQL instance`() {
        val source = dataSource()
        migrateDatabase(source)
        source.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT to_regclass('webchat.guest_identities') IS NOT NULL").use { result ->
                    assertTrue(result.next() && result.getBoolean(1))
                }
                statement.executeQuery("SELECT COUNT(*) FROM webchat.DATABASECHANGELOGLOCK").use { result ->
                    assertTrue(result.next() && result.getInt(1) == 1)
                }
            }
        }
    }

    @Test
    fun `Liquibase does not reapply changeset on subsequent run`() {
        val source = dataSource()
        migrateDatabase(source)
        val changesetsBefore = countChangesets(source)
        val lockRowsBefore = countLockRows(source)
        migrateDatabase(source)
        assertEquals(1, changesetsBefore)
        assertEquals(changesetsBefore, countChangesets(source))
        assertEquals(1, lockRowsBefore)
        assertEquals(lockRowsBefore, countLockRows(source))
    }

    @Test
    fun `repository persists guest identity in PostgreSQL`() {
        val source = dataSource()
        migrateDatabase(source)
        val repository = PostgresGuestIdentityRepository(source)
        val identity = GuestIdentity(UUID.randomUUID().toString(), "TestGuest")
        repository.save(identity)
        val persistedName = source.connection.use { connection ->
            connection.prepareStatement("SELECT display_name FROM webchat.guest_identities WHERE id = ?").use { statement ->
                statement.setObject(1, UUID.fromString(identity.id))
                statement.executeQuery().use { result -> if (result.next()) result.getString("display_name") else null }
            }
        }
        assertEquals("TestGuest", persistedName)
    }

    @Test
    fun `registered identity duplicate email is reported as a controlled error`() {
        val source = dataSource()
        migrateDatabase(source)
        val repository = PostgresIdentityRepository(source)
        repository.save(chat.web.auth.RegisteredIdentity(UUID.randomUUID().toString(), "same@example.com", "First"), "hash")
        kotlin.test.assertFailsWith<chat.web.auth.DuplicateEmailException> {
            repository.save(chat.web.auth.RegisteredIdentity(UUID.randomUUID().toString(), "same@example.com", "Second"), "hash")
        }
    }

    private fun countChangesets(source: PGSimpleDataSource): Int = source.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM webchat.DATABASECHANGELOG WHERE ID = '1-create-guest-identities'").use { result ->
                result.next()
                result.getInt(1)
            }
        }
    }

    private fun countLockRows(source: PGSimpleDataSource): Int = source.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM webchat.DATABASECHANGELOGLOCK").use { result ->
                result.next()
                result.getInt(1)
            }
        }
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
