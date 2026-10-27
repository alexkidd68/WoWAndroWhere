package app.gamenative.wow.ui.addons

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    var loginEnabled by mutableStateOf(false)
        private set
    var patches by mutableStateOf<List<ClientPatch>?>(null)
        private set
    var working by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun refresh() {
        scope.launch {
            val (login, found) = withContext(Dispatchers.IO) {
                runCatching { ClientMods.loginEnabled(root) }.getOrDefault(false) to
                    runCatching { ClientMods.patches(root) }.onFailure { Timber.w(it, "Listing client patches failed") }.getOrDefault(emptyList())
            }
            loginEnabled = login
            patches = found
        }
    }

    fun setLogin(enabled: Boolean) =
        act(if (enabled) "Turning on AndroWhere Login" else "Turning off AndroWhere Login") {
            if (enabled) ClientMods.enableLogin(root) else ClientMods.disableLogin(root)
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
