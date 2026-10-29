package app.gamenative.wow.legacy

import android.content.Context
import app.gamenative.externaldisplay.PadGame
import app.gamenative.externaldisplay.PadSettings
import com.winlator.container.Container
import java.io.File

/**
 * Turns on the second-screen pad (buttons, trackpad and keyboard on the AYN Thor's bottom screen) for
 * the private server container. The pad comes from wyattabuntjer/AYN-Thor-WoW-Launcher (GPL-3.0).
 * It only shows when the device has a second display, so single-screen devices are unaffected.
 */
object LegacyPad {
    fun prepare(context: Context, container: Container, clientRoot: File) {
        PadGame.name = "legacy"
        PadGame.label = "private servers"
        PadGame.backupDir = clientRoot.path
        PadSettings.init(context)
        PadSettings.launchProfile(PadGame.name).takeIf { it > 0 }?.let { PadSettings.loadProfile(it) }
        container.setExternalDisplayMode(Container.EXTERNAL_DISPLAY_MODE_BUTTONS)
    }
}
