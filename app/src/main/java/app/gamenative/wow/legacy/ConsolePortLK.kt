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
        val ours = existing?.id ?: ((manager.getProfiles(false).maxOfOrNull { it.id } ?: 0) + 1)
        writeIfOutdated(context, ours)
        val chosenOther = current != 0 && current != ours && manager.getProfile(current) != null
        if (!chosenOther) container.putExtra("profileId", ours.toString())
    }

    /**
     * Writes the shipped profile under [id] when it's missing or an older mapping was written. The version
     * lives in prefs because Edit controls rewrites the file without unknown keys.
     */
    private fun writeIfOutdated(context: Context, id: Int) {
        val shipped = JSONObject(context.assets.open(PROFILE_ASSET).bufferedReader().use { it.readText() })
        val version = shipped.optInt(MAPPING_VERSION)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val file = ControlsProfile.getProfileFile(context, id)
        if (file.isFile && prefs.getInt(MAPPING_VERSION, 0) >= version) return
        shipped.put("id", id)
        file.writeText(shipped.toString())
        prefs.edit().putInt(MAPPING_VERSION, version).apply()
    }

    private const val PREFS = "wow_consoleportlk"
    private const val MAPPING_VERSION = "mappingVersion"
}
