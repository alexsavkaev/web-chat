package chat.web.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.http.Cookie
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val displayName: String, val password: String)
@Serializable
data class LoginRequest(val email: String, val password: String)
@Serializable
data class ProfileUpdateRequest(val displayName: String)
@Serializable
data class IdentityResponse(val id: String, val email: String, val displayName: String, val role: String)
@Serializable
data class ErrorResponse(val error: String)

fun Route.identityAuthRoutes(auth: AuthService, authz: Authorization, cookiePolicy: SessionCookiePolicy = SessionCookiePolicy()) {
    fun IdentityResponse(identity: RegisteredIdentity) = IdentityResponse(identity.id, identity.email, identity.displayName, identity.role.name.lowercase())
    fun csrfValid(call: io.ktor.server.application.ApplicationCall): Boolean {
        val expected = call.request.cookies[CSRF_COOKIE]
        return expected != null && expected == call.request.header("X-CSRF-Token")
    }
    fun issueCsrf(call: io.ktor.server.application.ApplicationCall) {
        val token = java.util.UUID.randomUUID().toString()
        call.response.cookies.append(Cookie(CSRF_COOKIE, token, path = "/", maxAge = cookiePolicy.maxAgeSeconds, httpOnly = false, secure = cookiePolicy.secure, extensions = mapOf("SameSite" to cookiePolicy.sameSite)))
    }
    post("/api/auth/register") {
        val request = runCatching { call.receive<RegisterRequest>() }.getOrNull()
        val identity = request?.let { auth.register(it.email, it.displayName, it.password) }
        if (identity == null) { call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid registration")); return@post }
        val loggedIn = auth.createSession(identity)
        call.response.cookies.append(sessionCookie(loggedIn.rawToken!!, cookiePolicy))
        issueCsrf(call)
        call.respond(HttpStatusCode.Created, IdentityResponse(identity))
    }
    post("/api/auth/login") {
        val request = runCatching { call.receive<LoginRequest>() }.getOrNull()
        val result = request?.let { auth.authenticate(it.email, it.password) }
        if (result == null) { call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials")); return@post }
        call.response.cookies.append(sessionCookie(result.rawToken!!, cookiePolicy))
        issueCsrf(call)
        call.respond(IdentityResponse(result.identity))
    }
    post("/api/auth/logout") {
        val token = call.request.cookies[SESSION_COOKIE]
        if (token != null && !csrfValid(call)) { call.respond(HttpStatusCode.Forbidden, ErrorResponse("CSRF validation failed")); return@post }
        auth.logout(token)
        call.response.cookies.append(Cookie(SESSION_COOKIE, "", path = "/", maxAge = 0, httpOnly = true, secure = cookiePolicy.secure, extensions = mapOf("SameSite" to cookiePolicy.sameSite)))
        call.response.cookies.append(Cookie(CSRF_COOKIE, "", path = "/", maxAge = 0, httpOnly = false, secure = cookiePolicy.secure, extensions = mapOf("SameSite" to cookiePolicy.sameSite)))
        call.respond(HttpStatusCode.NoContent)
    }
    get("/api/auth/me") {
        val current = authz.authenticate(call.request.cookies[SESSION_COOKIE])
        if (current == null) { call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Authentication required")); return@get }
        call.respond(IdentityResponse(current.identity))
    }
    put("/api/profile") {
        if (!csrfValid(call)) { call.respond(HttpStatusCode.Forbidden, ErrorResponse("CSRF validation failed")); return@put }
        val current = authz.authenticate(call.request.cookies[SESSION_COOKIE])
        if (!authz.requireSession(current)) { call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Authentication required")); return@put }
        val request = runCatching { call.receive<ProfileUpdateRequest>() }.getOrNull()
        val name = request?.displayName?.trim()
        if (name.isNullOrEmpty() || name.length > 64) { call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid profile")); return@put }
        val updated = auth.updateProfile(current!!.identity.id, name)
        if (updated == null) { call.respond(HttpStatusCode.NotFound, ErrorResponse("Profile not found")); return@put }
        call.respond(IdentityResponse(updated))
    }
    get("/api/moderation/ping") {
        val current = authz.authenticate(call.request.cookies[SESSION_COOKIE])
        if (!authz.require(current, Role.MODERATOR, Role.ADMIN)) {
            call.respond(HttpStatusCode.Forbidden, ErrorResponse("Moderator role required")); return@get
        }
        call.respond(mapOf("status" to "ok"))
    }
}
