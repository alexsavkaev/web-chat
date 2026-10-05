package chat.web.auth

import chat.web.module
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuestLoginRouteTest {
    @Test
    fun `guest login issues an anonymous identity`() = testApplication {
        application { module() }

        val response = client.post("/api/auth/guest") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"displayName\":\"  Fox  \"}")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"displayName\":\"Fox\""))
        assertTrue(response.bodyAsText().contains("\"id\":"))
    }

    @Test
    fun `guest login rejects blank display name`() = testApplication {
        application { module() }

        val response = client.post("/api/auth/guest") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"displayName\":\"  \"}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
