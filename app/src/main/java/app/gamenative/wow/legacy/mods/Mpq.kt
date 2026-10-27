package app.gamenative.wow.legacy.mods

import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/**
 * Just enough of Blizzard's MPQ format for client add-ons: reading small files (zlib or bzip2 sectors)
 * from the client's archives, and writing a small uncompressed archive of our own.
 */
object Mpq {
    private val cryptTable = IntArray(0x500).also { table ->
        var seed = 0x00100001
        for (i in 0 until 256) {
            var index = i
            repeat(5) {
                seed = (seed * 125 + 3) % 0x2AAAAB
                val high = (seed and 0xFFFF) shl 16
                seed = (seed * 125 + 3) % 0x2AAAAB
                table[index] = high or (seed and 0xFFFF)
                index += 256
            }
        }
    }

    private const val HASH_OFFSET = 0
    private const val HASH_A = 1
    private const val HASH_B = 2
    private const val HASH_KEY = 3

    private const val FILE_EXISTS = 0x80000000.toInt()
    private const val FILE_COMPRESS = 0x00000200
    private const val FILE_ENCRYPTED = 0x00010000
    private const val FILE_SINGLE_UNIT = 0x01000000
    private const val EMPTY = -1

    internal fun hash(name: String, type: Int): Int {
        var seed1 = 0x7FED7FED
        var seed2 = 0xEEEEEEEE.toInt()
        for (ch in name.uppercase().replace('/', '\\')) {
            val c = ch.code
            seed1 = cryptTable[type * 256 + c] xor (seed1 + seed2)
            seed2 = c + seed1 + seed2 + (seed2 shl 5) + 3
        }
        return seed1
    }

    private fun crypt(words: IntArray, startKey: Int, decrypt: Boolean) {
        var key = startKey
        var seed = 0xEEEEEEEE.toInt()
        for (i in words.indices) {
            seed += cryptTable[0x400 + (key and 0xFF)]
            val plain = if (decrypt) words[i] xor (key + seed) else words[i]
            words[i] = words[i] xor (key + seed)
            key = ((key.inv() shl 0x15) + 0x11111111) or (key ushr 0x0B)
            seed = plain + seed + (seed shl 5) + 3
        }
    }

    private fun words(bytes: ByteArray) = IntArray(bytes.size / 4).also { ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(it) }

    private fun bytes(words: IntArray) = ByteBuffer.allocate(words.size * 4).order(ByteOrder.LITTLE_ENDIAN).also { it.asIntBuffer().put(words) }.array()

    /** Reads [name] from [archive], or null when it isn't there or uses a compression we don't read. */
    fun read(archive: File, name: String): ByteArray? = runCatching {
        RandomAccessFile(archive, "r").use { file ->
            val header = ByteArray(32).also { file.readFully(it) }
            val h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            if (h.getInt(0) != 0x1A51504D) return null
            val sectorSize = 512 shl h.getShort(14).toInt()
            val hashTable = words(ByteArray(h.getInt(24) * 16).also { file.seek(h.getInt(16).toLong() and 0xFFFFFFFFL); file.readFully(it) })
            val blockTable = words(ByteArray(h.getInt(28) * 16).also { file.seek(h.getInt(20).toLong() and 0xFFFFFFFFL); file.readFully(it) })
            crypt(hashTable, hash("(hash table)", HASH_KEY), decrypt = true)
            crypt(blockTable, hash("(block table)", HASH_KEY), decrypt = true)

            val entries = hashTable.size / 4
            val a = hash(name, HASH_A)
            val b = hash(name, HASH_B)
            var i = Integer.remainderUnsigned(hash(name, HASH_OFFSET), entries)
            var block = -1
            for (step in 0 until entries) {
                val index = hashTable[i * 4 + 3]
                if (index == EMPTY) break
                if (hashTable[i * 4] == a && hashTable[i * 4 + 1] == b && index in 0 until blockTable.size / 4) {
                    block = index
                    break
                }
                i = (i + 1) % entries
            }
            if (block < 0) return null
            val offset = blockTable[block * 4].toLong() and 0xFFFFFFFFL
            val packedSize = blockTable[block * 4 + 1]
            val size = blockTable[block * 4 + 2]
            val flags = blockTable[block * 4 + 3]
            if ((flags and FILE_EXISTS) == 0 || (flags and FILE_ENCRYPTED) != 0) return null
            val raw = ByteArray(packedSize).also { file.seek(offset); file.readFully(it) }
            when {
                (flags and FILE_COMPRESS) == 0 -> raw.copyOf(size)
                (flags and FILE_SINGLE_UNIT) != 0 -> unpack(raw, size)
                else -> {
                    val sectors = (size + sectorSize - 1) / sectorSize
                    val starts = ByteBuffer.wrap(raw, 0, (sectors + 1) * 4).order(ByteOrder.LITTLE_ENDIAN)
                    val out = java.io.ByteArrayOutputStream(size)
                    for (s in 0 until sectors) {
                        val from = starts.getInt(s * 4)
                        val to = starts.getInt((s + 1) * 4)
                        out.write(unpack(raw.copyOfRange(from, to), minOf(sectorSize, size - s * sectorSize)) ?: return null)
                    }
                    out.toByteArray()
                }
            }
        }
    }.getOrNull()

