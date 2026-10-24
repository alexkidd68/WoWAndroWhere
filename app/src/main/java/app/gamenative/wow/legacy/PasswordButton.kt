package app.gamenative.wow.legacy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.gamenative.R
import com.winlator.container.Container

/**
 * On-screen "Type password" button for the 3.3.5a login screen. It goes away once used, since
 * after login it would sit over the game; the quick menu item stays for later logins.
 */
@Composable
fun PasswordButton(container: Container?, hidden: Boolean, modifier: Modifier = Modifier) {
    if (container?.getExtra(LegacyClient.TYPE_PASSWORD_EXTRA) != "1") return
    val context = LocalContext.current
    var used by rememberSaveable { mutableStateOf(false) }
    if (used || hidden) return
    Box(modifier.padding(16.dp)) {
        OutlinedButton(
            onClick = {
                used = true
                LegacyClient.typeServerPassword(context)
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFC9A06A)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF10171A).copy(alpha = 0.85f),
                contentColor = Color(0xFFEFE6D6),
            ),
        ) {
            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.androwhere_type_password))
        }
    }
}
