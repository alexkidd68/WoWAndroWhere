package app.gamenative.wow.ui

import android.view.KeyEvent as AndroidKey
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.gamenative.utils.StorageUtils
import app.gamenative.wow.legacy.LegacyClient
import app.gamenative.wow.legacy.LegacyRenderer
import app.gamenative.wow.legacy.LegacyServer
import app.gamenative.wow.legacy.mods.AndroWherePatch
import app.gamenative.wow.ui.addons.ClientModsController
import app.gamenative.wow.ui.components.FwButton
import app.gamenative.wow.ui.components.FwButtonKind
import app.gamenative.wow.ui.components.FwChip
import app.gamenative.wow.ui.components.FwDialog
import app.gamenative.wow.ui.components.FwRow
import app.gamenative.wow.ui.components.FwTextField
import app.gamenative.wow.ui.components.FwTopBar
import app.gamenative.wow.ui.theme.Fw
import java.io.File

private enum class Pane(val title: String, val hint: String) {
    SERVER("Server", "Your servers. Play starts on the selected one."),
    LOGIN("Login", "How the game's login screen fills in your password."),
    GRAPHICS("Graphics", "Renderer, lighter graphics and the performance overlay."),
    CLIENT("Client", "Your 3.3.5a client folder and logs."),
}

/**
 * Private server "Client and server": the panes on the left, the chosen pane's options on the right.
 * The D-pad moves, right enters a pane and B goes back to the list. One line at the bottom explains
 * whatever is highlighted, so the options themselves fit without long text.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PrivateServerScreen(launcher: LauncherController, onBack: () -> Unit) {
    var pane by remember { mutableStateOf(if (launcher.legacy.root.isBlank()) Pane.CLIENT else Pane.SERVER) }
    var hint by remember { mutableStateOf(pane.hint) }
    var inPane by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var returnTo by remember { mutableStateOf<String?>(null) }
    val nav = remember { Pane.entries.associateWith { FocusRequester() } }
    LaunchedEffect(Unit) {
        launcher.detectLegacyBuild()
        runCatching { nav.getValue(pane).requestFocus() }
    }
    BackHandler(enabled = inPane && editing == null) { runCatching { nav.getValue(pane).requestFocus() } }

    Column(Modifier.fillMaxSize().background(Fw.Ground)) {
        FwTopBar("Client and server", onBack)
        HorizontalDivider(color = Fw.Rule)
        Row(Modifier.weight(1f)) {
            Column(
                Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .focusProperties { enter = { nav.getValue(pane) } }
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Pane.entries.forEach { p ->
                    FwRow(
                        selected = p == pane,
                        onClick = { pane = p },
                        modifier = Modifier
                            .focusRequester(nav.getValue(p))
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    if (pane != p) {
                                        pane = p
                                        editing = null
                                        returnTo = null
                                    }
                                    hint = p.hint
                                }
                            },
                    ) { Text(p.title, style = Fw.Text) }
                }
            }
            VerticalDivider(color = Fw.Rule)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onFocusChanged { inPane = it.hasFocus }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val setHint: (String) -> Unit = { hint = it }
                when (pane) {
                    Pane.SERVER -> ServerPane(
                        launcher,
                        editing,
                        returnTo,
                        onEdit = { id ->
                            if (id == null) returnTo = editing
                            editing = id
                        },
                        setHint,
                    )
                    Pane.LOGIN -> LoginPane(launcher, setHint)
                    Pane.GRAPHICS -> GraphicsPane(launcher, setHint)
                    Pane.CLIENT -> ClientPane(launcher, setHint)
                }
            }
        }
        HorizontalDivider(color = Fw.Rule)
        Text(
            launcher.errorMessage ?: hint,
            style = Fw.Secondary.copy(color = if (launcher.errorMessage != null) Fw.Warn else Fw.Ash),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

/** Shows [text] in the hint line while this item is highlighted. */
private fun Modifier.hint(text: String, setHint: (String) -> Unit) = onFocusChanged { if (it.hasFocus) setHint(text) }

