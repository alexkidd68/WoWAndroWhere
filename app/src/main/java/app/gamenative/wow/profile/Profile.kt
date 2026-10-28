package app.gamenative.wow.profile

import android.content.Context
import app.gamenative.ui.screen.wow.GamePath
import app.gamenative.wow.legacy.LegacyServer
import app.gamenative.wow.legacy.LegacyServers
import app.gamenative.wow.product.GameVersion
import app.gamenative.wow.product.GameVersions
import app.gamenative.wow.product.InstallRegistry
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One game setup the player switches between: a name, a game version and its folder. */
@Serializable
data class Profile(val id: String, val name: String, val version: String, val root: String = "") {
    val gameVersion get() = GameVersion.fromKey(version)

    /** Profiles with the same home share one AddOns folder on disk. Empty until a folder is chosen. */
    val home get() = if (root.isBlank()) "" else "$version|$root"
}

/**
 * The saved profiles and which one is active, kept in memory for the process. The active profile sets
 * [GameVersions.selected], the per-version game path and the 3.3.5a settings file, so code that reads
 * those keeps working unchanged.
 */
object Profiles {
    private const val PREFS = "wow_profiles"
    private const val KEY_LIST = "profiles"
    private const val KEY_ACTIVE = "active"

    /** The 3.3.5a profile made from the settings saved before profiles existed. It keeps their files. */
    const val LEGACY_MAIN = "private_335"
    private const val LEGACY_PREFS = "wow_legacy"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    var all: List<Profile> = emptyList()
        private set

    @Volatile
    var activeId: String? = null
        private set

    val active get() = all.firstOrNull { it.id == activeId }

    /** The 3.3.5a settings and server list of the active profile. */
    @Volatile
    var legacyPrefs: String = LEGACY_PREFS
        private set

    fun legacyPrefsFor(id: String) = if (id == LEGACY_MAIN) LEGACY_PREFS else "${LEGACY_PREFS}_$id"

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_LIST, null)?.let { runCatching { json.decodeFromString<List<Profile>>(it) }.getOrNull() }
        if (stored != null) {
            all = stored
            activeId = prefs.getString(KEY_ACTIVE, null)?.takeIf { id -> stored.any { it.id == id } } ?: stored.firstOrNull()?.id
        } else {
            val registry = InstallRegistry(context)
            val installs = GameVersion.entries.filterNot { it.isPrivateServer }.mapNotNull { v -> registry.activeFor(v)?.let { v to it.root } }.toMap()
            val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            val hasLegacy = !legacy.getString("root", null).isNullOrBlank() || !legacy.getString("realmlist", null).isNullOrBlank()
            val (list, active) = migrate(installs, legacy.getString("root", null).orEmpty(), hasLegacy, GameVersions.selected)
            all = list
            activeId = active
            save(context)
        }
        applyActive(context)
    }

    /**
     * Turns what an older version saved into profiles: one per game version with an install, named after
     * the version, plus the 3.3.5a setup when there is one. The selected version's profile becomes active.
     */
    internal fun migrate(
        installs: Map<GameVersion, String>,
        legacyRoot: String,
        hasLegacy: Boolean,
        selected: GameVersion,
    ): Pair<List<Profile>, String?> {
        val blizzard = GameVersion.entries.mapNotNull { v -> installs[v]?.let { Profile(v.key, v.displayName, v.key, it) } }
        val legacy = if (hasLegacy || selected.isPrivateServer) {
            listOf(Profile(LEGACY_MAIN, GameVersion.PRIVATE_335.displayName, GameVersion.PRIVATE_335.key, legacyRoot))
        } else {
            emptyList()
        }
        val list = blizzard + legacy
        return list to (list.firstOrNull { it.gameVersion == selected } ?: list.firstOrNull())?.id
    }

    private fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LIST, json.encodeToString(all))
            .putString(KEY_ACTIVE, activeId)
            .apply()
    }

    fun activate(context: Context, id: String) {
        if (all.none { it.id == id }) return
        activeId = id
        save(context)
        applyActive(context)
    }

    private fun applyActive(context: Context) {
        val profile = active ?: return
        GameVersions.save(context, profile.gameVersion)
        legacyPrefs = legacyPrefsFor(profile.id)
        if (!profile.gameVersion.isPrivateServer && profile.root.isNotBlank()) GamePath.save(context, profile.root)
    }

    /** A name not taken yet: "Classic Era", then "Classic Era 2" and so on. */
    fun freeName(base: String, except: String? = null): String {
        val taken = all.filter { it.id != except }.map { it.name.lowercase() }.toSet()
        return generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base $it" }.first { it.lowercase() !in taken }
    }

    /** A new profile; [replacing] is one about to be removed, so its name is free to take. */
    fun create(context: Context, name: String, version: GameVersion, replacing: String? = null): Profile {
        val profile = Profile(UUID.randomUUID().toString().take(8), freeName(name.trim().ifBlank { version.displayName }, except = replacing), version.key)
        if (version.isPrivateServer) {
            // Its own first server, so its password gets its own vault instead of the original one.
            val server = LegacyServer(UUID.randomUUID().toString().take(8), "Server", "")
            val prefs = context.getSharedPreferences(legacyPrefsFor(profile.id), Context.MODE_PRIVATE)
            prefs.edit().putString("server_id", server.id).apply()
            LegacyServers.save(context, listOf(server), legacyPrefsFor(profile.id))
        }
        all = all + profile
        save(context)
        return profile
    }

    fun update(context: Context, profile: Profile) {
        all = all.map { if (it.id == profile.id) profile else it }
        save(context)
        if (profile.id == activeId) applyActive(context)
    }

    /** Forgets [id] and its settings and saved server passwords. Its game files stay on storage. */
    fun remove(context: Context, id: String) {
        if (id == activeId) return
        val prefsName = legacyPrefsFor(id)
        LegacyServers.loadStored(context, prefsName).forEach { LegacyServers.vault(context, it.id).clear() }
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().apply()
        ProfileAddons.forget(context, id)
        all = all.filterNot { it.id == id }
        save(context)
    }

    /** The other profiles that use [profile]'s folder for the same game version. */
    fun sharers(profile: Profile): List<Profile> =
        if (profile.home.isEmpty()) emptyList() else all.filter { it.id != profile.id && it.home == profile.home }
}
