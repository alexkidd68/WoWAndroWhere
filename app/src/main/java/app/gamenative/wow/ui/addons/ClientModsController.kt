package app.gamenative.wow.ui.addons

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.gamenative.wow.legacy.mods.AndroWherePatch
import app.gamenative.wow.legacy.mods.ClientMods
import app.gamenative.wow.legacy.mods.ClientPatch
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** UI state for the 3.3.5a client add-ons: our login screen add-on and the client's own patch archives. */
class ClientModsController(val root: File, private val scope: CoroutineScope) {
    /** Our patches and whether each is installed; null until read. */
    var ours by mutableStateOf<Map<AndroWherePatch, Boolean>?>(null)
        private set
    var patches by mutableStateOf<List<ClientPatch>?>(null)
        private set
    var working by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun refresh() {
        scope.launch {
            val (installed, found) = withContext(Dispatchers.IO) {
                AndroWherePatch.entries.associateWith { runCatching { ClientMods.installed(root, it) }.getOrDefault(false) } to
                    runCatching { ClientMods.patches(root) }.onFailure { Timber.w(it, "Listing client patches failed") }.getOrDefault(emptyList())
            }
            ours = installed
            patches = found
        }
    }

    fun setInstalled(patch: AndroWherePatch, installed: Boolean) =
        act(if (installed) "Installing ${patch.title}" else "Removing ${patch.title}") {
            ClientMods.setInstalled(root, patch, installed)
        }

    fun setPatch(patch: ClientPatch, enabled: Boolean) =
        act(if (enabled) "Turning on ${patch.name}" else "Turning off ${patch.name}") {
            ClientMods.setPatchEnabled(patch, enabled)
            ClientMods.refresh(root)
        }

    private fun act(label: String, block: () -> Unit) {
        if (working != null) return
        working = label
        message = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, label)
                message = "$label failed: ${e.message ?: "unknown error"}"
            } finally {
                working = null
                refresh()
            }
        }
    }
}
