package app.gamenative.wow.addons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurseForgeAccessTest {
    @Test
    fun normalizesHttpsProxies() {
        assertEquals("https://cf.example.com", CurseForgeAccess.normalizeProxy(" https://CF.example.com/ "))
        assertEquals("https://cf.example.com", CurseForgeAccess.normalizeProxy("cf.example.com"))
        assertEquals("https://cf.example.com:8443/api", CurseForgeAccess.normalizeProxy("https://cf.example.com:8443/api/"))
    }

    @Test
    fun rejectsUnsafeProxies() {
        assertNull(CurseForgeAccess.normalizeProxy("http://cf.example.com"))
        assertNull(CurseForgeAccess.normalizeProxy("https://user:pass@cf.example.com"))
        assertNull(CurseForgeAccess.normalizeProxy("https://cf.example.com/?key=1"))
        assertNull(CurseForgeAccess.normalizeProxy("https://cf.example.com/#x"))
        assertNull(CurseForgeAccess.normalizeProxy("https://"))
        assertNull(CurseForgeAccess.normalizeProxy(""))
    }

    @Test
    fun offWithoutKeyOrProxy() {
        val none = CurseForgeSetup()
        assertFalse(none.enabled)
        assertFalse(none.provider(AddonTarget.FOREVER).enabled)
    }

    @Test
    fun keyWinsOverProxy() {
        val both = CurseForgeSetup(apiKey = "secret", proxyUrl = "https://cf.example.com")
        assertTrue(both.usesKey)
        assertFalse(both.usesProxy)
        assertTrue(both.provider(AddonTarget.FOREVER).enabled)
    }

    @Test
    fun proxyAloneEnables() {
        val proxy = CurseForgeSetup(proxyUrl = "https://cf.example.com")
        assertTrue(proxy.usesProxy)
        assertTrue(proxy.provider(AddonTarget.FOREVER).enabled)
    }

    @Test
    fun toStringHidesKey() {
        assertFalse("secret" in CurseForgeSetup(apiKey = "secret").toString())
    }
}
