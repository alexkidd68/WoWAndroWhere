package app.gamenative.wow.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.gamenative.wow.addons.AddonDb
import app.gamenative.wow.addons.AddonFlavor
import app.gamenative.wow.addons.InstalledAddon
import app.gamenative.wow.product.GameVersion
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [29])
@RunWith(RobolectricTestRunner::class)
class ProfileAddonsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var root: File
    private lateinit var addonsDir: File

    @Before
    fun setUp() {
        Profiles.load(context)
        root = Files.createTempDirectory("client").toFile()
        addonsDir = File(root, "Interface/AddOns").apply { mkdirs() }
    }

    private fun addon(id: String, vararg folders: String) =
        InstalledAddon(id, id, folders.toList(), "1", AddonFlavor.MAIN, null, 0)

    private fun profile(name: String) =
        Profiles.create(context, name, GameVersion.PRIVATE_335).copy(root = root.path).also { Profiles.update(context, it) }

    @Test
    fun aFolderOfItsOwnSeesEverything() {
        val solo = profile("Solo")
        assertTrue(ProfileAddons.visibility(context, solo)("github:anything"))
    }

    @Test
    fun aProfileJoiningAFolderStartsWithoutTheAddonsAlreadyThere() {
        AddonDb.forAddonsDir(addonsDir).save(listOf(addon("questie", "Questie"), addon("cp", "ConsolePort")))
        val first = profile("First")
        val second = profile("Second")
        ProfileAddons.joined(context, second, addonsDir, null)

        assertTrue(ProfileAddons.visibility(context, first)("questie"))
        assertFalse(ProfileAddons.visibility(context, second)("questie"))

        ProfileAddons.claim(context, second, setOf("dbm"))
        assertTrue(ProfileAddons.visibility(context, second)("dbm"))
        assertFalse(ProfileAddons.visibility(context, first)("dbm"))
    }

    @Test
    fun removingKeepsFilesAnotherProfileUses() {
        AddonDb.forAddonsDir(addonsDir).save(listOf(addon("questie", "Questie")))
        val first = profile("First")
        val second = profile("Second")
        ProfileAddons.joined(context, second, addonsDir, null)
        ProfileAddons.claim(context, second, setOf("questie"))

        assertTrue(ProfileAddons.release(context, first, addon("questie", "Questie")))
        assertFalse(ProfileAddons.visibility(context, first)("questie"))
        assertFalse(ProfileAddons.release(context, second, addon("questie", "Questie")))
    }

    @Test
    fun removingAnUnclaimedAddonHidesItOnlyHere() {
        val first = profile("First")
        val second = profile("Second")
        assertTrue(ProfileAddons.release(context, first, addon("local:Bagnon", "Bagnon")))
        assertFalse(ProfileAddons.visibility(context, first)("local:Bagnon"))
        assertTrue(ProfileAddons.visibility(context, second)("local:Bagnon"))
    }

    @Test
    fun launchTurnsOffTheOtherProfilesAddonsAndBackOn() {
        AddonDb.forAddonsDir(addonsDir).save(listOf(addon("questie", "Questie", "Questie-335"), addon("bagnon", "Bagnon")))
        val first = profile("First")
        val second = profile("Second")
        ProfileAddons.joined(context, second, addonsDir, null)
        ProfileAddons.claim(context, second, setOf("bagnon"))
        val wtf = File(root, "WTF")
        val account = File(wtf, "Account/DANIEL").apply { mkdirs() }
        val character = File(account, "Icecrown/Thrall").apply { mkdirs() }
        File(character, "AddOns.txt").writeText("Questie: enabled\r\nBagnon: enabled\r\nRecount: disabled\r\n")

        ProfileAddons.applyAtLaunch(context, second, addonsDir, wtf)
        assertEquals(
            listOf("Questie: disabled", "Bagnon: enabled", "Recount: disabled", "Questie-335: disabled"),
            File(character, "AddOns.txt").readLines().filter { it.isNotBlank() },
        )
        assertTrue(File(account, "AddOns.txt").readText().contains("Questie: disabled"))

        // Back on the first profile: Questie on again, Bagnon off; the player's own Recount choice stays.
        ProfileAddons.applyAtLaunch(context, first, addonsDir, wtf)
        assertEquals(
            listOf("Questie: enabled", "Bagnon: disabled", "Recount: disabled", "Questie-335: enabled"),
            File(character, "AddOns.txt").readLines().filter { it.isNotBlank() },
        )
    }

    @Test
    fun statesKeepOtherLines() {
        assertEquals(
            listOf("A: disabled", "B: enabled", "C: disabled"),
            ProfileAddons.withStates(listOf("A: enabled", "B: disabled", ""), off = setOf("A", "C"), on = setOf("B")),
        )
    }
}
