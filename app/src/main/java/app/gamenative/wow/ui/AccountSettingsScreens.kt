package app.gamenative.wow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.gamenative.BuildConfig
import app.gamenative.utils.renderReleaseNotes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.ui.platform.LocalContext
import app.gamenative.wow.AppName
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.gamenative.wow.ui.components.FwButton
import app.gamenative.wow.ui.components.FwChip
import app.gamenative.wow.ui.components.Links
import app.gamenative.wow.ui.components.FwButtonKind
import app.gamenative.wow.ui.components.FwProgress
import app.gamenative.wow.ui.components.FwTopBar
import app.gamenative.wow.ui.theme.Fw

@Composable
internal fun FwPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Fw.Ground)) {
        FwTopBar(title, onBack)
        HorizontalDivider(color = Fw.Rule)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) { content() }
    }
}

@Composable
internal fun FwSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = Fw.Label.copy(color = Fw.Brass, fontSize = 12.sp, letterSpacing = 1.6.sp))
        content()
    }
}

@Composable
fun SettingsScreen(
    launcher: LauncherController,
    onBack: () -> Unit,
    onEditLogin: () -> Unit,
) {
    FwPage("Settings", onBack) {
        FwSection("App") {
            Text("${AppName.FULL} v${BuildConfig.VERSION_NAME}", style = Fw.Text)
            val update = launcher.appUpdate
            if (update == null) {
                Text("No app update found.", style = Fw.Secondary)
                FwButton("Check for app update", launcher::checkAppUpdate, kind = FwButtonKind.Ghost)
            } else {
                Text("Version ${update.version} is available.", style = Fw.Text.copy(color = Fw.Brass))
                if (update.notes.isNotBlank()) Text(renderReleaseNotes(update.notes), style = Fw.Secondary)
                if (launcher.isDownloadingAppUpdate) {
                    Text("Downloading ${(launcher.appUpdateProgress * 100).toInt()}%", style = Fw.Data)
                    FwProgress(launcher.appUpdateProgress)
                } else {
                    FwButton(if (launcher.downloadedApk?.exists() == true) "Install update" else "Update app", launcher::performAppUpdate)
                }
            }
        }
        BattleNetSection(launcher, onEditLogin)
        FwSection("Support WoW Foreverywhere") {
            val context = LocalContext.current
            Text("${AppName.FULL} is built on WoW Foreverywhere, a free and open source project. If it saves you some time, you can support its development on Ko-fi.", style = Fw.Text)
            FwButton("Support on Ko-fi", { Links.open(context, Links.KOFI) }, kind = FwButtonKind.Ghost, icon = Icons.Filled.LocalCafe)
        }
        FwSection("Addon sources") {
            ProviderLine("CurseForge", launcher.curseForge.enabled, offText = "Off: no key or proxy")
            ProviderLine("Wago Addons", BuildConfig.WAGO_API_KEY.isNotBlank())
            ProviderLine("WoWInterface", true)
            ProviderLine("GitHub (addon catalog and links)", true)
            Text("Wago needs an API key built into the app. Builds without one still use the other sources.", style = Fw.Secondary)
        }
        CurseForgeSection(launcher)
        FwSection("Device") {
            Text("${launcher.gpu.edition} profile · render size ${launcher.gpu.screenSize}", style = Fw.Data)
        }
        FwSection("About") {
            Text(
                "Unofficial community project, not affiliated with Blizzard Entertainment. The game is downloaded from Blizzard's own servers and needs your own Battle.net account. Based on jaredgei/wow-forever-android (GPL-3.0).",
                style = Fw.Secondary,
            )
            val context = LocalContext.current
            FwButton("Source code on GitHub", { Links.open(context, Links.REPO) }, kind = FwButtonKind.Ghost, icon = Icons.AutoMirrored.Filled.OpenInNew)
        }
    }
}

