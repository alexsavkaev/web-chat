package chat.web.auth

import java.util.UUID

/** Public authentication-module contract for an anonymous chat identity. */
data class GuestIdentity(val id: String, val displayName: String)

interface GuestIdentityContract {
    fun create(displayName: String): GuestIdentity?
}

class GuestIdentityService : GuestIdentityContract {
    override fun create(displayName: String): GuestIdentity? {
        val normalized = displayName.trim()
        if (normalized.isEmpty() || normalized.length > MAX_DISPLAY_NAME_LENGTH) return null
        return GuestIdentity(UUID.randomUUID().toString(), normalized)
    }

    private companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 64
    }
}
