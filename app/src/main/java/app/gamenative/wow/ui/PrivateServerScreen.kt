package app.gamenative.wow.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gamenative.utils.StorageUtils
import app.gamenative.wow.legacy.LegacyClient
import app.gamenative.wow.legacy.LegacyEmulator
import app.gamenative.wow.legacy.LegacyRenderer
import app.gamenative.wow.ui.components.FwButton
import app.gamenative.wow.ui.components.FwButtonKind
import app.gamenative.wow.ui.components.FwChip
import app.gamenative.wow.ui.theme.Fw
import java.io.File

/** Private-server spike: pick a 3.3.5a client folder and realmlist, choose how it runs, and launch. */
@Composable
fun PrivateServerScreen(launcher: LauncherController, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = launcher.legacy
    var realmlist by remember { mutableStateOf(settings.realmlist) }
    var folderError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { launcher.detectLegacyBuild() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val path = StorageUtils.getPathFromTreeUri(context, uri) ?: return@rememberLauncherForActivityResult
        val root = LegacyClient.findRoot(File(path))
        folderError = if (root == null) "No ${LegacyClient.EXE} with a Data folder in $path." else null
        root?.let { launcher.updateLegacy(settings.copy(root = it.absolutePath)) }
    }

    FwPage("Private server (test)", onBack) {
        FwSection("Client") {
            Text(
                "Point the app at your own 3.3.5a client folder, the one with ${LegacyClient.EXE} and Data. This is a test build to see whether old clients run on this device.",
                style = Fw.Secondary,
            )
            if (settings.root.isBlank()) {
                Text("No folder chosen.", style = Fw.Text)
            } else {
                Text(settings.root, style = Fw.Data)
                Text(launcher.legacyBuild?.let { "Client build $it" } ?: "Client build unknown", style = Fw.Data)
            }
            folderError?.let { Text(it, style = Fw.Secondary.copy(color = Fw.Warn)) }
            FwButton("Choose folder", { picker.launch(null) }, kind = FwButtonKind.Ghost)
        }

        FwSection("Server") {
            OutlinedTextField(
                realmlist,
                {
                    realmlist = it
                    launcher.updateLegacy(settings.copy(realmlist = it))
                },
                singleLine = true,
                label = { Text("Realmlist, e.g. logon.example.com") },
                textStyle = Fw.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FwSection("Renderer") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegacyRenderer.entries.forEach { r ->
                    FwChip(r.label, settings.renderer == r, { launcher.updateLegacy(settings.copy(renderer = r)) })
                }
            }
        }

        FwSection("x86 emulator") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegacyEmulator.entries.forEach { e ->
                    FwChip(e.label, settings.emulator == e, { launcher.updateLegacy(settings.copy(emulator = e)) })
                }
            }
        }

        FwSection("Play") {
            if (launcher.isLaunching) Text(launcher.statusText, style = Fw.Text)
            launcher.errorMessage?.let { Text(it, style = Fw.Secondary.copy(color = Fw.Warn)) }
            FwButton(
                "Launch",
                { launcher.launchLegacy() },
                enabled = !launcher.busy && settings.root.isNotBlank() && settings.realmlist.isNotBlank(),
            )
        }
    }
}
