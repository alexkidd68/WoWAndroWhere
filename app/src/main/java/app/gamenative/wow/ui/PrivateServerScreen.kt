package app.gamenative.wow.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gamenative.utils.StorageUtils
import app.gamenative.wow.legacy.LegacyClient
import app.gamenative.wow.legacy.LegacyRenderer
import app.gamenative.wow.legacy.mods.AndroWherePatch
import app.gamenative.wow.ui.addons.ClientModsController
import app.gamenative.wow.ui.components.FwButton
import app.gamenative.wow.ui.components.FwButtonKind
import app.gamenative.wow.ui.components.FwChip
import app.gamenative.wow.ui.components.FwRow
import app.gamenative.wow.ui.theme.Fw
import java.io.File

/** Private server "Game files": the player's 3.3.5a client folder, its realmlist and how it runs. */
@Composable
fun PrivateServerScreen(launcher: LauncherController, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = launcher.legacy
    var folderError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { launcher.detectLegacyBuild() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val path = StorageUtils.getPathFromTreeUri(context, uri) ?: return@rememberLauncherForActivityResult
        val root = LegacyClient.findRoot(File(path))
        folderError = if (root == null) "No ${LegacyClient.EXE} with a Data folder in $path." else null
        root?.let {
            val existing = LegacyClient.readRealmlist(it)
            launcher.updateLegacy(settings.copy(root = it.absolutePath, realmlist = existing ?: settings.realmlist))
        }
    }

    FwPage("Client and server", onBack) {
        FwSection("Client") {
            Text(
                "Point the app at your own 3.3.5a client folder, the one with ${LegacyClient.EXE} and Data. The app never downloads or changes the client beyond its realmlist, its settings and the client add-ons you turn on.",
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

        FwSection("Servers") {
            Text(
                "Play starts on the selected server. With AndroWhere Login installed, L1 and R1 switch between these on the login screen.",
                style = Fw.Secondary,
            )
            launcher.servers.forEach { server ->
                FwRow(selected = server.id == settings.serverId, onClick = { launcher.selectServer(server.id) }) {
                    Column(Modifier.weight(1f)) {
                        Text(server.name, style = Fw.Text)
                        Text(server.realmlist.ifBlank { "No realmlist yet" }, style = Fw.Data)
                    }
                    if (server.id == settings.serverId) Text("Selected", style = Fw.Data.copy(color = Fw.Brass))
                }
            }
            val selected = launcher.selectedServer
            key(selected.id) {
                var name by remember { mutableStateOf(selected.name) }
                var realmlist by remember { mutableStateOf(selected.realmlist) }
                var account by remember { mutableStateOf(selected.account) }
                LaunchedEffect(settings.realmlist) { if (settings.realmlist != realmlist) realmlist = settings.realmlist }
                val save = { launcher.updateServer(selected.copy(name = name.ifBlank { "Server" }, realmlist = realmlist.trim(), account = account.trim())) }
                OutlinedTextField(name, { name = it; save() }, singleLine = true, label = { Text("Name") }, textStyle = Fw.Text, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    realmlist,
                    { realmlist = it; save() },
                    singleLine = true,
                    label = { Text("Realmlist, e.g. logon.example.com") },
                    textStyle = Fw.Text,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    account,
                    { account = it; save() },
                    singleLine = true,
                    label = { Text("Account name (optional)") },
                    textStyle = Fw.Text,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                "The account name fills the login screen's account box for this server. Leave it empty to use the one the game remembers. Each server's password is saved in Server login.",
                style = Fw.Secondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FwButton("Add server", launcher::addServer, kind = FwButtonKind.Ghost)
                if (launcher.servers.size > 1) {
                    FwButton("Remove ${selected.name}", { launcher.removeServer(selected.id) }, kind = FwButtonKind.Danger)
                }
            }
        }

        FwSection("Renderer") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegacyRenderer.entries.forEach { r ->
                    FwChip(r.label, settings.renderer == r, { launcher.updateLegacy(settings.copy(renderer = r)) })
                }
            }
            Text("Use DXVK. OpenGL is much slower and is only a fallback for when DXVK doesn't start.", style = Fw.Secondary)
        }

        FwSection("Performance") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FwChip("Overlay off", !settings.perfOverlay, { launcher.updateLegacy(settings.copy(perfOverlay = false)) })
                FwChip("Overlay on", settings.perfOverlay, { launcher.updateLegacy(settings.copy(perfOverlay = true)) })
            }
            Text(
                "Shows frame rate, frame times and GPU load in the game, with the DXVK renderer only. If GPU load stays well below 100% while the frame rate is low, the processor is the limit and lowering graphics settings that add objects, like view distance and ground clutter, helps most.",
                style = Fw.Secondary,
            )
            FwButton("Use lighter graphics", launcher::applyHandheldGraphics, kind = FwButtonKind.Ghost, enabled = settings.root.isNotBlank())
            Text(
                if (launcher.legacyGraphicsApplied) {
                    "Lighter graphics set. They take effect the next time the game starts, and you can change any of them in the game's video options."
                } else {
                    "Lowers view distance, ground clutter, particles, weather, shadows and full-screen glow in the client's settings, which eases the load on the processor."
                },
                style = Fw.Secondary,
            )
        }

        FwSection("Logs") {
            launcher.errorMessage?.let { Text(it, style = Fw.Secondary.copy(color = Fw.Warn)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FwChip("Don't save logs", !settings.saveLog, { launcher.updateLegacy(settings.copy(saveLog = false)) })
                FwChip("Save a log", settings.saveLog, { launcher.updateLegacy(settings.copy(saveLog = true)) })
            }
            Text(
                "Saving a log slows the game down, so turn it on only to track down a problem. Each launch then saves a Wine log as ${LegacyClient.LOG} in the client folder and replaces the last one, and when the game closes the app asks whether to share it.",
                style = Fw.Secondary,
            )
        }
    }
}

