package chat.web.auth

import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class Role { USER, MODERATOR, ADMIN }
data class RegisteredIdentity(val id: String, val email: String, val displayName: String, val role: Role = Role.USER)
data class AuthSession(val id: String, val identityId: String, val tokenHash: String, val expiresAt: Instant, var revokedAt: Instant? = null)
data class AuthenticatedIdentity(val identity: RegisteredIdentity, val session: AuthSession, val rawToken: String? = null)

interface IdentityRepository {
    fun findByEmail(email: String): RegisteredIdentity?
    fun save(identity: RegisteredIdentity, passwordHash: String)
    fun passwordHash(identityId: String): String?
    fun findById(id: String): RegisteredIdentity?
    fun updateProfile(id: String, displayName: String): RegisteredIdentity?
}
interface SessionRepository {
    fun save(session: AuthSession)
    fun findByTokenHash(tokenHash: String): AuthSession?
    fun revoke(id: String, at: Instant)
}

class InMemoryIdentityRepository : IdentityRepository {
    private val identities = mutableMapOf<String, RegisteredIdentity>()
    private val passwords = mutableMapOf<String, String>()
    override fun findByEmail(email: String) = identities.values.firstOrNull { it.email == email }
    override fun save(identity: RegisteredIdentity, passwordHash: String) { identities[identity.id] = identity; passwords[identity.id] = passwordHash }
    override fun passwordHash(identityId: String) = passwords[identityId]
    override fun findById(id: String) = identities[id]
    override fun updateProfile(id: String, displayName: String) = identities[id]?.copy(displayName = displayName.trim())?.also { identities[id] = it }
}
class InMemorySessionRepository : SessionRepository {
    private val sessions = mutableMapOf<String, AuthSession>()
    override fun save(session: AuthSession) { sessions[session.id] = session }
    override fun findByTokenHash(tokenHash: String) = sessions.values.firstOrNull { it.tokenHash == tokenHash }
    override fun revoke(id: String, at: Instant) { sessions[id]?.revokedAt = at }
}

data class SessionCookiePolicy(val secure: Boolean = true, val sameSite: String = "Strict", val maxAgeSeconds: Int = 86_400)
class PasswordHasher {
    private val random = SecureRandom()
    fun hash(password: String): String {
        val salt = ByteArray(16).also(random::nextBytes)
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return "pbkdf2$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derived)
    }
    fun verify(password: String, stored: String): Boolean = runCatching {
        val parts = stored.split('$'); if (parts.size != 3 || parts[0] != "pbkdf2") return false
        val salt = Base64.getDecoder().decode(parts[1]); val expected = Base64.getDecoder().decode(parts[2])
        val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, 120_000, expected.size * 8)).encoded
        MessageDigest.isEqual(actual, expected)
    }.getOrDefault(false)
}

class AuthService(
    private val identities: IdentityRepository,
    private val sessions: SessionRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val hasher: PasswordHasher = PasswordHasher(),
    private val sessionLifetime: Duration = Duration.ofHours(24)
) {
    private val random = SecureRandom()
    fun updateProfile(id: String, displayName: String) = identities.updateProfile(id, displayName)
    fun register(email: String, displayName: String, password: String): RegisteredIdentity? {
        val normalized = email.trim().lowercase()
        if (!EMAIL_REGEX.matches(normalized) || displayName.trim().isEmpty() || displayName.trim().length > 64 || password.length < 8 || identities.findByEmail(normalized) != null) return null
        return RegisteredIdentity(UUID.randomUUID().toString(), normalized, displayName.trim()).also { identities.save(it, hasher.hash(password)) }
    }
    fun authenticate(email: String, password: String): AuthenticatedIdentity? {
        val identity = identities.findByEmail(email.trim().lowercase()) ?: return null
        val stored = identities.passwordHash(identity.id) ?: return null
        if (!hasher.verify(password, stored)) return null
        return createSession(identity)
    }
    fun createSession(identity: RegisteredIdentity): AuthenticatedIdentity {
        val raw = ByteArray(32).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
        val session = AuthSession(UUID.randomUUID().toString(), identity.id, tokenHash(token), clock.instant().plus(sessionLifetime))
        sessions.save(session)
        return AuthenticatedIdentity(identity, session, token)
    }
    fun resolve(rawToken: String?): AuthenticatedIdentity? {
        if (rawToken.isNullOrBlank()) return null
        val session = sessions.findByTokenHash(tokenHash(rawToken)) ?: return null
        if (session.revokedAt != null || !session.expiresAt.isAfter(clock.instant())) return null
        return identities.findById(session.identityId)?.let { AuthenticatedIdentity(it, session) }
    }
    fun logout(rawToken: String?) { if (!rawToken.isNullOrBlank()) sessions.findByTokenHash(tokenHash(rawToken))?.let { sessions.revoke(it.id, clock.instant()) } }
    fun tokenHash(token: String) = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
    companion object { val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") }
}

class Authorization(private val auth: AuthService) {
    fun authenticate(token: String?) = auth.resolve(token)
    fun require(identity: AuthenticatedIdentity?, vararg roles: Role): Boolean = identity != null && (roles.isEmpty() || identity.identity.role in roles)
    fun requireSession(identity: AuthenticatedIdentity?) = identity != null

    /** Shared boundary for WebSocket handshakes and message authorization. */
    fun authorizeWebSocket(sessionToken: String?, vararg roles: Role): AuthenticatedIdentity? {
        val identity = authenticate(sessionToken)
        return identity?.takeIf { require(it, *roles) }
    }
}

const val SESSION_COOKIE = "webchat_session"
const val CSRF_COOKIE = "webchat_csrf"
fun sessionCookie(token: String, policy: SessionCookiePolicy) = Cookie(SESSION_COOKIE, token, maxAge = policy.maxAgeSeconds, httpOnly = true, secure = policy.secure, extensions = mapOf("SameSite" to policy.sameSite), encoding = CookieEncoding.RAW)
