package app.gamenative.wow.legacy.mods

import app.gamenative.wow.legacy.LegacyClient
import java.io.File

/** A patch MPQ found in the player's client, with its locale twin when it has one. */
data class ClientPatch(val key: String, val name: String, val files: List<File>, val size: Long, val enabled: Boolean)

/** A patch we make for the 3.3.5a client, installed and removed from the Addons screen's Client tab. */
enum class AndroWherePatch(val title: String, val summary: String) {
    LOGIN(
        "AndroWhere Login",
        "Controller support on the login, realm and character screens, with button hints at the top. " +
            "A logs you in when Log in with A is on in Server login. Uses the ConsolePortLK controller layout. " +
            "Installing it changes 5 bytes of Wow.exe so the client accepts it, and keeps the original to put back when you remove it.",
    ),
}

/**
 * Add-ons that change the 3.3.5a client itself rather than its AddOns folder: our own login screen add-on,
 * and switches for the patch MPQs (HD packs and the like) already in the player's client.
 */
object ClientMods {
    const val DISABLED = ".disabled"
    private const val LOGIN_FILE = "AndroWhereLogin.lua"
    private const val PASSWORD_FILE = "AndroWhereLoginData.lua"
    private const val GLUE_DIR = "Interface\\GlueXML\\"
    private const val LOGIN_RESOURCE = "/androwhere/glue/$LOGIN_FILE"
    private const val GLUE_TOC = "Interface\\GlueXML\\GlueXML.toc"
    private const val TOC_LAST = "## This Always Runs Last"

    /** Our archive's letter. Locale patches with letters load above Blizzard's numbered ones and the HD packs' early letters. */
    private const val OUR_SUFFIX = "Z"

    private val PATCH = Regex("patch(?:-([a-z]{2}[A-Z]{2}))?(?:-([0-9A-Za-z]))?\\.mpq", RegexOption.IGNORE_CASE)
    private val LABEL_LINE = Regex("patch-([0-9a-z])\\.mpq\\s*-\\s*(.+)", RegexOption.IGNORE_CASE)
    private val PACK_LABEL = Regex("\"([0-9a-z])\"\\s*:\\s*\\{\\s*\"name\"\\s*:\\s*\"(?:\\(\\d+\\))?([^\"]+)\"", RegexOption.IGNORE_CASE)

    private fun locale(root: File) = LegacyClient.locales(root).firstOrNull() ?: "enUS"

    internal fun archive(root: File) = ourArchive(root)

    private fun ourArchive(root: File): File {
        val locale = locale(root)
        return File(root, "Data/$locale/patch-$locale-$OUR_SUFFIX.MPQ")
    }

    private fun exe(root: File) = File(root, LegacyClient.EXE)

    fun loginEnabled(root: File) = ourArchive(root).isFile

    fun installed(root: File, patch: AndroWherePatch) = when (patch) {
        AndroWherePatch.LOGIN -> loginEnabled(root)
    }

    fun setInstalled(root: File, patch: AndroWherePatch, installed: Boolean) = when (patch) {
        AndroWherePatch.LOGIN -> if (installed) enableLogin(root) else disableLogin(root)
    }

    /** Writes our login add-on and patches the exe so the client accepts it. */
    fun enableLogin(root: File, password: String? = null, autoLogin: Boolean = false) {
        WowExePatch.apply(exe(root))
        writeLogin(root, password, autoLogin)
    }

    fun disableLogin(root: File) {
        ourArchive(root).delete()
        WowExePatch.restore(exe(root))
    }

    /**
     * Rebuilds the add-on at launch so it follows the client's current login file list, e.g. after an HD patch changed.
     * With [password], the login screen fills it in, and with [autoLogin] also logs in once;
     * [wipePassword] takes it out again once the client has the archive open.
     */
    fun refresh(root: File, password: String? = null, autoLogin: Boolean = false) {
        if (loginEnabled(root)) enableLogin(root, password, autoLogin)
    }

    fun hasPassword(root: File) = ourArchive(root).isFile && Mpq.read(ourArchive(root), GLUE_DIR + PASSWORD_FILE) != null

    /** Rewrites the add-on without the password. A no-op when it holds none. */
    fun wipePassword(root: File) {
        if (hasPassword(root)) writeLogin(root, null, false)
    }

    /**
     * Writes a new archive and renames it over ours. The running client keeps reading the archive it opened,
     * so replacing it never corrupts the game, and the old copy, password included, is gone from storage.
     */
    @Synchronized
    private fun writeLogin(root: File, password: String?, autoLogin: Boolean) {
        val toc = effectiveGlueToc(root) ?: error("Couldn't find the client's login screen files.")
        val lua = checkNotNull(javaClass.getResourceAsStream(LOGIN_RESOURCE)) { "Missing $LOGIN_FILE" }.use { it.readBytes() }
        val archive = ourArchive(root)
        archive.parentFile?.mkdirs()
        val files = mutableMapOf(
            GLUE_TOC to withLoginFile(toc, password != null).toByteArray(Charsets.ISO_8859_1),
            GLUE_DIR + LOGIN_FILE to lua,
        )
        if (password != null) files[GLUE_DIR + PASSWORD_FILE] = loginDataLua(password, autoLogin).toByteArray(Charsets.ISO_8859_1)
        val temp = File(archive.parentFile, archive.name + ".tmp")
        Mpq.write(temp, files)
        if (!temp.renameTo(archive)) {
            temp.delete()
            error("Couldn't replace ${archive.name}.")
        }
    }

