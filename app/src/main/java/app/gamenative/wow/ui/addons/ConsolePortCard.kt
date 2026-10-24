package app.gamenative.wow.ui.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.gamenative.wow.legacy.ConsolePortLK
import app.gamenative.wow.product.GameVersion
import app.gamenative.wow.ui.components.FwButton
import app.gamenative.wow.ui.theme.Fw

/** On the 3.3.5a client, which has no controller support of its own: offers ConsolePortLK until it's installed. */
@Composable
internal fun ConsolePortCard(c: AddonsController) {
    if (c.version != GameVersion.PRIVATE_335 || c.installed == null || ConsolePortLK.isInstalled(c.addonsDir)) return
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
            Text("Controller support", style = Fw.Text)
            Text(
                "3.3.5a can't use a controller on its own. ConsolePortLK adds it. After installing, start the game and press each button once when the addon asks.",
                style = Fw.Secondary,
            )
        }
        FwButton("Install ConsolePortLK", { c.installFromUrl(ConsolePortLK.REPO_URL) }, enabled = c.working == null)
    }
}
