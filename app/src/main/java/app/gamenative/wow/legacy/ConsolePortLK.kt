package app.gamenative.wow.legacy

import android.content.Context
import com.winlator.container.Container
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.inputcontrols.InputControlsManager
import java.io.File
import org.json.JSONObject

/**
 * ConsolePortLK, the 3.3.5a backport of ConsolePort. The 3.3.5a client can't read a gamepad, so the addon
 * expects controller buttons to arrive as keys: [applyControls] gives the legacy container a profile that
 * turns the controller into F1-F12, Shift and Ctrl (the addon's two modifiers), WASD and the mouse.
 * The addon then asks the player to press each button once to calibrate.
 */
object ConsolePortLK {
    const val REPO_URL = "https://github.com/leoaviana/ConsolePortLK"
    const val PROFILE_NAME = "ConsolePortLK"
    private const val PROFILE_ASSET = "wow/consoleportlk.icp"
    private const val MAIN_FOLDER = "ConsolePort"

    fun isInstalled(addonsDir: File) = File(addonsDir, "$MAIN_FOLDER/$MAIN_FOLDER.toc").isFile

    fun isInstalledIn(clientRoot: File) = isInstalled(File(clientRoot, "Interface/AddOns"))

    /**
     * Uses the ConsolePortLK profile while the addon is installed and the player hasn't picked another
     * profile for the container, and goes back to the default profile once the addon is removed.
     */
    fun applyControls(context: Context, container: Container, clientRoot: File) {
        val manager = InputControlsManager(context)
        val current = container.getExtra("profileId", "0").toIntOrNull() ?: 0
        val existing = manager.getProfiles(false).firstOrNull { it.name == PROFILE_NAME }
        if (!isInstalledIn(clientRoot)) {
            if (existing != null && current == existing.id) container.putExtra("profileId", null)
            return
        }
        val ours = existing?.id ?: install(context, manager)
        val chosenOther = current != 0 && current != ours && manager.getProfile(current) != null
        if (!chosenOther) container.putExtra("profileId", ours.toString())
    }

    private fun install(context: Context, manager: InputControlsManager): Int {
        val id = (manager.getProfiles(false).maxOfOrNull { it.id } ?: 0) + 1
        val data = JSONObject(context.assets.open(PROFILE_ASSET).bufferedReader().use { it.readText() })
        data.put("id", id)
        ControlsProfile.getProfileFile(context, id).writeText(data.toString())
        return id
    }
}
