package app.gamenative.wow.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import app.gamenative.wow.legacy.LegacyRenderer
import app.gamenative.wow.ui.components.FwButton
import app.gamenative.wow.ui.components.FwButtonKind
import app.gamenative.wow.ui.components.FwChip
import app.gamenative.wow.ui.theme.Fw
import java.io.File

/** Private server "Game files": the player's 3.3.5a client folder, its realmlist and how it runs. */
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
        root?.let {
            val existing = LegacyClient.readRealmlist(it)
            if (existing != null) realmlist = existing
            launcher.updateLegacy(settings.copy(root = it.absolutePath, realmlist = existing ?: settings.realmlist))
        }
    }

    FwPage("Client and server", onBack) {
        FwSection("Client") {
            Text(
                "Point the app at your own 3.3.5a client folder, the one with ${LegacyClient.EXE} and Data. The app never downloads or changes the client beyond its realmlist and settings.",
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

        FwSection("Logs") {
            launcher.errorMessage?.let { Text(it, style = Fw.Secondary.copy(color = Fw.Warn)) }
            Text("Each launch saves a Wine log as ${LegacyClient.LOG} in the client folder and replaces the last one. When the game closes, the app asks whether to share it.", style = Fw.Secondary)
        }
    }
}

/** Private server "Account": the saved password that the in-game menu's Type password enters. */
@Composable
fun ServerLoginScreen(launcher: LauncherController, onBack: () -> Unit) {
    var password by remember { mutableStateOf("") }
    FwPage("Server login", onBack) {
        FwSection("Password") {
            Text(
                if (launcher.hasServerPassword) {
                    "Your server password is saved on this device, encrypted with the Android keystore. On the login screen, open the menu with Back and choose Type password."
                } else {
                    "Save your server password to enter it from the in-game menu instead of typing it each time. The client remembers your account name itself."
                },
                style = Fw.Text,
            )
            OutlinedTextField(
                password,
                { password = it },
                singleLine = true,
                label = { Text(if (launcher.hasServerPassword) "New password" else "Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = Fw.Text,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FwButton(
                    "Save password",
                    {
                        launcher.saveServerPassword(password)
                        password = ""
                    },
                    enabled = password.isNotEmpty(),
                )
                if (launcher.hasServerPassword) FwButton("Forget password", launcher::forgetServerPassword, kind = FwButtonKind.Danger)
            }
            launcher.errorMessage?.let { Text(it, style = Fw.Secondary.copy(color = Fw.Warn)) }
        }
        FwSection("How it works") {
            Text(
                "The password never goes into the game folder. Type password presses its keys and Enter in the game, so use it only when the password box is selected. Security codes your server emails you are still typed by hand.",
                style = Fw.Secondary,
            )
        }
    }
}
