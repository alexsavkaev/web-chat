package chat.web.auth

import chat.web.configureHttpModule
import chat.web.configureDependencies
import io.ktor.client.request.cookie
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class IdentityAuthRouteTest {
    @Test
    fun `registration sets secure HttpOnly session and readable csrf cookies`() = testApplication {
        application { configureHttpModule(configureDependencies(InMemoryGuestIdentityRepository()), SessionCookiePolicy(secure = true, sameSite = "Strict")) }
        val response = client.post("/api/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"email\":\"route@example.com\",\"displayName\":\"Route\",\"password\":\"long-password\"}")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val cookies = response.headers.getAll(HttpHeaders.SetCookie).orEmpty().joinToString(";")
        assertContains(cookies, "webchat_session=")
        assertContains(cookies, "HttpOnly")
        assertContains(cookies, "Secure")
        assertContains(cookies, "SameSite=Strict")
        assertContains(cookies, "webchat_csrf=")
    }

    @Test
    fun `anonymous profile is rejected and csrf is required for update`() = testApplication {
        application { configureHttpModule(configureDependencies(InMemoryGuestIdentityRepository()), SessionCookiePolicy(secure = false)) }
        val anonymous = client.put("/api/profile") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"displayName\":\"New\"}")
        }
        assertEquals(HttpStatusCode.Forbidden, anonymous.status)

        val register = client.post("/api/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"email\":\"csrf@example.com\",\"displayName\":\"Csrf\",\"password\":\"long-password\"}")
        }
        val setCookie = register.headers.getAll(HttpHeaders.SetCookie).orEmpty().joinToString(";")
        val session = Regex("webchat_session=([^;]+)").find(setCookie)!!.groupValues[1]
        val csrf = Regex("webchat_csrf=([^;]+)").find(setCookie)!!.groupValues[1]
        val rejected = client.put("/api/profile") {
            cookie(SESSION_COOKIE, session)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"displayName\":\"New\"}")
        }
        assertEquals(HttpStatusCode.Forbidden, rejected.status)
        val accepted = client.put("/api/profile") {
            cookie(SESSION_COOKIE, session)
            cookie(CSRF_COOKIE, csrf)
            header("X-CSRF-Token", csrf)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"displayName\":\"New\"}")
        }
        assertEquals(HttpStatusCode.OK, accepted.status)
        assertContains(accepted.bodyAsText(), "New")
    }

    @Test
    fun `login failures are generic, me requires auth, logout revokes session`() = testApplication {
        application { configureHttpModule(configureDependencies(InMemoryGuestIdentityRepository()), SessionCookiePolicy(secure = false)) }
        suspend fun badLogin(email: String) = client.post("/api/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"email\":\"$email\",\"password\":\"wrong-password\"}")
        }
        val unknown = badLogin("unknown@example.com")
        assertEquals(HttpStatusCode.Unauthorized, unknown.status)
        assertEquals("{\"error\":\"Invalid credentials\"}", unknown.bodyAsText())
        val register = client.post("/api/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"email\":\"login@example.com\",\"displayName\":\"Login\",\"password\":\"long-password\"}")
        }
        val setCookie = register.headers.getAll(HttpHeaders.SetCookie).orEmpty().joinToString(";")
        val session = Regex("webchat_session=([^;]+)").find(setCookie)!!.groupValues[1]
        val csrf = Regex("webchat_csrf=([^;]+)").find(setCookie)!!.groupValues[1]
        val me = client.get("/api/auth/me") { cookie(SESSION_COOKIE, session) }
        assertEquals(HttpStatusCode.OK, me.status)
        val loggedOut = client.post("/api/auth/logout") {
            cookie(SESSION_COOKIE, session); cookie(CSRF_COOKIE, csrf); header("X-CSRF-Token", csrf)
        }
        assertEquals(HttpStatusCode.NoContent, loggedOut.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/auth/me") { cookie(SESSION_COOKIE, session) }.status)
    }

    @Test
    fun `moderation endpoint denies normal users`() = testApplication {
        application { configureHttpModule(configureDependencies(InMemoryGuestIdentityRepository()), SessionCookiePolicy(secure = false)) }
        val register = client.post("/api/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"email\":\"user@example.com\",\"displayName\":\"User\",\"password\":\"long-password\"}")
        }
        val cookie = Regex("webchat_session=([^;]+)").find(register.headers.getAll(HttpHeaders.SetCookie).orEmpty().joinToString(";"))!!.groupValues[1]
        assertEquals(HttpStatusCode.Forbidden, client.get("/api/moderation/ping") { cookie(SESSION_COOKIE, cookie) }.status)
    }
}
