package chat.web.auth

import chat.web.GuestIdentityResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class GuestLoginRequest(val displayName: String)

fun Route.guestAuthRoutes(service: GuestIdentityContract) {
    post("/api/auth/guest") {
        val request = runCatching { call.receive<GuestLoginRequest>() }.getOrNull()
        if (request == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
            return@post
        }
        val identity = service.create(request.displayName)
        if (identity == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Display name must be 1-64 characters"))
            return@post
        }
        call.respond(
            HttpStatusCode.Created,
            GuestIdentityResponse(identity.id, identity.displayName, "guest")
        )
    }
}
