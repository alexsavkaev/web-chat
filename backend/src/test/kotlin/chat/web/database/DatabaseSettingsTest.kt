package chat.web.database

import chat.web.database.databaseSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseSettingsTest {
    @Test
    fun `uses local development defaults`() {
        val settings = databaseSettings(emptyMap())

        assertEquals("jdbc:postgresql://localhost:5432/webchat", settings.jdbcUrl)
        assertEquals("webchat", settings.username)
        assertEquals("webchat", settings.password)
    }

    @Test
    fun `environment variables override database defaults`() {
        val settings = databaseSettings(
            mapOf(
                "DATABASE_URL" to "jdbc:postgresql://db:5432/chat",
                "DATABASE_USER" to "chatuser",
                "DATABASE_PASSWORD" to "secret"
            )
        )

        assertEquals("jdbc:postgresql://db:5432/chat", settings.jdbcUrl)
        assertEquals("chatuser", settings.username)
        assertEquals("secret", settings.password)
    }
}
