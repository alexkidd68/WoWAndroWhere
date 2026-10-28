package app.gamenative.wow.legacy.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatchRulesTest {

    private val allOff = listOf("A", "U", "M", "N", "J", "K", "6").associateWith { false }

    @Test
    fun turningOnAPatchTurnsOnWhatItNeeds() {
        assertEquals(mapOf("U" to true, "A" to true), PatchRules.plan(allOff, "U", true))
        assertEquals(mapOf("N" to true, "M" to true), PatchRules.plan(allOff, "N", true))
    }

    @Test
    fun turningOffAPatchTurnsOffWhatNeedsIt() {
        val on = allOff + mapOf("A" to true, "U" to true)
        assertEquals(mapOf("A" to false, "U" to false), PatchRules.plan(on, "A", false))
        assertEquals(mapOf("U" to false), PatchRules.plan(on, "U", false))
    }

    @Test
    fun characterCreationScreensAreOneAtATime() {
        val j = allOff + ("J" to true)
        assertEquals(mapOf("K" to true, "J" to false), PatchRules.plan(j, "K", true))
        assertEquals(mapOf("J" to false), PatchRules.plan(j, "J", false))
    }

    @Test
    fun missingPatchesAndUnrelatedOnesStayAlone() {
        assertEquals(mapOf("U" to true), PatchRules.plan(mapOf("U" to false), "U", true))
        assertEquals(mapOf("6" to true), PatchRules.plan(allOff, "6", true))
        assertEquals(emptyMap<String, Boolean>(), PatchRules.plan(allOff, "6", false))
    }

    @Test
    fun hintsNameTheOtherPatch() {
        val names = mapOf("A" to "HD characters", "K" to "SL char creation")
        assertEquals("Needs HD characters", PatchRules.hint("U", names))
        assertEquals("Can't be on with SL char creation", PatchRules.hint("J", names))
        assertNull(PatchRules.hint("6", names))
    }
}
