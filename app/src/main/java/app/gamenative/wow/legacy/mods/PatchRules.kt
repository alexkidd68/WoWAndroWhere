package app.gamenative.wow.legacy.mods

import java.io.File

/** Which patches need another one on, and which can't be on together, by letter. */
data class PatchRuleSet(val needs: Map<String, Set<String>>, val conflicts: List<Set<String>>)

/**
 * The WotLK HD pack's patch letters: what each changes, which need another, and which can't be on together.
 * The rules come from its patch menu's config; they only apply to a client that has it, since other packs reuse the letters.
 */
object PatchRules {
    private const val PACK_CONFIG = "patchmenu/.config/config_en.json"

    /** A patch entry and its conditions list, e.g. `"u": { "name": ..., "conditions": [["a11", "..."]], "actions"`. */
    private val ENTRY = Regex("\"([0-9a-z])\"\\s*:\\s*\\{\\s*\"name\"[^{}]*?\"conditions\"\\s*:\\s*\\[(.*?)\\]\\s*,\\s*\"actions\"", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    /** A condition: another patch's letter and the state its data and locale archives must be in (1 on, 0 off). */
    private val CONDITION = Regex("\"([0-9a-z])([01])([01])\"", RegexOption.IGNORE_CASE)

    /** What the pack's config said on 2026-10-29, for a config this can't read. */
    internal val builtIn = PatchRuleSet(mapOf("U" to setOf("A"), "N" to setOf("M")), listOf(setOf("J", "K")))

    /** Checked against the pack's files on Daniel's PC on 2026-10-29. */
    internal val descriptions = mapOf(
        "A" to "Higher detail player characters, helmets and shoulders.",
        "U" to "Undead player characters without bare bones showing.",
        "F" to "Higher detail creatures, NPCs and world objects.",
        "6" to "Higher resolution textures for the world, buildings and dungeons.",
        "7" to "Higher detail armor, and herb and ore nodes. The pack marks it dangerous.",
        "8" to "Higher detail weapons, shields and quivers.",
        "J" to "Character creation in the Mists of Pandaria style.",
        "K" to "Character creation in the Shadowlands style.",
        "9" to "Later-expansion target frame, minimap, spellbook and character windows.",
        "D" to "New cursors and minimap tracking icons, and the ACP addon manager.",
        "S" to "New spell effects and spell sounds.",
        "I" to "Higher resolution icons.",
        "T" to "Trees and bushes from later expansions.",
        "W" to "New water, lava and slime.",
        "X" to "New stars and sky lighting.",
        "M" to "World map pages for dungeons and raids.",
        "N" to "Minimaps inside dungeons. The pack marks it dangerous.",
        "5" to "Addons: later-expansion raid frames, loss of control alerts and level up banner.",
        "Q" to "New loading screens.",
        "E" to "New spell sounds and some new zone and city music.",
        "B" to "Blood splashes in combat. The pack marks it dangerous.",
        "C" to "The pack's login screens, with its own server and account list.",
    )

    /** The pack's rules, or null when [root] isn't the HD pack. */
    fun read(root: File): PatchRuleSet? {
        val config = File(root, PACK_CONFIG).takeIf { it.isFile } ?: return null
        return runCatching { parse(config.readText()) }.getOrNull() ?: builtIn
    }

    /** A condition that wants the other patch on is a need; one that wants it off means the two can't be on together. */
    internal fun parse(config: String): PatchRuleSet? {
        val needs = mutableMapOf<String, Set<String>>()
        val conflicts = mutableListOf<Set<String>>()
        ENTRY.findAll(config).forEach { entry ->
            val key = entry.groupValues[1].uppercase()
            CONDITION.findAll(entry.groupValues[2]).forEach { condition ->
                val (other, data, locale) = condition.destructured
                if (data == "1" || locale == "1") {
                    needs[key] = needs[key].orEmpty() + other.uppercase()
                } else {
                    val pair = setOf(key, other.uppercase())
                    if (pair !in conflicts) conflicts += pair
                }
            }
        }
        return PatchRuleSet(needs, conflicts).takeIf { needs.isNotEmpty() || conflicts.isNotEmpty() }
    }

    fun description(key: String): String? = descriptions[key]

    /** One line on what a patch needs or rules out, for its row in the Client tab. */
    fun hint(rules: PatchRuleSet, key: String, names: Map<String, String>): String? {
        val (needs, conflicts) = rules
        fun name(k: String) = names[k] ?: "patch $k"
        val parts = listOfNotNull(
            needs[key]?.joinToString(" and ") { name(it) }?.let { "Needs $it" },
            conflicts.filter { key in it }.flatMap { it - key }.takeIf { it.isNotEmpty() }
                ?.joinToString(" or ") { name(it) }?.let { "Can't be on with $it" },
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString(". ")
    }

    /**
     * The switches to flip so [key] ends up [enabled] and every rule still holds: turning one on also turns on
     * what it needs and turns off what it can't be on with, turning one off also turns off what needs it.
     * Letters missing from [current] are left alone.
     */
    fun plan(rules: PatchRuleSet, current: Map<String, Boolean>, key: String, enabled: Boolean): Map<String, Boolean> {
        val (needs, conflicts) = rules
        val state = current.toMutableMap()
        fun set(k: String, on: Boolean) {
            if (k !in state || state[k] == on) return
            state[k] = on
            if (on) {
                needs[k].orEmpty().forEach { set(it, true) }
                conflicts.filter { k in it }.flatMap { it - k }.forEach { set(it, false) }
            } else {
                needs.filterValues { k in it }.keys.forEach { set(it, false) }
            }
        }
        set(key, enabled)
        return state.filter { (k, on) -> current[k] != on }
    }
}
