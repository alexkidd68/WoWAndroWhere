package app.gamenative.wow.legacy.mods

import android.content.Context
import android.os.FileObserver
import app.gamenative.wow.legacy.LegacyClient
import app.gamenative.wow.legacy.LegacyServers
import app.gamenative.wow.legacy.LegacySettings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Log in with A: AndroWhere Login carries the saved server passwords into the client for one launch.
 * It sits in our archive in the client folder from launch until the client has opened the archive, and is
 * wiped then, at a backstop, when the game ends and whenever the launcher comes back. See NOTES.md.
 */
object FastLogin {
    private const val AFTER_OPEN_MS = 10_000L
    private const val MAX_LIFETIME_MS = 180_000L

    private var observer: FileObserver? = null

    /** Called at launch, after the client's config is written. */
    fun prepare(context: Context, root: File, settings: LegacySettings) {
        if (!ClientMods.loginEnabled(root)) return
        val servers = LegacyServers.load(context, settings)
        val passwords = if (settings.fastLogin) servers.map { LegacyServers.password(context, it.id) } else null
        val data = LoginData(
            servers = servers.map { GlueServer(it.name, it.realmlist, it.account) },
            selected = servers.indexOfFirst { it.id == settings.serverId }.coerceAtLeast(0),
            passwords = passwords,
            autoLogin = settings.autoLogin,
        )
        ClientMods.refresh(root, data)
        if (passwords?.any { it != null } == true) watch(root)
    }

    /** Takes the password out of the client folder. Safe to call any time; does nothing when it isn't there. */
    fun wipe(context: Context) {
        val root = LegacyClient.load(context).root.takeIf { it.isNotBlank() } ?: return
        CoroutineScope(Dispatchers.IO).launch { wipe(File(root)) }
    }

    private fun wipe(root: File) {
        runCatching { ClientMods.wipePassword(root) }.onFailure { Timber.w(it, "Couldn't wipe the login password") }
    }

    private fun watch(root: File) {
        val archive = ClientMods.archive(root)
        observer?.stopWatching()
        observer = object : FileObserver(archive.parentFile!!, ACCESS or OPEN) {
            override fun onEvent(event: Int, path: String?) {
                if (path != archive.name) return
                stopWatching()
                CoroutineScope(Dispatchers.IO).launch {
                    delay(AFTER_OPEN_MS)
                    wipe(root)
                }
            }
        }.also { runCatching { it.startWatching() }.onFailure { e -> Timber.w(e, "Can't watch the login archive; using the backstop only") } }
        CoroutineScope(Dispatchers.IO).launch {
            delay(MAX_LIFETIME_MS)
            observer?.stopWatching()
            wipe(root)
        }
    }
}
