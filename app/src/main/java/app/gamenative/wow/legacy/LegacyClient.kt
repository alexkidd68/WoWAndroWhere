package app.gamenative.wow.legacy

import android.content.Context
import java.io.File
import java.io.RandomAccessFile

/** A pre-CASC 32-bit client (1.12, 2.4.3, 3.3.5a) for private servers. Players supply the folder. */
data class ClientBuild(val major: Int, val minor: Int, val patch: Int, val build: Int) {
    override fun toString() = "$major.$minor.$patch.$build"
}

enum class LegacyRenderer(val label: String, val dxwrapper: String, val gxApi: String, val execArgs: String) {
    DXVK("DXVK (D3D9)", "dxvk", "d3d9", ""),
    WINED3D("WineD3D (D3D9)", "wined3d", "d3d9", ""),
    OPENGL("OpenGL", "wined3d", "opengl", "-opengl"),
}

data class LegacySettings(
    val root: String = "",
    val realmlist: String = "",
    val renderer: LegacyRenderer = LegacyRenderer.DXVK,
)

object LegacyClient {
    const val EXE = "Wow.exe"
    const val LOG = "AndroWhere-wine.log"

    /** The bundled 32-bit emulator. FEX's WoW64 DLL isn't bundled, so only Box64 (wowbox64) can run x86 code. */
    const val EMULATOR = "Box64"
    private const val PREFS = "wow_legacy"
    private const val PENDING_LOG = "log_pending_previous_debug"
    private const val LOG_FLAG_MIGRATED = "log_flag_migrated"
    private const val FIXED_FILE_INFO_SIGNATURE = 0xFEEF04BDL
    private val LOCALE = Regex("[a-z]{2}[A-Z]{2}")
    private val REALMLIST_LINE = Regex("set\\s+realmlist\\s+(.*)", RegexOption.IGNORE_CASE)