/** Settings, Battle.net login: one saved login for every Blizzard profile. */
@Composable
private fun BattleNetSection(launcher: LauncherController, onEditLogin: () -> Unit) {
    FwSection("Battle.net login") {
        Text(
            if (launcher.hasSavedLogin) {
                "Your email and password are saved on this device, encrypted with the Android keystore. Every WoW Forever and other Blizzard profile logs in with it. Authenticator codes are still typed in the game."
            } else {
                "No login saved. Save one to skip the login screen in every Blizzard profile, or sign in inside the game each time."
            },
            style = Fw.Text,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FwButton(if (launcher.hasSavedLogin) "Change login" else "Save login", onEditLogin)
            if (launcher.hasSavedLogin) FwButton("Forget login", launcher::forgetLogin, kind = FwButtonKind.Danger)
        }
        Text(
            "When you press Play the app hands your login to the game in a temporary file, then wipes it as soon as the game has read it, or after three minutes at most. Private servers use the password in their profile's Client and server instead.",
            style = Fw.Secondary,
        )
    }
}

@Composable
private fun ProviderLine(name: String, enabled: Boolean, offText: String = "Off: no API key") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = Fw.Text)
        Text(if (enabled) "On" else offText, style = Fw.Data.copy(color = if (enabled) Fw.Ok else Fw.Ash))
    }
}

/** Settings, CurseForge: the player's own API key or proxy. There is no shared one. */
@Composable
private fun CurseForgeSection(launcher: LauncherController) {
    val setup = launcher.curseForge
    var useProxy by remember { mutableStateOf(setup.usesProxy) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun pick(proxy: Boolean) {
        useProxy = proxy
        input = ""
        error = null
    }
    FwSection("CurseForge") {
        Text(
            when {
                setup.usesKey -> "On, with your API key. It's saved on this device, encrypted with the Android keystore, and only sent to CurseForge."
                setup.usesProxy -> "On, through your proxy at ${setup.proxyUrl}. The app sends no key to it."
                else -> "Off. CurseForge needs your own API key or a proxy you run. Wago, WoWInterface and GitHub work without either."
            },
            style = Fw.Text,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FwChip("Your API key", !useProxy, { pick(false) })
            FwChip("Your proxy", useProxy, { pick(true) })
        }
        OutlinedTextField(
            input,
            {
                input = it
                error = null
            },
            singleLine = true,
            label = {
                Text(
                    when {
                        useProxy -> "Proxy address, e.g. https://cf.example.com"
                        setup.usesKey -> "New API key"
                        else -> "API key"
                    },
                )
            },
            visualTransformation = if (useProxy) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = if (useProxy) KeyboardType.Uri else KeyboardType.Password),
            textStyle = Fw.Text,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FwButton(
                if (useProxy) "Save proxy" else "Save key",
                {
                    val saved = if (useProxy) launcher.saveCurseForgeProxy(input) else launcher.saveCurseForgeKey(input)
                    error = when {
                        saved -> null
                        useProxy -> "Enter an https address, like https://cf.example.com."
                        else -> "Couldn't save the key on this device."
                    }
                    if (saved) input = ""
                },
                enabled = input.isNotBlank(),
            )
            if (setup.enabled) {
                FwButton(
                    if (setup.usesKey) "Forget key" else "Forget proxy",
                    {
                        launcher.forgetCurseForge()
                        input = ""
                        error = null
                    },
                    kind = FwButtonKind.Danger,
                )
            }
        }
        error?.let { Text(it, style = Fw.Secondary.copy(color = Fw.Warn)) }
        Text(
            if (useProxy) {
                "A proxy keeps the key on a server you run, so it's never on the device. The source code includes one in server/curseforge-proxy. Searches then go through that server."
            } else {
                "Apply for a free API key at console.curseforge.com. CurseForge's terms say a key is for its owner only, so don't share it. Saving a key replaces a saved proxy."
            },
            style = Fw.Secondary,
        )
    }
}
