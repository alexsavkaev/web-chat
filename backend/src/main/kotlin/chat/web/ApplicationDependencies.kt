package chat.web

import chat.web.auth.GuestIdentityRepository
import chat.web.auth.GuestIdentityService
import chat.web.auth.InMemoryGuestIdentityRepository

/** Application composition root. Replace adapters here without coupling routes to storage. */
data class ApplicationDependencies(
    val guestIdentityService: GuestIdentityService
)

fun configureDependencies(
    guestIdentityRepository: GuestIdentityRepository = InMemoryGuestIdentityRepository()
): ApplicationDependencies = ApplicationDependencies(
    guestIdentityService = GuestIdentityService(guestIdentityRepository)
)
