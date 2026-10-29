package app.gamenative.externaldisplay

/**
 * The game the second-screen pad is serving, set before the game starts. Stands in for the
 * launcher's game flavor in the pad code taken from wyattabuntjer/AYN-Thor-WoW-Launcher.
 */
object PadGame {
    /** Key for the profile the pad loads at launch. */
    @Volatile var name: String = "legacy"

    /** Shown in the pad settings ("Loads at launch for ..."). */
    @Volatile var label: String = "private servers"

    /** Where the pad's layout backup goes; next to the game, so it survives reinstalling the app. */
    @Volatile var backupDir: String = ""
}
