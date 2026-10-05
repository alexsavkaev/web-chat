package chat.web.auth

import java.util.UUID

/** Public authentication-module contract for an anonymous chat identity. */
data class GuestIdentity(val id: String, val displayName: String)

interface GuestIdentityRepository {
    fun save(identity: GuestIdentity): GuestIdentity
}

class InMemoryGuestIdentityRepository : GuestIdentityRepository {
    private val identities = mutableMapOf<String, GuestIdentity>()

    override fun save(identity: GuestIdentity): GuestIdentity {
        identities[identity.id] = identity
        return identity
    }
}

interface GuestIdentityContract {
    fun create(displayName: String): GuestIdentity?
}

class GuestIdentityService(
    private val repository: GuestIdentityRepository
) : GuestIdentityContract {
    override fun create(displayName: String): GuestIdentity? {
        val normalized = displayName.trim()
        if (normalized.isEmpty() || normalized.length > MAX_DISPLAY_NAME_LENGTH) return null
        return repository.save(GuestIdentity(UUID.randomUUID().toString(), normalized))
    }

    private companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 64
    }
}
