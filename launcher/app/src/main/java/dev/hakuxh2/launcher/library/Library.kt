package dev.hakuxh2.launcher.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One imported dump in the launcher library (Phase 0: metadata only, no core wiring yet). */
data class GameEntry(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val verified: Boolean,
)

/**
 * XDVDFS probe: a real Xbox image carries the ASCII magic "MICROSOFT*XBOX" in the
 * volume descriptor within its first 64 KiB. Lighter than a full sector walk and
 * enough to warn about mis-dumped files at import time.
 */
object XisoProbe {
    private val MAGIC = "MICROSOFT*XBOX".toByteArray(Charsets.US_ASCII)
    private const val WINDOW = 0x10000

    fun hasXboxSignature(context: Context, uri: Uri): Boolean =
        context.contentResolver.openInputStream(uri)?.use { ins ->
            val buf = ByteArray(WINDOW)
            var off = 0
            while (off < buf.size) {
                val n = ins.read(buf, off, buf.size - off)
                if (n < 0) break
                off += n
            }
            indexOf(buf, off, MAGIC) >= 0
        } ?: false

    private fun indexOf(hay: ByteArray, len: Int, needle: ByteArray): Int {
        outer@ for (i in 0..len - needle.size) {
            for (j in needle.indices) if (hay[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }
}

/** Persisted library backed by SharedPreferences + org.json — zero extra dependencies. */
object LibraryStore {
    private const val PREFS = "library"
    private const val KEY = "entries"

    fun load(context: Context): List<GameEntry> = runCatching {
        val arr = JSONArray(context.getSharedPreferences(PREFS, 0).getString(KEY, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            GameEntry(o.getString("name"), o.getString("path"), o.getLong("sizeBytes"), o.getBoolean("verified"))
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, entries: List<GameEntry>) {
        val arr = JSONArray()
        entries.forEach {
            arr.put(
                JSONObject()
                    .put("name", it.name)
                    .put("path", it.path)
                    .put("sizeBytes", it.sizeBytes)
                    .put("verified", it.verified)
            )
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, arr.toString()).apply()
    }

    /** Display name as reported by the storage provider for the picked document. */
    fun queryPickedDisplayName(context: Context, uri: Uri): String {
        var name = "dump.bin"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    name = c.getString(0) ?: name
                }
            }
        return name
    }

    /** Copy-on-import: the pristine dump on the user's storage is never touched. */
    fun copyIntoDumps(context: Context, uri: Uri, displayName: String): File {
        val dir = File(context.filesDir, "dumps").apply { mkdirs() }
        var target = File(dir, displayName)
        var i = 1
        while (target.exists()) {
            val dot = displayName.lastIndexOf('.')
            target = if (dot > 0) {
                File(dir, displayName.substring(0, dot) + " ($i)" + displayName.substring(dot))
            } else {
                File(dir, "$displayName ($i)")
            }
            i++
        }
        context.contentResolver.openInputStream(uri)?.use { ins ->
            target.outputStream().use { outs -> ins.copyTo(outs) }
        } ?: throw IllegalStateException("Cannot open $uri")
        return target
    }
}
