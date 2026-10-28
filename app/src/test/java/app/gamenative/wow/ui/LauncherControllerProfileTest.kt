package app.gamenative.wow.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.gamenative.wow.install.InstallController
import app.gamenative.wow.install.InstallProgress
import app.gamenative.wow.install.InstallRequest
import app.gamenative.wow.install.InstallUiState
import app.gamenative.wow.product.GameVersion
import app.gamenative.wow.product.WowProduct
import app.gamenative.wow.profile.Profiles
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Android 10: Robolectric has no storage volumes for the Android 11+ all-files-access check.
@Config(sdk = [29])
@RunWith(RobolectricTestRunner::class)
class LauncherControllerProfileTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun launcher() = LauncherController(context, CoroutineScope(Dispatchers.Unconfined)) {}

    @Before
    fun load() = Profiles.load(context)

    @After
    fun reset() {
        InstallController.update(InstallUiState.Idle)
    }

    private fun profile(name: String, version: GameVersion, root: String) =
        Profiles.create(context, name, version).copy(root = root).also { Profiles.update(context, it) }

    @Test
    fun switchRefusedWhileInstalling() {
        val forever = profile("Forever", GameVersion.FOREVER, "/forever")
        val retail = profile("Retail", GameVersion.RETAIL, "/retail")
        Profiles.activate(context, forever.id)
        val launcher = launcher()
        InstallController.update(
            InstallUiState.Running(InstallRequest(File("/x"), WowProduct.FOREVER), InstallProgress(InstallProgress.Phase.entries.first(), 0, 1, null)),
        )
        assertFalse(launcher.switchProfile(retail.id))
        assertEquals(forever.id, Profiles.activeId)
        assertEquals(GameVersion.FOREVER, launcher.version)
    }

    @Test
    fun switchLoadsTheProfilesVersionAndFolder() {
        val forever = profile("Forever", GameVersion.FOREVER, "/forever")
        val retail = profile("Retail", GameVersion.RETAIL, "/retail")
        Profiles.activate(context, forever.id)
        val launcher = launcher()

        assertTrue(launcher.switchProfile(retail.id))
        assertEquals("/retail", launcher.gamePath)
        assertEquals(GameVersion.RETAIL, launcher.version)
        assertEquals("retail", context.getSharedPreferences("wow_settings", Context.MODE_PRIVATE).getString("game_version", null))

        launcher.cycleProfile(1)
        assertEquals("/forever", launcher.gamePath)
    }

    @Test
    fun privateProfilesKeepTheirOwnServers() {
        val warmane = profile("Warmane", GameVersion.PRIVATE_335, "/client")
        val other = profile("Other", GameVersion.PRIVATE_335, "/client")
        Profiles.activate(context, warmane.id)
        val launcher = launcher()
        launcher.updateServer(launcher.selectedServer.copy(realmlist = "logon.warmane.com"))

        assertTrue(launcher.switchProfile(other.id))
        assertEquals("", launcher.legacy.realmlist)
        assertNotEquals("logon.warmane.com", launcher.selectedServer.realmlist)

        assertTrue(launcher.switchProfile(warmane.id))
        assertEquals("logon.warmane.com", launcher.legacy.realmlist)
    }

    @Test
    fun newProfileReplacesABlizzardProfileWithoutAFolder() {
        val unset = Profiles.create(context, "WoW Forever", GameVersion.FOREVER)
        Profiles.activate(context, unset.id)
        val launcher = launcher()

        assertTrue(launcher.createProfile("", GameVersion.CLASSIC_ERA))
        assertEquals(listOf("Classic Era"), launcher.profiles.map { it.name })
        assertEquals(GameVersion.CLASSIC_ERA, launcher.version)
    }
}
