package chat.web

import chat.web.auth.GuestIdentityRepository
import chat.web.auth.GuestIdentityService
import chat.web.database.migrateDatabase
import chat.web.database.PostgresGuestIdentityRepository
import chat.web.database.PostgresIdentityRepository
import chat.web.database.PostgresSessionRepository
import chat.web.database.createDataSource
import chat.web.database.databaseSettings
import chat.web.auth.AuthService
import chat.web.auth.Authorization
import chat.web.auth.IdentityRepository
import chat.web.auth.InMemoryIdentityRepository
import chat.web.auth.InMemorySessionRepository

/** Application composition root. Replace adapters here without coupling routes to storage. */
data class ApplicationDependencies(
    val guestIdentityRepository: GuestIdentityRepository,
    val guestIdentityService: GuestIdentityService,
    val authService: AuthService,
    val authorization: Authorization
)

fun configureDependencies(
    guestIdentityRepository: GuestIdentityRepository? = null,
    environment: Map<String, String> = System.getenv(),
    identityRepository: IdentityRepository? = null
): ApplicationDependencies {
    val dataSource = if (guestIdentityRepository == null && identityRepository == null) {
        val settings = databaseSettings(environment)
        createDataSource(settings)
    } else null
    val repository = guestIdentityRepository ?: run {
        checkNotNull(dataSource)
        migrateDatabase(dataSource)
        PostgresGuestIdentityRepository(dataSource)
    }
    val durableIdentity = identityRepository ?: dataSource?.let { PostgresIdentityRepository(it) } ?: InMemoryIdentityRepository()
    val sessions = dataSource?.let { PostgresSessionRepository(it) } ?: InMemorySessionRepository()
    val auth = AuthService(durableIdentity, sessions)
    return ApplicationDependencies(
        guestIdentityRepository = repository,
        guestIdentityService = GuestIdentityService(repository),
        authService = auth,
        authorization = Authorization(auth)
    )
}