    /** Every byte as a decimal escape, so no password can break out of the Lua string. */
    internal fun loginDataLua(password: String, autoLogin: Boolean) =
        password.toByteArray(Charsets.UTF_8).joinToString("", "AndroWhereLoginPassword = \"", "\"\r\n") { "\\" + (it.toInt() and 0xFF) } +
            "AndroWhereLoginAuto = $autoLogin\r\n"

    /** Adds our files before the line Blizzard keeps last, so they run after every stock login file. */
    internal fun withLoginFile(toc: String, withPassword: Boolean = false): String {
        val lines = toc.replace("\r", "").split("\n").filterNot { it.trim() == LOGIN_FILE || it.trim() == PASSWORD_FILE }.toMutableList()
        val last = lines.indexOfFirst { it.startsWith(TOC_LAST) }.takeIf { it >= 0 } ?: lines.size
        lines.addAll(last, listOfNotNull(PASSWORD_FILE.takeIf { withPassword }, LOGIN_FILE))
        return lines.joinToString("\r\n")
    }

    /** The login file list the client would load without us: the highest priority archive that has one. */
    private fun effectiveGlueToc(root: File): String? {
        val locale = locale(root)
        val ours = ourArchive(root).name
        val candidates = (File(root, "Data/$locale").listFiles().orEmpty().filter { it.isFile && it.name != ours && PATCH.matches(it.name) }
            .sortedByDescending { priority(it.name) }) +
            listOf(File(root, "Data/$locale/locale-$locale.MPQ"))
        return candidates.firstNotNullOfOrNull { Mpq.read(it, GLUE_TOC) }?.toString(Charsets.ISO_8859_1)
    }

    /** Lettered patches load over numbered ones, which load over the plain patch archive. */
    internal fun priority(name: String): Int {
        val suffix = PATCH.matchEntire(name)?.groupValues?.get(2).orEmpty().uppercase()
        return when {
            suffix.isEmpty() -> 0
            suffix[0].isDigit() -> 1 + suffix[0].digitToInt()
            else -> 100 + (suffix[0] - 'A')
        }
    }

    /** Patch archives the player added, grouped by letter, without Blizzard's own or ours. */
    fun patches(root: File): List<ClientPatch> {
        val locale = locale(root)
        val labels = labels(root)
        val files = listOf(File(root, "Data"), File(root, "Data/$locale")).flatMap { dir -> dir.listFiles().orEmpty().filter { it.isFile } }
        return files.mapNotNull { file ->
            val enabled = !file.name.endsWith(DISABLED, ignoreCase = true)
            val match = PATCH.matchEntire(file.name.removeSuffix(DISABLED).removeSuffix(DISABLED.uppercase())) ?: return@mapNotNull null
            val key = match.groupValues[2].uppercase()
            if (key.isEmpty() || key == "2" || key == "3") return@mapNotNull null
            if (file.name.equals(ourArchive(root).name, ignoreCase = true)) return@mapNotNull null
            key to (file to enabled)
        }.groupBy({ it.first }, { it.second }).map { (key, entries) ->
            ClientPatch(
                key = key,
                name = labels[key] ?: "Patch $key",
                files = entries.map { it.first },
                size = entries.sumOf { it.first.length() },
                enabled = entries.all { it.second },
            )
        }.sortedBy { it.key }
    }

    /** Turns a patch on or off the way the HD packs' own menus do, by adding or removing `.disabled`. */
    fun setPatchEnabled(patch: ClientPatch, enabled: Boolean) {
        patch.files.forEach { file ->
            val off = file.name.endsWith(DISABLED, ignoreCase = true)
            val target = when {
                enabled && off -> File(file.parentFile, file.name.dropLast(DISABLED.length))
                !enabled && !off -> File(file.parentFile, file.name + DISABLED)
                else -> return@forEach
            }
            check(file.renameTo(target)) { "Couldn't rename ${file.name}." }
        }
    }

    /** Names from a pack's own label file or patch menu config, by patch letter. */
    internal fun labels(root: File): Map<String, String> {
        val listed = File(root, "Data/Patch_Lables.txt").takeIf { it.isFile }?.readLines().orEmpty()
            .mapNotNull { LABEL_LINE.matchEntire(it.trim())?.destructured?.let { (key, name) -> key.uppercase() to name.trim() } }
        val pack = File(root, "patchmenu/.config/config_en.json").takeIf { it.isFile }?.readText()
            ?.let { text -> PACK_LABEL.findAll(text).map { it.groupValues[1].uppercase() to it.groupValues[2].trim() }.toList() }
            .orEmpty()
        return (pack + listed).toMap()
    }
}
