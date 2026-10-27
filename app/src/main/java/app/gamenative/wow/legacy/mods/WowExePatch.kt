package app.gamenative.wow.legacy.mods

import java.io.File
import java.io.RandomAccessFile

/**
 * The 3.3.5a (build 12340) client refuses changed login screen files ("Your login interface files are
 * corrupt"). These five changes, known from community clients, let it load them; tested on Daniel's PC
 * on 2026-10-27, where the single interface-check byte some guides name was not enough.
 */
object WowExePatch {
    private class Spot(val offset: Long, original: String, patched: String) {
        val original = original.hexBytes()
        val patched = patched.hexBytes()
    }

    private val SPOTS = listOf(
        Spot(0x415A25, "75", "EB"),
        Spot(0x415A3F, "01", "03"),
        Spot(0x415A95, "01", "03"),
        Spot(0x415B46, "7F", "EB"),
        Spot(0x415B5F, "83C0035E8BE55D", "B803000000EBED"),
    )

    const val BACKUP_SUFFIX = ".androwhere-original"

    enum class State { ORIGINAL, PATCHED, UNKNOWN }

    fun state(exe: File): State = runCatching {
        RandomAccessFile(exe, "r").use { file ->
            val found = SPOTS.map { spot -> ByteArray(spot.original.size).also { file.seek(spot.offset); file.readFully(it) } }
            when {
                SPOTS.zip(found).all { (spot, bytes) -> bytes.contentEquals(spot.original) } -> State.ORIGINAL
                SPOTS.zip(found).all { (spot, bytes) -> bytes.contentEquals(spot.patched) } -> State.PATCHED
                else -> State.UNKNOWN
            }
        }
    }.getOrDefault(State.UNKNOWN)

    fun backupOf(exe: File) = File(exe.parentFile, exe.name + BACKUP_SUFFIX)

    /** Patches an original exe, keeping a copy to put back. An exe that's already patched is left alone. */
    fun apply(exe: File) {
        when (state(exe)) {
            State.PATCHED -> return
            State.UNKNOWN -> error("This Wow.exe isn't the 3.3.5a (12340) client, so login screen add-ons can't be used with it.")
            State.ORIGINAL -> Unit
        }
        val backup = backupOf(exe)
        if (!backup.isFile) exe.copyTo(backup)
        RandomAccessFile(exe, "rw").use { file ->
            SPOTS.forEach { file.seek(it.offset); file.write(it.patched) }
        }
    }

    /** Puts back the exe [apply] copied. An exe patched by something else stays as it is. */
    fun restore(exe: File) {
        val backup = backupOf(exe)
        if (!backup.isFile) return
        backup.copyTo(exe, overwrite = true)
        backup.delete()
    }

    private fun String.hexBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
