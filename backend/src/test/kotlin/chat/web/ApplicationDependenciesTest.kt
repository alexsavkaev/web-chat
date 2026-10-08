package chat.web

import chat.web.auth.GuestIdentityService
import chat.web.auth.InMemoryGuestIdentityRepository
import kotlin.test.Test
import kotlin.test.assertSame

class ApplicationDependenciesTest {
    @Test
    fun `configureDependencies uses injected repository as service backing store`() {
        val repository = RecordingGuestIdentityRepository()

        val dependencies = configureDependencies(repository)
        val result = dependencies.guestIdentityService.create("TestGuest")

        assertSame(repository.lastSavedIdentity, result)
    }

    private class RecordingGuestIdentityRepository : chat.web.auth.GuestIdentityRepository {
        var lastSavedIdentity: chat.web.auth.GuestIdentity? = null
            private set

        override fun save(identity: chat.web.auth.GuestIdentity): chat.web.auth.GuestIdentity {
            lastSavedIdentity = identity
            return identity
        }
    }
}