@Composable
private fun ServerPane(
    launcher: LauncherController,
    editing: String?,
    returnTo: String?,
    onEdit: (String?) -> Unit,
    setHint: (String) -> Unit,
) {
    val server = editing?.let { id -> launcher.servers.firstOrNull { it.id == id } }
    if (server != null) {
        BackHandler { onEdit(null) }
        ServerPage(launcher, server, onDone = { onEdit(null) }, setHint)
    } else {
        ServerList(launcher, returnTo, onEdit, setHint)
    }
}

@Composable
private fun ServerList(launcher: LauncherController, returnTo: String?, onEdit: (String?) -> Unit, setHint: (String) -> Unit) {
    var removing by remember { mutableStateOf<LegacyServer?>(null) }
    // Where focus lands after closing a server or the remove dialog: that server, or the first one.
    var focusAfter by remember { mutableStateOf(returnTo) }
    var tick by remember { mutableStateOf(0) }
    val refocus = remember { FocusRequester() }
    val target = focusAfter?.takeIf { id -> launcher.servers.any { it.id == id } } ?: launcher.servers.first().id
    LaunchedEffect(focusAfter, tick) { if (focusAfter != null) runCatching { refocus.requestFocus() } }
    val canRemove = launcher.servers.size > 1
    val rowHint = if (canRemove) "A  Select and edit      X  Add server      Y  Remove" else "A  Select and edit      X  Add server"
    val add = {
        launcher.addServer()
        onEdit(launcher.legacy.serverId)
    }
    launcher.servers.forEach { s ->
        FwRow(
            selected = false,
            onClick = {
                launcher.selectServer(s.id)
                onEdit(s.id)
            },
            modifier = Modifier
                .then(if (s.id == target) Modifier.focusRequester(refocus) else Modifier)
                .hint(rowHint, setHint)
                .onPreviewKeyEvent { e ->
                    when {
                        e.pressed(AndroidKey.KEYCODE_BUTTON_X) -> {
                            add()
                            true
                        }
                        e.pressed(AndroidKey.KEYCODE_BUTTON_Y) && canRemove -> {
                            removing = s
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = Fw.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.realmlist.ifBlank { "No realmlist yet" }, style = Fw.Data, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (s.id == launcher.legacy.serverId) Text("Selected", style = Fw.Data.copy(color = Fw.Brass))
        }
    }
    FwRow(selected = false, onClick = add, modifier = Modifier.hint("Adds a server and opens it.", setHint)) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Fw.Brass, modifier = Modifier.size(24.dp))
        Text("Add server", style = Fw.Text)
    }
    removing?.let { s ->
        FwDialog(
            title = "Remove ${s.name}?",
            message = "Its realmlist, account name and saved password are removed from this profile.",
            confirm = "Remove",
            onConfirm = {
                launcher.removeServer(s.id)
                removing = null
                focusAfter = launcher.servers.first().id
                tick++
            },
            onDismiss = {
                removing = null
                focusAfter = s.id
                tick++
            },
        )
    }
}

/** One server's details. It is the selected server while it's open, so the password shown is its own. */
@Composable
private fun ServerPage(launcher: LauncherController, server: LegacyServer, onDone: () -> Unit, setHint: (String) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(server.id) { runCatching { first.requestFocus() } }
    key(server.id) {
        var name by remember { mutableStateOf(server.name) }
        var realmlist by remember { mutableStateOf(server.realmlist) }
        var account by remember { mutableStateOf(server.account) }
        val save = { launcher.updateServer(server.copy(name = name.ifBlank { "Server" }, realmlist = realmlist.trim(), account = account.trim())) }
        Text(server.name, style = Fw.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        FwTextField(
            name,
            { name = it; save() },
            "Name",
            Modifier.fillMaxWidth().hint("A name for the server list and the login screen.", setHint),
            focusRequester = first,
        )
        FwTextField(
            realmlist,
            { realmlist = it; save() },
            "Realmlist, e.g. logon.example.com",
            Modifier.fillMaxWidth().hint("The server's logon address, from its website.", setHint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        FwTextField(
            account,
            { account = it; save() },
            "Account name (optional)",
            Modifier.fillMaxWidth().hint("Fills the login screen's account box. Leave it empty to use the one the game remembers.", setHint),
        )
        PasswordFields(launcher, server, setHint)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FwButton("Done", onDone, modifier = Modifier.hint("Back to the server list. Changes are saved as you type.", setHint))
            if (launcher.servers.size > 1) {
                FwButton(
                    "Remove server",
                    {
                        launcher.removeServer(server.id)
                        onDone()
                    },
                    modifier = Modifier.hint("Removes this server and its saved password.", setHint),
                    kind = FwButtonKind.Danger,
                )
            }
        }
    }
}

@Composable
private fun PasswordFields(launcher: LauncherController, server: LegacyServer, setHint: (String) -> Unit) {
    var password by remember(server.id) { mutableStateOf("") }
    var justSaved by remember(server.id) { mutableStateOf(false) }
    val saved = launcher.hasServerPassword
    if (saved) Text(if (justSaved) "Password saved  ••••••••" else "Saved password  ••••••••", style = Fw.Text.copy(color = Fw.Ok))
    FwTextField(
        password,
        { password = it; justSaved = false },
        if (saved) "Change password" else "Password",
        Modifier.fillMaxWidth().hint("Saved on this device, encrypted with the Android keystore. Login uses it.", setHint),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FwButton(
            "Save password",
            {
                launcher.saveServerPassword(password)
                password = ""
                justSaved = true
            },
            kind = FwButtonKind.Ghost,
            enabled = password.isNotEmpty(),
        )
        if (saved) FwButton("Forget password", launcher::forgetServerPassword, kind = FwButtonKind.Danger)
    }
}

@Composable
private fun LoginPane(launcher: LauncherController, setHint: (String) -> Unit) {
    val settings = launcher.legacy
    if (settings.root.isBlank()) {
        Text("Choose your client folder under Client first.", style = Fw.Text)
        return
    }
    val root = File(settings.root)
    val scope = rememberCoroutineScope()
    val mods = remember(root) { ClientModsController(root, scope, launcher::recordClientMods) }
    LaunchedEffect(root) { mods.refresh() }
    when (mods.ours?.get(AndroWherePatch.LOGIN)) {
        null -> Text("Checking the client…", style = Fw.Secondary)
        false -> {
            Text(
                mods.working ?: mods.message
                    ?: "AndroWhere Login lets the controller work on the login, realm and character screens, and fills in your password.",
                style = Fw.Text,
            )
            FwButton(
                "Install AndroWhere Login",
                { mods.setInstalled(AndroWherePatch.LOGIN, true) },
                modifier = Modifier.hint("Needs ConsolePortLK. Changes 5 bytes of Wow.exe; the original is kept and put back if you remove it.", setHint),
                enabled = mods.working == null,
            )
        }
        true -> {
            if (!launcher.hasServerPassword) {
                Text("No password saved for ${launcher.selectedServer.name}. Save one under Server.", style = Fw.Secondary.copy(color = Fw.Warn))
            }
            ToggleRow(
                "Fill in password",
                settings.fastLogin,
                hint = "The app puts the password in the client folder at launch and removes it about 10 seconds after the game reads it, or after 3 minutes at most.",
                setHint = setHint,
            ) { launcher.updateLegacy(settings.copy(fastLogin = !settings.fastLogin)) }
            ToggleRow(
                "Log in automatically",
                settings.fastLogin && settings.autoLogin,
                enabled = settings.fastLogin,
                hint = if (settings.fastLogin) "Presses Log in once each time the game starts. If it fails, press A to try again." else "Needs Fill in password.",
                setHint = setHint,
            ) { launcher.updateLegacy(settings.copy(autoLogin = !settings.autoLogin)) }
        }
    }
}

@Composable
private fun GraphicsPane(launcher: LauncherController, setHint: (String) -> Unit) {
    val settings = launcher.legacy
    Text("Renderer", style = Fw.Data)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LegacyRenderer.entries.forEach { r ->
            FwChip(
                r.label,
                settings.renderer == r,
                { launcher.updateLegacy(settings.copy(renderer = r)) },
                Modifier.hint("Use DXVK. OpenGL is much slower and only a fallback for when DXVK doesn't start.", setHint),
            )
        }
    }
    FwButton(
        "Use lighter graphics",
        launcher::applyHandheldGraphics,
        modifier = Modifier.hint("Lowers view distance, ground clutter, particles, weather, shadows and glow, which eases the load on the processor.", setHint),
        kind = FwButtonKind.Ghost,
        enabled = settings.root.isNotBlank(),
    )
    if (launcher.legacyGraphicsApplied) {
        Text("Lighter graphics set. They apply the next time the game starts.", style = Fw.Secondary.copy(color = Fw.Ok))
    }
    ToggleRow(
        "Performance overlay",
        settings.perfOverlay,
        hint = "Frame rate, frame times and GPU load in the game, DXVK only. Low GPU load with a low frame rate means the processor is the limit.",
        setHint = setHint,
    ) { launcher.updateLegacy(settings.copy(perfOverlay = !settings.perfOverlay)) }
}

@Composable
private fun ClientPane(launcher: LauncherController, setHint: (String) -> Unit) {
    val context = LocalContext.current
    val settings = launcher.legacy
    var folderError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val path = StorageUtils.getPathFromTreeUri(context, uri) ?: return@rememberLauncherForActivityResult
        val root = LegacyClient.findRoot(File(path))
        folderError = if (root == null) "No ${LegacyClient.EXE} with a Data folder in $path." else null
        root?.let {
            val existing = LegacyClient.readRealmlist(it)
            launcher.updateLegacy(settings.copy(root = it.absolutePath, realmlist = existing ?: settings.realmlist))
        }
    }
    if (settings.root.isBlank()) {
        Text("No folder chosen.", style = Fw.Text)
    } else {
        Text(settings.root, style = Fw.Data)
        Text(launcher.legacyBuild?.let { "Client build $it" } ?: "Client build unknown", style = Fw.Data)
        SharedFolderNote(launcher)
    }
    folderError?.let { Text(it, style = Fw.Secondary.copy(color = Fw.Warn)) }
    FwButton(
        "Choose folder",
        { picker.launch(null) },
        modifier = Modifier.hint(
            "The folder with ${LegacyClient.EXE} and Data. The app only changes its realmlist, its settings and the client add-ons you turn on.",
            setHint,
        ),
        kind = if (settings.root.isBlank()) FwButtonKind.Primary else FwButtonKind.Ghost,
    )
    Text("Client patches are in Addons, on the Client tab.", style = Fw.Secondary)
    ToggleRow(
        "Save a log",
        settings.saveLog,
        hint = "Slows the game down, so use it only to track down a problem. Saves ${LegacyClient.LOG} in the client folder and offers to share it when the game closes.",
        setHint = setHint,
    ) { launcher.updateLegacy(settings.copy(saveLog = !settings.saveLog)) }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, enabled: Boolean = true, hint: String, setHint: (String) -> Unit, onToggle: () -> Unit) {
    FwRow(selected = false, onClick = { if (enabled) onToggle() }, modifier = Modifier.hint(hint, setHint)) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = Fw.Brass, uncheckedColor = Fw.Ash, checkmarkColor = Fw.BrassInk),
        )
        Text(title, style = if (enabled) Fw.Text else Fw.Text.copy(color = Fw.Ash), modifier = Modifier.weight(1f))
    }
}
