package app.gamenative.wow.legacy.mods

import java.io.File

/**
 * The WotLK HD pack's patch letters: what each changes, which need another, and which can't be on together.
 * Its own menu enforces these; they only apply to a client with that menu's config, since other packs reuse the letters.
 */
object PatchRules {
    private const val PACK_CONFIG = "patchmenu/.config/config_en.json"

    internal val descriptions = mapOf(
        "A" to "Higher detail player characters.",
        "U" to "Undead characters without visible bones.",
        "F" to "Higher detail creatures and NPCs.",
        "6" to "Higher resolution textures for the world.",
        "7" to "Higher detail armor. The pack marks it as risky.",
        "8" to "Higher detail weapons.",
        "J" to "Character creation screen in the Mists of Pandaria style.",
        "K" to "Character creation screen in the Shadowlands style.",
        "9" to "Restyled interface windows.",
        "D" to "New cursor and interface art.",
        "S" to "New spell effects.",
        "I" to "Higher resolution icons.",
        "T" to "Higher detail trees.",
        "W" to "New water.",
        "X" to "New skies.",
        "M" to "Dungeon maps.",
        "N" to "Dungeon minimaps.",
        "5" to "Later-expansion interface extras, like raid frames and a level up banner.",
        "Q" to "New loading screens.",
        "E" to "New music.",
        "B" to "Blood effects.",
        "C" to "The pack's login screens, with its server and account list.",
    )

    internal val needs = mapOf("U" to setOf("A"), "N" to setOf("M"))
    internal val conflicts = listOf(setOf("J", "K"))

    fun applies(root: File) = File(root, PACK_CONFIG).isFile

    fun description(key: String): String? = descriptions[key]

    /** One line on what a patch needs or rules out, for its row in the Client tab. */
    fun hint(key: String, names: Map<String, String>): String? {
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
    fun plan(current: Map<String, Boolean>, key: String, enabled: Boolean): Map<String, Boolean> {
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