/** Private server "Account": the saved password that the in-game menu's Type password enters. */
@Composable
fun ServerLoginScreen(launcher: LauncherController, onBack: () -> Unit) {
    var password by remember { mutableStateOf("") }
    FwPage("Server login", onBack) {
        FwSection(if (launcher.servers.size > 1) "Password for ${launcher.selectedServer.name}" else "Password") {
            Text(
                if (launcher.hasServerPassword) {
                    "Your server password is saved on this device, encrypted with the Android keystore. " +
                        if (launcher.legacy.fastLogin) "On the login screen, press A." else "On the login screen, open the menu with Back and choose Type password."
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
            val settings = launcher.legacy
            CheckRow(
                "Fill in password",
                "Needs AndroWhere Login from Addons, Client. The login screen fills in this password, so A logs you in. " +
                    "To do that, the app puts the password in AndroWhere Login's file in the client folder when the game starts, and removes it about 10 seconds after the game opens that file, " +
                    "or after 3 minutes at most, when the game closes and when the app opens again. While it's there, any app that can read the client folder could read it.",
                settings.fastLogin,
            ) { launcher.updateLegacy(settings.copy(fastLogin = !settings.fastLogin)) }
            CheckRow(
                "Log in automatically",
                if (settings.fastLogin) {
                    "Presses Log in for you once each time the game starts. If that login fails, press A to try again."
                } else {
                    "Needs Fill in password."
                },
                settings.fastLogin && settings.autoLogin,
                enabled = settings.fastLogin,
                modifier = Modifier.padding(start = 24.dp),
            ) { launcher.updateLegacy(settings.copy(autoLogin = !settings.autoLogin)) }
            if (settings.fastLogin && settings.root.isNotBlank()) LoginAddOnStatus(File(settings.root))
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
                if (launcher.legacy.fastLogin) {
                    "Type password presses the password's keys and Enter in the game, so use it only when the password box is selected. Security codes your server emails you are still typed by hand."
                } else {
                    "The password never goes into the game folder. Type password presses its keys and Enter in the game, so use it only when the password box is selected. Security codes your server emails you are still typed by hand."
                },
                style = Fw.Secondary,
            )
        }
    }
}

@Composable
private fun CheckRow(title: String, detail: String, checked: Boolean, enabled: Boolean = true, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    FwRow(selected = false, onClick = { if (enabled) onToggle() }, modifier = modifier) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = Fw.Brass, uncheckedColor = Fw.Ash, checkmarkColor = Fw.BrassInk),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = if (enabled) Fw.Text else Fw.Text.copy(color = Fw.Ash))
            Text(detail, style = Fw.Secondary)
        }
    }
}

/** Fill in password does nothing without AndroWhere Login, so say so where it's ticked and offer to install it. */
@Composable
private fun LoginAddOnStatus(root: File) {
    val scope = rememberCoroutineScope()
    val mods = remember(root) { ClientModsController(root, scope) }
    LaunchedEffect(root) { mods.refresh() }
    if (mods.ours?.get(AndroWherePatch.LOGIN) != false) return
    Text(
        mods.working ?: mods.message ?: "AndroWhere Login isn't installed in this client, so the login screen can't fill in the password yet.",
        style = Fw.Secondary,
    )
    FwButton("Install AndroWhere Login", { mods.setInstalled(AndroWherePatch.LOGIN, true) }, enabled = mods.working == null)
}