    private fun unpack(data: ByteArray, size: Int): ByteArray? {
        if (data.size >= size) return data.copyOf(size)
        val body = data.copyOfRange(1, data.size)
        return when (data[0].toInt() and 0xFF) {
            0x02 -> Inflater().run {
                setInput(body)
                ByteArray(size).also { inflate(it); end() }
            }
            0x10 -> BZip2CompressorInputStream(ByteArrayInputStream(body)).use { input ->
                ByteArray(size).also { out ->
                    var read = 0
                    while (read < size) {
                        val n = input.read(out, read, size - read)
                        if (n < 0) break
                        read += n
                    }
                }
            }
            else -> null
        }
    }

    /** Writes an uncompressed archive holding [files] (MPQ paths with backslashes) and a listfile. */
    fun write(archive: File, files: Map<String, ByteArray>) {
        val all = LinkedHashMap(files).apply { put("(listfile)", files.keys.joinToString("\r\n", postfix = "\r\n").toByteArray()) }
        var slots = 16
        while (slots < all.size * 2) slots *= 2
        val hashTable = IntArray(slots * 4) { EMPTY }
        val blockTable = IntArray(all.size * 4)
        val body = java.io.ByteArrayOutputStream()
        all.entries.forEachIndexed { index, (name, data) ->
            blockTable[index * 4] = 32 + body.size()
            blockTable[index * 4 + 1] = data.size
            blockTable[index * 4 + 2] = data.size
            blockTable[index * 4 + 3] = FILE_EXISTS
            body.write(data)
            var slot = Integer.remainderUnsigned(hash(name, HASH_OFFSET), slots)
            while (hashTable[slot * 4 + 3] != EMPTY) slot = (slot + 1) % slots
            hashTable[slot * 4] = hash(name, HASH_A)
            hashTable[slot * 4 + 1] = hash(name, HASH_B)
            hashTable[slot * 4 + 2] = 0
            hashTable[slot * 4 + 3] = index
        }
        crypt(hashTable, hash("(hash table)", HASH_KEY), decrypt = false)
        crypt(blockTable, hash("(block table)", HASH_KEY), decrypt = false)
        val hashBytes = bytes(hashTable)
        val blockBytes = bytes(blockTable)
        val hashOffset = 32 + body.size()
        val blockOffset = hashOffset + hashBytes.size
        val header = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x1A51504D).putInt(32).putInt(blockOffset + blockBytes.size)
            .putShort(0).putShort(3)
            .putInt(hashOffset).putInt(blockOffset).putInt(slots).putInt(all.size)
        archive.outputStream().use {
            it.write(header.array())
            body.writeTo(it)
            it.write(hashBytes)
            it.write(blockBytes)
        }
    }
}
