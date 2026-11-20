package com.rfandango.haku_x.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * An XDVDFS filesystem (the Xbox disc image's) walked for a file: the volume
 * descriptor's root, and each folder a binary tree of dirents. Pure of
 * Android so the tests build one and read it back; XboxDisc owns the channel
 * and the partition the core's xdvdfs.c probes.
 */
object Xdvdfs {
  /** One directory entry: its name, whether it is a folder, and where its bytes are. */
  class Entry(val name: String, val isDir: Boolean, val sector: Long, val size: Long)

  /** A directory too far from the tree's rules to read. */
  class DiscException(msg: String) : Exception(msg)

  private const val SECTOR = 2048L
  private const val MAX_DIR = 1024 * 1024

  /** A reader over one game partition of an image. */
  class Reader(private val ch: FileChannel, private val partition: Long) {

    /** A folder's entries by path from the root ("maps"); null: the path is not there. */
    fun listDir(path: String): List<Entry>? {
      val root = rootDir() ?: return null
      var dir = root
      for (part in path.split('/').filter { it.isNotEmpty() }) {
        dir = findDir(dir, part) ?: return null
      }
      return readDir(dir).map { it.second }
    }

    /** A file's bytes by path from the root ("maps/coagulation.map"); null: not there. */
    fun readFile(path: String, maxBytes: Long = 512L shl 20): ByteArray? {
      val parts = path.split('/').filter { it.isNotEmpty() }
      if (parts.isEmpty()) return null
      val root = rootDir() ?: return null
      var dir = root
      for (part in parts.dropLast(1)) {
        dir = findDir(dir, part) ?: return null
      }
      val dirent = readDir(dir).firstOrNull { it.second.name.equals(parts.last(), ignoreCase = true) }?.second
        ?: return null
      if (dirent.isDir || dirent.size > maxBytes || dirent.size <= 0) return null
      return read(partition + dirent.sector * SECTOR, dirent.size.toInt())
    }

    /** The root directory's (sector, size) out of the volume descriptor. */
    private fun rootDir(): Pair<Long, Long>? {
      val vd = read(partition + 0x10000 + 20, 8) ?: return null
      val sector = vd.int.toLong() and 0xFFFFFFFFL
      val size = vd.int.toLong() and 0xFFFFFFFFL
      if (size == 0L || size > MAX_DIR) return null
      return sector to size
    }

    /** A subdirectory's place by name. */
    private fun findDir(dir: Pair<Long, Long>, name: String): Pair<Long, Long>? {
      val found = readDir(dir).firstOrNull { it.second.name.equals(name, ignoreCase = true) && it.second.isDir }
        ?: return null
      if (found.second.size == 0L || found.second.size > MAX_DIR) return null
      return found.second.sector to found.second.size
    }

    /** One directory block walked: each dirent with the offset it was read at. */
    private fun readDir(dir: Pair<Long, Long>): List<Pair<Int, Entry>> {
      val b = read(partition + dir.first * SECTOR, dir.second.toInt())
        ?: throw DiscException("the folder is out of the image")
      val out = mutableListOf<Pair<Int, Entry>>()
      fun walk(offset: Int, depth: Int) {
        if (depth > 64) throw DiscException("the folder's tree runs away")
        val left = b.getShort(offset).toInt() and 0xFFFF
        val right = b.getShort(offset + 2).toInt() and 0xFFFF
        val sector = b.getInt(offset + 4).toLong() and 0xFFFFFFFFL
        val size = b.getInt(offset + 8).toLong() and 0xFFFFFFFFL
        val flags = b.get(offset + 12).toInt() and 0xFF
        val nameLen = b.get(offset + 13).toInt() and 0xFF
        if (offset + 14 + nameLen > b.size) throw DiscException("a name runs past the folder")
        val name = String(ByteArray(nameLen) { b.get(offset + 14 + it) }, Charsets.US_ASCII)
        if (name.isNotEmpty()) {
          out += offset to Entry(name, flags and 0x10 != 0, sector, size)
        }
        if (left != 0 && left != 0xFFFF) walk(left * 4, depth + 1)
        if (right != 0 && right != 0xFFFF) walk(right * 4, depth + 1)
      }
      walk(0, 0)
      return out
    }

    private fun read(at: Long, n: Int): ByteArray? {
      if (n <= 0 || at < 0) return null
      val buf = ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN)
      var pos = at
      while (buf.hasRemaining()) {
        val got = ch.read(buf, pos)
        if (got <= 0) return null
        pos += got
      }
      return buf.array()
    }
  }
}
