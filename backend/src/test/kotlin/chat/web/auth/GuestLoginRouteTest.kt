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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GuestIdentityPayload(val id: String, val displayName: String, val kind: String)

private fun String.guestPayload(): GuestIdentityPayload = Json.decodeFromString(this)


class GuestLoginRouteTest {
    @Test
    fun `guest login issues an anonymous identity`() = testApplication {
        application { module() }

        val response = client.post("/api/auth/guest") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"displayName\":\"  Fox  \"}")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val payload = response.bodyAsText().guestPayload()
        assertEquals("Fox", payload.displayName)
        assertEquals("guest", payload.kind)
        assertEquals(36, payload.id.length)
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

    @Test
    fun `guest login rejects malformed JSON`() = testApplication {
        application { module() }

        val response = client.post("/api/auth/guest") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{invalid-json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `guest login rejects missing display name`() = testApplication {
        application { module() }

        val response = client.post("/api/auth/guest") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
