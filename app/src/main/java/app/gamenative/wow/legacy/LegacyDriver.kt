package app.gamenative.wow.legacy

import java.io.File

/**
 * Picks the Vulkan driver for the private server (3.3.5a) container.
 *
 * On Adreno 740 (AYN Thor) the test Turnip that WoW Forever uses aborts in DXVK's D3D9 path
 * (Mesa assertion `image_view->format == image->format` in vk_image_view_init), so the game closes
 * after a black screen. GameNative's Turnip v26.2.0 R4 runs the same client with DXVK on the same
 * device, so the 3.3.5a container uses it there. WoW Forever keeps its own driver.
 *
 * A one-line [OVERRIDE_FILE] next to Wow.exe overrides the choice for testing: the name of an
 * installed driver folder, or `System` for the phone's own Qualcomm driver.
 */
object LegacyDriver {
    const val ADRENO_740 = "Turnip-v26.2.0-R4"
    const val OVERRIDE_FILE = "androwhere-driver.txt"

    private val SAFE_NAME = Regex("[A-Za-z0-9._-]{1,64}")

    /** [current] is the WoW Forever driver for this GPU, [foreverAdreno740] the one it uses on Adreno 740. */
    fun pick(clientRoot: File, current: String, foreverAdreno740: String): String {
        override(clientRoot)?.let { return it }
        return if (current == foreverAdreno740) ADRENO_740 else current
    }

    private fun override(clientRoot: File): String? {
        val text = runCatching { File(clientRoot, OVERRIDE_FILE).takeIf { it.isFile }?.readText() }.getOrNull() ?: return null
        val name = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null
        return name.takeIf { SAFE_NAME.matches(it) }
    }
}
