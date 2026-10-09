package chat.web.auth

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdentityAuthTest {
    @Test
    fun `registration login generic failures expiry revocation and profile update`() {
        val identities = InMemoryIdentityRepository()
        val sessions = InMemorySessionRepository()
        val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        val auth = AuthService(identities, sessions, clock, sessionLifetime = Duration.ofMinutes(5))
        val registered = assertNotNull(auth.register("user@example.com", "User", "long-password"))
        assertNull(auth.authenticate("missing@example.com", "long-password"))
        assertNull(auth.authenticate("user@example.com", "wrong-password"))
        val login = assertNotNull(auth.authenticate("USER@example.com", "long-password"))
        val raw = assertNotNull(login.rawToken)
        assertNull(sessions.findByTokenHash(raw))
        assertNotNull(auth.resolve(raw))
        assertEquals("New Name", auth.updateProfile(registered.id, " New Name ")?.displayName)
        auth.logout(raw)
        assertNull(auth.resolve(raw))
        val expiring = auth.createSession(registered)
        clock.advance(Duration.ofMinutes(6))
        assertNull(auth.resolve(expiring.rawToken))
    }

    @Test
    fun `authorization checks roles`() {
        val identities = InMemoryIdentityRepository()
        val auth = AuthService(identities, InMemorySessionRepository())
        val user = assertNotNull(auth.register("user@example.com", "User", "long-password"))
        val session = auth.authenticate("user@example.com", "long-password")!!
        val authorization = Authorization(auth)
        assertTrue(authorization.requireSession(session))
        assertFalse(authorization.require(session, Role.ADMIN, Role.MODERATOR))
        assertTrue(authorization.require(session, Role.USER))
        assertNull(authorization.authorizeWebSocket(session.rawToken, Role.MODERATOR))
        assertNotNull(authorization.authorizeWebSocket(session.rawToken, Role.USER))
    }

    private class MutableClock(private var instant: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = instant
        fun advance(duration: Duration) { instant = instant.plus(duration) }
    }
}
