package app.gamenative.wow.ui.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.gamenative.wow.legacy.ConsolePortLK
import app.gamenative.wow.legacy.mods.AndroWherePatch
import app.gamenative.wow.ui.components.FwButton
import app.gamenative.wow.ui.components.FwButtonKind
import app.gamenative.wow.ui.theme.Fw

/** On the Installed list once ConsolePortLK is in: offers our patches that aren't installed yet, one press each. */
@Composable
internal fun AndroWherePatchesCard(c: AddonsController) {
    val mods = c.clientMods ?: return
    LaunchedEffect(mods) { mods.refresh() }
    if (c.mode != AddonsController.Mode.INSTALLED || !ConsolePortLK.isInstalled(c.addonsDir)) return
    val missing = mods.ours?.filterValues { !it }?.keys?.firstOrNull() ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .background(Fw.Stone, Fw.Shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("AndroWhere patch: ${missing.title}", style = Fw.Text)
            Text(
                when (missing) {
                    AndroWherePatch.LOGIN -> "Use the controller on the login, realm and character screens too. More AndroWhere patches are in the Client tab."
                },
                style = Fw.Secondary,
            )
        }
        FwButton("Install", { mods.setInstalled(missing, true) }, enabled = mods.working == null)
        FwButton("Client tab", { c.switchMode(AddonsController.Mode.CLIENT) }, kind = FwButtonKind.Ghost)
    }
}
