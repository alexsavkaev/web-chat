package chat.web

import chat.web.auth.GuestIdentityRepository
import chat.web.auth.GuestIdentityService
import chat.web.database.migrateDatabase
import chat.web.database.PostgresGuestIdentityRepository
import chat.web.database.createDataSource
import chat.web.database.databaseSettings

/** Application composition root. Replace adapters here without coupling routes to storage. */
data class ApplicationDependencies(
    val guestIdentityRepository: GuestIdentityRepository,
    val guestIdentityService: GuestIdentityService
)

fun configureDependencies(
    guestIdentityRepository: GuestIdentityRepository? = null,
    environment: Map<String, String> = System.getenv()
): ApplicationDependencies {
    val repository = guestIdentityRepository ?: run {
        val settings = databaseSettings(environment)
        val dataSource = createDataSource(settings)
        migrateDatabase(dataSource)
        PostgresGuestIdentityRepository(dataSource)
    }
    return ApplicationDependencies(
        guestIdentityRepository = repository,
        guestIdentityService = GuestIdentityService(repository)
    )
}
