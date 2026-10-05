package chat.web.database

import chat.web.auth.GuestIdentity
import chat.web.auth.GuestIdentityRepository
import java.sql.Connection

class PostgresGuestIdentityRepository(
    private val connectionFactory: () -> Connection
) : GuestIdentityRepository {
    override fun save(identity: GuestIdentity): GuestIdentity = connectionFactory().use { connection ->
        connection.prepareStatement(
            "INSERT INTO webchat.guest_identities (id, display_name) VALUES (?, ?)"
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(identity.id))
            statement.setString(2, identity.displayName)
            statement.executeUpdate()
        }
        identity
    }
}
