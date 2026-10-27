package app.gamenative.wow.ui.addons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.gamenative.wow.legacy.mods.ClientPatch
import app.gamenative.wow.ui.components.FwChip
import app.gamenative.wow.ui.components.FwRow
import app.gamenative.wow.ui.theme.Fw

/** The Client tab on 3.3.5a: add-ons that change the client itself, each with an on/off switch. */
@Composable
internal fun ClientModsPanel(c: ClientModsController, modifier: Modifier = Modifier) {
    LaunchedEffect(c) { c.refresh() }
    LazyColumn(modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (c.working ?: c.message)?.let { status ->
            item { Text(status, style = Fw.Secondary.copy(color = if (c.working != null) Fw.Brass else Fw.Warn)) }
        }
        item { Text("From AndroWhere", style = Fw.Label.copy(color = Fw.Ash), modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) }
        item {
            Toggle(
                title = "AndroWhere Login",
                detail = "Controller support on the login, realm and character screens, with button hints. Uses the ConsolePortLK controller layout. " +
                    "Turning it on changes 5 bytes of Wow.exe so the client accepts it, and keeps the original to put back when you turn it off.",
                on = c.loginEnabled,
                onToggle = { c.setLogin(!c.loginEnabled) },
            )
        }
        item { Text("In your client", style = Fw.Label.copy(color = Fw.Ash), modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)) }
        val patches = c.patches
        when {
            patches == null -> item { Text("Reading the client's Data folder…", style = Fw.Secondary) }
            patches.isEmpty() -> item { Text("No extra patch files in this client.", style = Fw.Secondary) }
            else -> items(patches, key = { it.key }) { patch ->
                Toggle(patch.name, detail(patch), patch.enabled, { c.setPatch(patch, !patch.enabled) })
            }
        }
    }
}

@Composable
private fun Toggle(title: String, detail: String, on: Boolean, onToggle: () -> Unit) {
    FwRow(selected = false, onClick = onToggle) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Fw.Text)
            Text(detail, style = Fw.Secondary)
        }
        FwChip(if (on) "On" else "Off", on, onToggle)
    }
}

private fun detail(patch: ClientPatch) = "Patch ${patch.key}, ${size(patch.size)}"

private fun size(bytes: Long) = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.0f MB".format(bytes / (1L shl 20).toDouble())
    else -> "%.0f KB".format(bytes / 1024.0)
}
