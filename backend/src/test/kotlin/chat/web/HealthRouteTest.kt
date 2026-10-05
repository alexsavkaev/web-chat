package chat.web

import chat.web.configureHttpModule
import chat.web.auth.InMemoryGuestIdentityRepository
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthRouteTest {
    @Test
    fun `health route reports service is ready`() = testApplication {
        application { configureHttpModule(InMemoryGuestIdentityRepository()) }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"status\":\"ok\""))
    }
}
