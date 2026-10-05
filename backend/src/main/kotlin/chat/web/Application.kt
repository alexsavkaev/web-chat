package chat.web

import chat.web.auth.GuestIdentity
import chat.web.auth.GuestIdentityService
import chat.web.auth.InMemoryGuestIdentityRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun Application.module() {
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    val guestIdentityService = GuestIdentityService(InMemoryGuestIdentityRepository())
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        post("/api/auth/guest") {
            val request = runCatching { call.receive<GuestLoginRequest>() }.getOrNull()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
                return@post
            }
            val identity = guestIdentityService.create(request.displayName)
            if (identity == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Display name must be 1-64 characters"))
                return@post
            }
            call.respond(HttpStatusCode.Created, identity.toResponse())
        }
    }
}

@Serializable
data class GuestLoginRequest(val displayName: String)

@Serializable
data class GuestIdentityResponse(val id: String, val displayName: String, val kind: String)

private fun GuestIdentity.toResponse() = GuestIdentityResponse(id, displayName, "guest")
