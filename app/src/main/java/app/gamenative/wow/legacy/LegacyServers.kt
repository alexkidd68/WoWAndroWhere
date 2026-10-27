package app.gamenative.wow.legacy

import android.content.Context
import app.gamenative.wow.LoginVault
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A saved private server. Its password is sealed in its own [LoginVault], never in this list. */
@Serializable
data class LegacyServer(val id: String, val name: String, val realmlist: String, val account: String = "")

/**
 * The private servers saved in the app. [LegacySettings.realmlist] stays the selected server's realmlist,
 * so the launch path is unchanged; this list adds the others for the login screen's server switch.
 */
object LegacyServers {
    /** The first server, whose password lives in the vault the app used before servers could be saved. */
    const val MAIN = "main"
    private const val PREFS = "wow_legacy"
    private const val KEY = "servers"
    private const val VAULT = "legacy_server_vault"

    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context, settings: LegacySettings): List<LegacyServer> {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.let { runCatching { json.decodeFromString<List<LegacyServer>>(it) }.getOrNull() }
        return withSelected(stored.orEmpty(), settings)
    }

    fun save(context: Context, servers: List<LegacyServer>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.encodeToString(servers)).apply()
    }

    /** Keeps the selected server in step with the realmlist in [settings], and makes sure there is one. */
    internal fun withSelected(servers: List<LegacyServer>, settings: LegacySettings): List<LegacyServer> {
        val list = servers.ifEmpty { listOf(LegacyServer(MAIN, "Server", settings.realmlist)) }
        return if (list.none { it.id == settings.serverId }) {
            list
        } else {
            list.map { if (it.id == settings.serverId) it.copy(realmlist = settings.realmlist) else it }
        }
    }

    fun newServer(count: Int) = LegacyServer(UUID.randomUUID().toString().take(8), "Server ${count + 1}", "")

    fun vault(context: Context, id: String) = LoginVault(context, if (id == MAIN) VAULT else "${VAULT}_$id")

    fun password(context: Context, id: String): String? =
        runCatching { vault(context, id).load()?.second?.takeIf { it.isNotEmpty() } }.getOrNull()
}
