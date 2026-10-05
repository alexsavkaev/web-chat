package chat.web.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GuestIdentityServiceTest {
    private val service = GuestIdentityService()

    @Test
    fun `guest identity trims and validates requested display name`() {
        assertEquals("GuestFox", service.create("  GuestFox  ")?.displayName)
    }

    @Test
    fun `guest identity rejects blank and long names`() {
        assertEquals(null, service.create("  "))
        assertEquals(null, service.create("x".repeat(65)))
    }

    @Test
    fun `each guest receives a distinct stable identifier`() {
        val first = service.create("Fox")!!
        val second = service.create("Fox")!!

        assertNotEquals(first.id, second.id)
        assertTrue(first.id.isNotBlank())
    }
}
