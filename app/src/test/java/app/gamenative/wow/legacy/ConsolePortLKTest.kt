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
    fun installedMeansTheMainFolderHasItsToc() {
        val addons = Files.createTempDirectory("addons").toFile()
        assertFalse(ConsolePortLK.isInstalled(addons))
        File(addons, "ConsolePort").mkdirs()
        assertFalse(ConsolePortLK.isInstalled(addons))
        File(addons, "ConsolePort/ConsolePort.toc").writeText("## Interface: 30300\n")
        assertTrue(ConsolePortLK.isInstalled(addons))
    }
}
