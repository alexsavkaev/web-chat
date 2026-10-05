package chat.web

import chat.web.auth.guestAuthRoutes
import chat.web.auth.GuestIdentityRepository
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun Application.module() = configureHttpModule(configureDependencies().guestIdentityRepository)

fun Application.configureHttpModule(
    guestIdentityRepository: GuestIdentityRepository
) {
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    val dependencies = configureDependencies(guestIdentityRepository)
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        guestAuthRoutes(dependencies.guestIdentityService)
    }
}

@Serializable
data class GuestIdentityResponse(val id: String, val displayName: String, val kind: String)
