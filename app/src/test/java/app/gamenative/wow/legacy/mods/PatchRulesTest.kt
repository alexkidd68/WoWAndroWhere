package app.gamenative.wow.legacy.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatchRulesTest {

    private val rules = PatchRules.builtIn
    private val allOff = listOf("A", "U", "M", "N", "J", "K", "6").associateWith { false }

    @Test
    fun turningOnAPatchTurnsOnWhatItNeeds() {
        assertEquals(mapOf("U" to true, "A" to true), PatchRules.plan(rules, allOff, "U", true))
        assertEquals(mapOf("N" to true, "M" to true), PatchRules.plan(rules, allOff, "N", true))
    }

    @Test
    fun turningOffAPatchTurnsOffWhatNeedsIt() {
        val on = allOff + mapOf("A" to true, "U" to true)
        assertEquals(mapOf("A" to false, "U" to false), PatchRules.plan(rules, on, "A", false))
        assertEquals(mapOf("U" to false), PatchRules.plan(rules, on, "U", false))
    }

    @Test
    fun characterCreationScreensAreOneAtATime() {
        val j = allOff + ("J" to true)
        assertEquals(mapOf("K" to true, "J" to false), PatchRules.plan(rules, j, "K", true))
        assertEquals(mapOf("J" to false), PatchRules.plan(rules, j, "J", false))
    }

    @Test
    fun missingPatchesAndUnrelatedOnesStayAlone() {
        assertEquals(mapOf("U" to true), PatchRules.plan(rules, mapOf("U" to false), "U", true))
        assertEquals(mapOf("6" to true), PatchRules.plan(rules, allOff, "6", true))
        assertEquals(emptyMap<String, Boolean>(), PatchRules.plan(rules, allOff, "6", false))
    }

    @Test
    fun hintsNameTheOtherPatch() {
        val names = mapOf("A" to "HD characters", "K" to "SL character creation")
        assertEquals("Needs HD characters", PatchRules.hint(rules, "U", names))
        assertEquals("Can't be on with SL character creation", PatchRules.hint(rules, "J", names))
        assertNull(PatchRules.hint(rules, "6", names))
    }

    @Test
    fun rulesComeFromThePackMenuConfig() {
        fun entry(key: String, conditions: String) =
            """"$key": {
                "name": "(9)Patch $key",
                "conditions": [
                    $conditions
                ],
                "actions": {
                    "Turned on": [1, 1],
                    "Turned off": [0, 0]
                }
            }"""
        val config = """{"patches": {"static": {"default": ["a11", "u00"]}, "dynamic": {
            ${entry("a", "[]")},
            ${entry("u", "[\"a11\", \"HD characters required\"]")},
            ${entry("j", "[\"k00\", \"Locked. Disable: SL character creation\"]")},
            ${entry("k", "[\"j00\", \"Locked. Disable: MoP character creation\"]")},
            ${entry("n", "[\"m01\", \"Locked. Must enable: Dungeon Maps\"]")}
        }}}"""
        assertEquals(PatchRules.builtIn, PatchRules.parse(config))
        assertNull(PatchRules.parse("""{"patches": {"dynamic": {${entry("a", "[]")}}}}"""))
    }
}