    fun load(context: Context): LegacySettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return LegacySettings(
            root = p.getString("root", "").orEmpty(),
            realmlist = p.getString("realmlist", "").orEmpty(),
            renderer = LegacyRenderer.entries.firstOrNull { it.name == p.getString("renderer", null) } ?: LegacyRenderer.DXVK,
        )
    }

    fun save(context: Context, settings: LegacySettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("root", settings.root)
            .putString("realmlist", settings.realmlist)
            .putString("renderer", settings.renderer.name)
            .apply()
    }

    /**
     * Remembers, across the launcher being rebuilt while the game runs, that a legacy launch turned on Wine
     * logging and what the setting was before.
     */
    fun markLogPending(context: Context, previousDebug: Boolean) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains(PENDING_LOG)) p.edit().putBoolean(PENDING_LOG, previousDebug).apply()
    }

    /** The Wine debug setting from before the last legacy launch, or null when no launch is waiting. Clears it. */
    fun takePendingLog(context: Context): Boolean? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Test builds before this kept the flag in memory, lost it, and left Wine logging on; the app has no
        // other switch for it, so turn it off once.
        if (!p.getBoolean(LOG_FLAG_MIGRATED, false)) {
            p.edit().putBoolean(LOG_FLAG_MIGRATED, true).apply()
            if (!p.contains(PENDING_LOG)) return false
        }
        if (!p.contains(PENDING_LOG)) return null
        val previous = p.getBoolean(PENDING_LOG, false)
        p.edit().remove(PENDING_LOG).apply()
        return previous
    }

    /** The folder holding [EXE] and `Data/`, searching [dir] and one level below it. */
    fun findRoot(dir: File): File? =
        sequenceOf(dir).plus(dir.listFiles()?.filter { it.isDirectory }.orEmpty())
            .firstOrNull { File(it, EXE).isFile && File(it, "Data").isDirectory }

    fun detectBuild(exe: File): ClientBuild? = runCatching {
        RandomAccessFile(exe, "r").use { file ->
            val bytes = ByteArray(minOf(file.length(), MAX_EXE_BYTES).toInt())
            file.readFully(bytes)
            parseBuild(bytes)
        }
    }.getOrNull()

    /** Reads `VS_FIXEDFILEINFO.dwFileVersion` from the PE version resource. */
    internal fun parseBuild(bytes: ByteArray): ClientBuild? {
        for (i in bytes.size - 16 downTo 0) {
            if (u32(bytes, i) != FIXED_FILE_INFO_SIGNATURE) continue
            val ms = u32(bytes, i + 8)
            val ls = u32(bytes, i + 12)
            return ClientBuild((ms ushr 16).toInt(), (ms and 0xFFFF).toInt(), (ls ushr 16).toInt(), (ls and 0xFFFF).toInt())
        }
        return null
    }

    private fun u32(b: ByteArray, i: Int): Long =
        (b[i].toLong() and 0xFF) or ((b[i + 1].toLong() and 0xFF) shl 8) or
            ((b[i + 2].toLong() and 0xFF) shl 16) or ((b[i + 3].toLong() and 0xFF) shl 24)

    fun locales(root: File): List<String> =
        File(root, "Data").listFiles()?.filter { it.isDirectory && LOCALE.matches(it.name) }?.map { it.name }?.sorted().orEmpty()

    /** The realm the client is already set to, from the first `realmlist.wtf` found. */
    fun readRealmlist(root: File): String? =
        (locales(root).map { File(root, "Data/$it/realmlist.wtf") } + File(root, "realmlist.wtf"))
            .firstOrNull { it.isFile }
            ?.readLines()
            ?.firstNotNullOfOrNull { REALMLIST_LINE.matchEntire(it.trim())?.groupValues?.get(1)?.trim('"', ' ') }
            ?.takeIf { it.isNotBlank() }

    /** Sets the realm in every locale's `realmlist.wtf`, keeping its other lines, and in `WTF/Config.wtf`. */
    fun applyConfig(root: File, settings: LegacySettings, screenSize: String) {
        val locales = locales(root)
        locales.forEach {
            val file = File(root, "Data/$it/realmlist.wtf")
            file.writeText(updateRealmlist(file.takeIf { f -> f.isFile }?.readText(), settings.realmlist))
        }
        val config = File(root, "WTF/Config.wtf")
        val lines = buildConfigWtf(config.takeIf { it.isFile }?.readLines(), settings, screenSize, locales.firstOrNull() ?: "enUS")
        config.parentFile?.mkdirs()
        config.writeText(lines.joinToString("\n", postfix = "\n"))
    }

    internal fun updateRealmlist(existing: String?, realmlist: String): String {
        val line = "set realmlist ${realmlist.trim()}"
        val others = existing.orEmpty().lines().map { it.trimEnd('\r') }.filter { it.isNotBlank() && !REALMLIST_LINE.matches(it.trim()) }
        return (listOf(line) + others).joinToString("\n", postfix = "\n")
    }

    /** Realm, renderer and resolution are always enforced; the rest is only a first-boot default so in-game changes stick. */
    internal fun buildConfigWtf(existing: List<String>?, settings: LegacySettings, screenSize: String, locale: String): List<String> {
        val forced = linkedMapOf(
            "realmList" to settings.realmlist.trim(),
            "gxApi" to settings.renderer.gxApi,
            "gxResolution" to screenSize,
        )
        val defaults = linkedMapOf(
            "locale" to locale,
            "gxWindow" to "1",
            "gxMaximize" to "1",
            "movie" to "0",
        )
        val lines = existing.orEmpty().filter { line -> forced.keys.none { line.startsWith("SET $it ", ignoreCase = true) } }
        val missing = defaults.filterKeys { key -> lines.none { it.startsWith("SET $key ", ignoreCase = true) } }
        return lines + (forced + missing).map { "SET ${it.key} \"${it.value}\"" }
    }

    private const val MAX_EXE_BYTES = 64L * 1024 * 1024

    /** Logs over this size keep their start (DLL loading) and their end (the crash) so they stay shareable. */
    const val LOG_HEAD = 1L shl 20
    const val LOG_TAIL = 3L shl 20

    fun trimLog(source: File, target: File) {
        val size = source.length()
        if (size <= LOG_HEAD + LOG_TAIL) {
            source.copyTo(target, overwrite = true)
            return
        }
        RandomAccessFile(source, "r").use { input ->
            target.outputStream().buffered().use { out ->
                val buffer = ByteArray(64 shl 10)
                fun copy(from: Long, length: Long) {
                    input.seek(from)
                    var left = length
                    while (left > 0) {
                        val read = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        left -= read
                    }
                }
                copy(0, LOG_HEAD)
                out.write("\n[... ${size - LOG_HEAD - LOG_TAIL} bytes cut ...]\n".toByteArray())
                copy(size - LOG_TAIL, LOG_TAIL)
            }
        }
    }
}
