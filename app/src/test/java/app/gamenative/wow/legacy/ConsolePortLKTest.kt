package app.gamenative.wow.legacy

import android.view.MotionEvent
import com.winlator.inputcontrols.Binding
import com.winlator.inputcontrols.ExternalControllerBinding
import java.io.File
import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsolePortLKTest {

    private val profile = JSONObject(File("src/main/assets/wow/consoleportlk.icp").readText())
    private val bindings = profile.getJSONArray("controllers").getJSONObject(0).getJSONArray("controllerBindings").let { a ->
        (0 until a.length()).associate { a.getJSONObject(it).getInt("keyCode") to a.getJSONObject(it).getString("binding") }
    }

    @Test
    fun profileIsNamedAndCoversAnyController() {
        assertEquals(ConsolePortLK.PROFILE_NAME, profile.getString("name"))
        assertEquals("*", profile.getJSONArray("controllers").getJSONObject(0).getString("id"))
    }

    @Test
    fun everyBindingIsKnown() {
        bindings.values.forEach { Binding.valueOf(it) }
    }

    @Test
    fun triggersAreTheAddonsModifiersAndButtonsAreDistinctKeys() {
        assertEquals("KEY_SHIFT_L", bindings[104])
        assertEquals("KEY_CTRL_L", bindings[105])
        val keys = bindings.values.filter { it.startsWith("KEY_F") }
        assertEquals(12, keys.size)
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun leftStickForwardIsW() {
        // getKeyCodeForAxis reports Android's negative Y (stick pushed forward) as AXIS_Y_POSITIVE.
        assertEquals(ExternalControllerBinding.AXIS_Y_POSITIVE.toInt(), ExternalControllerBinding.getKeyCodeForAxis(MotionEvent.AXIS_Y, -1))
        assertEquals("KEY_W", bindings[ExternalControllerBinding.AXIS_Y_POSITIVE.toInt()])
        assertEquals("KEY_S", bindings[ExternalControllerBinding.AXIS_Y_NEGATIVE.toInt()])
        assertEquals("KEY_A", bindings[ExternalControllerBinding.AXIS_X_NEGATIVE.toInt()])
        assertEquals("KEY_D", bindings[ExternalControllerBinding.AXIS_X_POSITIVE.toInt()])
    }

    @Test
    fun setupAddonCalibrationMatchesTheProfile() {
        val lua = File("src/main/assets/wow/consoleportlk-setup/Setup.lua").readText()
        val keys = Regex("""(CP_[A-Z0-9_]+) = '(F\d+)'""").findAll(lua).associate { it.groupValues[1] to it.groupValues[2] }
        val buttons = mapOf(
            "CP_R_DOWN" to 96, "CP_R_RIGHT" to 97, "CP_R_LEFT" to 99, "CP_R_UP" to 100,
            "CP_L_UP" to 19, "CP_L_DOWN" to 20, "CP_L_LEFT" to 21, "CP_L_RIGHT" to 22,
            "CP_T1" to 102, "CP_T2" to 103, "CP_X_LEFT" to 109, "CP_X_RIGHT" to 108,
        )
        assertEquals(buttons.keys, keys.keys)
        buttons.forEach { (button, keyCode) -> assertEquals(button, "KEY_${keys[button]}", bindings[keyCode]) }
    }

    @Test
    fun installedMeansTheMainFolderHasItsToc() {
        val addons = Files.createTempDirectory("addons").toFile()
        assertFalse(ConsolePortLK.isInstalled(addons))
        File(addons, "ConsolePort").mkdirs()
        assertFalse(ConsolePortLK.isInstalled(addons))
        File(addons, "ConsolePort/ConsolePort.toc").writeText("## Interface: 30300\n")
        assertTrue(ConsolePortLK.isInstalled(addons))
    }

    @Test
    fun immersionControlsUseCalibratedButtons() {
        val lua = File("src/main/assets/wow/consoleportlk-setup/Setup.lua").readText()
        val calibrated = Regex("""(CP_[A-Z0-9_]+) = '(F\d+)'""").findAll(lua).map { it.groupValues[1] }.toSet()
        val actions = lua.substringAfter("local actions = {").substringBefore("\n\t}")
        val used = Regex("""(CP_[A-Z0-9_]+) =""").findAll(actions).map { it.groupValues[1] }.toSet()
        assertTrue(used.isNotEmpty())
        assertTrue("$used", calibrated.containsAll(used))
    }
}
