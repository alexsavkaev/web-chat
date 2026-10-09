package chat.web.database

import chat.web.auth.AuthSession
import chat.web.auth.DuplicateEmailException
import chat.web.auth.IdentityRepository
import chat.web.auth.RegisteredIdentity
import chat.web.auth.Role
import chat.web.auth.SessionRepository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class PostgresIdentityRepository(private val dataSource: DataSource) : IdentityRepository {
    override fun findByEmail(email: String): RegisteredIdentity? = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT id, email, display_name, role FROM webchat.identities WHERE email = ?").use { statement ->
            statement.setString(1, email)
            statement.executeQuery().use { result -> if (result.next()) result.toIdentity() else null }
        }
    }

    override fun save(identity: RegisteredIdentity, passwordHash: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("INSERT INTO webchat.identities (id, email, display_name, password_hash, role) VALUES (?, ?, ?, ?, ?)").use { statement ->
                statement.setObject(1, UUID.fromString(identity.id))
                statement.setString(2, identity.email)
                statement.setString(3, identity.displayName)
                statement.setString(4, passwordHash)
                statement.setString(5, identity.role.name)
                try {
                    statement.executeUpdate()
                } catch (failure: java.sql.SQLException) {
                    if (failure.sqlState == "23505") throw DuplicateEmailException()
                    throw failure
                }
            }
        }
    }

    override fun passwordHash(identityId: String): String? = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT password_hash FROM webchat.identities WHERE id = ?").use { statement ->
            statement.setObject(1, UUID.fromString(identityId))
            statement.executeQuery().use { result -> if (result.next()) result.getString(1) else null }
        }
    }

    override fun findById(id: String): RegisteredIdentity? = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT id, email, display_name, role FROM webchat.identities WHERE id = ?").use { statement ->
            statement.setObject(1, UUID.fromString(id))
            statement.executeQuery().use { result -> if (result.next()) result.toIdentity() else null }
        }
    }

    override fun updateProfile(id: String, displayName: String): RegisteredIdentity? = dataSource.connection.use { connection ->
        connection.prepareStatement("UPDATE webchat.identities SET display_name = ? WHERE id = ?").use { statement ->
            statement.setString(1, displayName)
            statement.setObject(2, UUID.fromString(id))
            if (statement.executeUpdate() == 0) return@use null
        }
        findById(id)
    }

    private fun java.sql.ResultSet.toIdentity() = RegisteredIdentity(getObject("id").toString(), getString("email"), getString("display_name"), Role.valueOf(getString("role")))
}

class PostgresSessionRepository(private val dataSource: DataSource) : SessionRepository {
    override fun save(session: AuthSession) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("INSERT INTO webchat.sessions (id, identity_id, token_hash, expires_at) VALUES (?, ?, ?, ?)").use { statement ->
                statement.setObject(1, UUID.fromString(session.id))
                statement.setObject(2, UUID.fromString(session.identityId))
                statement.setString(3, session.tokenHash)
                statement.setTimestamp(4, Timestamp.from(session.expiresAt))
                statement.executeUpdate()
            }
        }
    }

    override fun findByTokenHash(tokenHash: String): AuthSession? = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT id, identity_id, token_hash, expires_at, revoked_at FROM webchat.sessions WHERE token_hash = ?").use { statement ->
            statement.setString(1, tokenHash)
            statement.executeQuery().use { result ->
                if (!result.next()) null else AuthSession(result.getObject("id").toString(), result.getObject("identity_id").toString(), result.getString("token_hash"), result.getTimestamp("expires_at").toInstant(), result.getTimestamp("revoked_at")?.toInstant())
            }
        }
    }

    override fun revoke(id: String, at: Instant) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("UPDATE webchat.sessions SET revoked_at = ? WHERE id = ?").use { statement ->
                statement.setTimestamp(1, Timestamp.from(at))
                statement.setObject(2, UUID.fromString(id))
                statement.executeUpdate()
            }
        }
    }
}
