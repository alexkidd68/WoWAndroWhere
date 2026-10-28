package app.gamenative.wow.profile

import app.gamenative.wow.product.GameVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilesTest {

    @Test
    fun migrationMakesAProfilePerInstalledVersion() {
        val (list, active) = Profiles.migrate(
            installs = mapOf(GameVersion.FOREVER to "/wf", GameVersion.RETAIL to "/retail"),
            legacyRoot = "/335",
            hasLegacy = true,
            selected = GameVersion.PRIVATE_335,
        )
        assertEquals(listOf("forever", "retail", Profiles.LEGACY_MAIN), list.map { it.id })
        assertEquals(listOf("/wf", "/retail", "/335"), list.map { it.root })
        assertEquals("Private server (3.3.5a)", list.last().name)
        assertEquals(Profiles.LEGACY_MAIN, active)
    }

    @Test
    fun migrationWithNothingInstalledHasNoProfiles() {
        val (list, active) = Profiles.migrate(emptyMap(), "", hasLegacy = false, selected = GameVersion.FOREVER)
        assertTrue(list.isEmpty())
        assertEquals(null, active)
    }

    @Test
    fun migrationKeepsAPrivateServerSelectedWithoutAFolder() {
        val (list, active) = Profiles.migrate(mapOf(GameVersion.FOREVER to "/wf"), "", hasLegacy = false, selected = GameVersion.PRIVATE_335)
        assertEquals(listOf("forever", Profiles.LEGACY_MAIN), list.map { it.id })
        assertEquals(Profiles.LEGACY_MAIN, active)
    }

    @Test
    fun theOriginal335ProfileKeepsTheOldSettingsFile() {
        assertEquals("wow_legacy", Profiles.legacyPrefsFor(Profiles.LEGACY_MAIN))
        assertEquals("wow_legacy_ab12cd34", Profiles.legacyPrefsFor("ab12cd34"))
    }

    @Test
    fun profilesShareAHomeOnlyForTheSameVersionAndFolder() {
        assertEquals(Profile("a", "A", "private_335", "/c").home, Profile("b", "B", "private_335", "/c").home)
        assertTrue(Profile("a", "A", "forever", "/c").home != Profile("b", "B", "retail", "/c").home)
        assertEquals("", Profile("a", "A", "forever").home)
    }
}
