package app.gamenative.wow.addons

import android.content.Context
import app.gamenative.wow.LoginVault
import app.gamenative.wow.addons.provider.CurseForgeProvider
import java.net.URI

/**
 * How this device reaches CurseForge: the player's own API key, sent only to [OFFICIAL_API], or the
 * player's own proxy that adds a key itself. With neither, CurseForge is off. There is no shared default.
 */
class CurseForgeSetup(val apiKey: String = "", val proxyUrl: String = "") {
    val usesKey get() = apiKey.isNotBlank()
    val usesProxy get() = !usesKey && proxyUrl.isNotBlank()
    val enabled get() = usesKey || usesProxy

    fun provider(target: AddonTarget): CurseForgeProvider = when {
        usesKey -> CurseForgeProvider(apiKey, apiBase = CurseForgeAccess.OFFICIAL_API, target = target)
        else -> CurseForgeProvider("", apiBase = proxyUrl.ifBlank { CurseForgeAccess.OFFICIAL_API }, proxy = usesProxy, target = target)
    }

    override fun equals(other: Any?) = other is CurseForgeSetup && other.apiKey == apiKey && other.proxyUrl == proxyUrl
    override fun hashCode() = 31 * apiKey.hashCode() + proxyUrl.hashCode()
    override fun toString() = "CurseForgeSetup(key=${if (usesKey) "set" else "none"}, proxy=$proxyUrl)"
}

/**
 * Stores [CurseForgeSetup]. The key is sealed with [LoginVault] in its own prefs, like the server
 * password; the proxy address isn't secret and sits in plain prefs. Saving one clears the other.
 */
object CurseForgeAccess {
    const val OFFICIAL_API = "https://api.curseforge.com"
    private const val PREFS = "curseforge"
    private const val PROXY = "proxy"
    private const val VAULT = "curseforge_vault"

    fun load(context: Context): CurseForgeSetup {
        val key = runCatching { LoginVault(context, VAULT).load()?.second }.getOrNull().orEmpty()
        val proxy = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PROXY, null).orEmpty()
        return CurseForgeSetup(key, proxy)
    }

    fun saveKey(context: Context, key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return false
        if (runCatching { LoginVault(context, VAULT).save("", trimmed) }.isFailure) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(PROXY).commit()
        return true
    }

    /** Saves [url] once [normalizeProxy] accepts it; returns false otherwise. */
    fun saveProxy(context: Context, url: String): Boolean {
        val normalized = normalizeProxy(url) ?: return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(PROXY, normalized).commit()
        LoginVault(context, VAULT).clear()
        return true
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        LoginVault(context, VAULT).clear()
    }

    /** An https address with a host and nothing else (no login, query or fragment), without a trailing slash. */
    fun normalizeProxy(url: String): String? {
        val text = url.trim().let { if ("://" in it) it else "https://$it" }
        val uri = runCatching { URI(text) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) return null
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
        val port = if (uri.port == -1) "" else ":${uri.port}"
        return "https://${uri.host.lowercase()}$port${uri.rawPath.orEmpty().trimEnd('/')}"
    }
}
