package com.rfandango.haku_x.data

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * Halo cache maps (.map) and the modders' patch files that make them, as
 * Assembly (XboxChaos' Blamite library) reads and writes them — the formats
 * in docs/mcc-map-research.md, ported so the importer knows what it is
 * holding and can build a modded Xbox map from the disc's own. Pure Kotlin:
 * no Android, so the JVM tests cover it all.
 */
object HaloMap {
  /** What a .map is. */
  enum class Kind { XBOX_H2, VISTA_H2, MCC_H2C, OTHER_HALO, UNKNOWN }

  /** A map's header read: what it is and who it says it is. */
  data class Info(
    val kind: Kind,
    /** The engine's version int (0x4): 8 the Xbox/Vista Halo 2, 13 the MCC one. */
    val version: Int,
    /** The build string ("02.09.27.09809" the disc's, "11081..." Vista's; "" unknown). */
    val build: String,
    /** The map's own name ("coagulation"): the file it is on a Halo 2 disc. */
    val internalName: String,
    val scenarioName: String,
    /** What the map is for: 0 campaign, 1 multiplayer, 2 the menu, 3 shared, 4 both, 5 sounds. */
    val type: Int,
    /** The MCC header's revision (true: Update 1, the current one). Null when not MCC. */
    val mccU1: Boolean?,
    /** An MCC map as stored (compressed); null when not MCC or already decompressed. */
    val mccCompressed: Boolean?,
  ) {
    val typeText: String
      get() = when (type) {
        0 -> "campaign"; 1 -> "multiplayer"; 2 -> "the menu"; 3 -> "shared"; 4 -> "campaign and multiplayer"; 5 -> "sounds"
        else -> "?"
      }
  }

  /** A header read: an UNKNOWN of the given reason. */
  private fun unknown(reason: String) =
    Info(Kind.UNKNOWN, 0, reason, "", "", -1, null, null)

  /** The map's header: what family it is from, and its own name. */
  fun read(b: ByteArray): Info {
    if (b.size < 0x400) return unknown("too small for a Halo map")
    /* H2 and MCC maps are little-endian ("daeh"); Halo 1's Xbox maps are big ("head") */
    if (!(b[0] == 'd'.code.toByte() && b[1] == 'a'.code.toByte() && b[2] == 'e'.code.toByte() && b[3] == 'h'.code.toByte())) {
      return if (b[0] == 'h'.code.toByte() && b[1] == 'e'.code.toByte() && b[2] == 'a'.code.toByte() && b[3] == 'd'.code.toByte()) {
        Info(Kind.OTHER_HALO, le32(b, 4), "big-endian: Halo 1's", "", "", -1, null, null)
      } else {
        unknown("not a Halo map")
      }
    }
    val version = le32(b, 4)
    return when {
      version == 8 -> {
        val build = asciiz(b, 0x120, 0x40)
        when {
          /* the disc: campaign 9809, the shipped MP maps 9214 (Engines.xml's "Halo 2" and "Halo 2 box_mp") */
          build.startsWith("02.") -> Info(
            Kind.XBOX_H2, version, build, asciiz(b, 0x198, 0x40), asciiz(b, 0x1BC, 0x40), le32(b, 0x140), null, null)
          build.startsWith("11081") || build.startsWith("11028") -> Info(
            Kind.VISTA_H2, version, build, asciiz(b, 0x198, 0x40), asciiz(b, 0x1BC, 0x40), le32(b, 0x140), null, null)
          else -> Info(Kind.OTHER_HALO, version, build, "", "", -1, null, null)
        }
      }
      version == 13 -> {
        /* MCC classic: two header revisions; Update 1 carries its compression in the header */
        val u1 = le32(b, 0x308) == 0x40000 && le32(b, 0x310) in 0x380 until b.size && le32(b, 0x314) > 0
        if (u1) {
          Info(Kind.MCC_H2C, version, asciiz(b, 0x90, 0x40), asciiz(b, 0xB0, 0x40), asciiz(b, 0xD0, 0x40),
            le16(b, 0x18).toInt(), true, le16(b, 0x1C) and 1 == 1)
        } else {
          /* the old header: compressed unless its meta area is already readable */
          val meta = le32(b, 0xC).toLong() and 0xFFFFFFFFL
          val plain = meta + 0x20 < b.size && b[(meta + 0x1C).toInt()] == 't'.code.toByte()
          Info(Kind.MCC_H2C, version, asciiz(b, 0x128, 0x40), asciiz(b, 0x1C0, 0x40), asciiz(b, 0x1E4, 0x40),
            le32(b, 0x148), false, !plain)
        }
      }
      version == 5 || version == 609 || version == 11 || version == 12 ->
        Info(Kind.OTHER_HALO, version, asciiz(b, 0x120, 0x40), "", "", -1, null, null)
      else -> unknown("a Halo map of version $version")
    }
  }

  /* ---- little-endian reads ---- */
  fun le16(b: ByteArray, at: Int): Int = (b[at].toInt() and 0xFF) or (b[at + 1].toInt() and 0xFF shl 8)
  fun le32(b: ByteArray, at: Int): Int =
    (b[at].toInt() and 0xFF) or (b[at + 1].toInt() and 0xFF shl 8) or
      (b[at + 2].toInt() and 0xFF shl 16) or (b[at + 3].toInt() and 0xFF shl 24)

  /** A null-terminated ASCII string at at, at most len bytes. */
  fun asciiz(b: ByteArray, at: Int, len: Int): String {
    if (at < 0 || at >= b.size) return ""
    val end = minOf(at + len, b.size)
    val sb = StringBuilder()
    for (i in at until end) {
      val c = b[i].toInt() and 0xFF
      if (c == 0) break
      sb.append(c.toChar())
    }
    return sb.toString()
  }
}

/**
 * MCC's map compression (Saber zlib, SecondGenSaberZLib.cs): the header
 * uncompressed, the rest in 0x40000-byte chunks of raw deflate behind a
 * 2-byte marker, padded to 0x80, and a table of (size, offset) pairs —
 * Update 1 keeps the table's place in the header, the old header has it
 * fixed at 0x1000. A negative size is a chunk some tool stored raw.
 */
object Saber {
  class MapFormatException(msg: String) : Exception(msg)

  /** The map decompressed (an already-plain one comes back as it is). */
  fun decompress(b: ByteArray, u1: Boolean): ByteArray {
    val headerSize = if (u1) 0x380 else 0x1000
    if (b.size <= headerSize) throw MapFormatException("too small for its header")
    val chunkSize = if (u1) HaloMap.le32(b, 0x308) else 0x40000
    val tableAt = if (u1) HaloMap.le32(b, 0x310) else 0x1000
    val count = if (u1) HaloMap.le32(b, 0x314) else 0x400
    if (chunkSize <= 0 || chunkSize > 0x1000000) throw MapFormatException("a chunk size of $chunkSize")
    if (tableAt < 0 || tableAt + 8 > b.size) throw MapFormatException("its chunk table is out of the file")
    if (count <= 0 || count > 0x10000) throw MapFormatException("a chunk count of $count")

    val out = ByteArrayOutputStream(maxOf(b.size, chunkSize * 2))
    out.write(b, 0, headerSize)

    var at = tableAt
    var wrote = 0
    for (i in 0 until count) {
      if (at + 8 > b.size) break
      val size = HaloMap.le32(b, at)
      val offset = HaloMap.le32(b, at + 4)
      at += 8
      if (size == 0) break /* the table's end */
      if (offset < 0 || offset >= b.size) throw MapFormatException("chunk $i points out of the file")
      if (size < 0) {
        /* stored raw by another tool */
        val n = minOf(-size, b.size - offset)
        if (n < 0) throw MapFormatException("chunk $i is stored past the file's end")
        out.write(b, offset, n)
        wrote += n
      } else {
        /* raw deflate behind a 2-byte marker */
        if (offset + 2 >= b.size) throw MapFormatException("chunk $i is past the file's end")
        val inf = Inflater(true)
        inf.setInput(b, offset + 2, b.size - offset - 2)
        val buf = ByteArray(chunkSize)
        val n = inf.inflate(buf)
        inf.end()
        if (n <= 0) throw MapFormatException("chunk $i would not decompress")
        out.write(buf, 0, n)
        wrote += n
      }
    }
    if (wrote == 0) throw MapFormatException("no chunks in its table")

    val plain = out.toByteArray()
    if (u1) {
      /* the compressed bit off, so the game loads it plain */
      val flags = plain[0x1C].toInt() and 0xFE
      plain[0x1C] = flags.toByte()
    }
    return plain
  }
}

/** Why a patch would not go on. */
class MapPatchException(msg: String) : Exception(msg)

/**
 * An Assembly patch (.asmp) or one of the older tools' (.patchdat Ascension,
 * .ascpatch Alteration): the map it is for and the changes it makes. The
 * older ones carry no map name — the file they came in has to.
 */
class MapPatch(
  /** The map's internal name the patch is for ("coagulation"); null: the old kinds. */
  val mapName: String?,
  val title: String?,
  val author: String?,
  val description: String?,
  /** The build string the patch was cut against (".asmp" v3); null: not said. */
  val build: String?,
  val segments: List<Segment>,
) {
  /** One area of the map: its place before and after, and the data writes in it. */
  class Segment(
    val oldOffset: Int, val oldSize: Int,
    val newOffset: Int, val newSize: Int,
    val resizeAtEnd: Boolean,
    val changes: List<Change>,
  )

  /** A data write at offset (from its segment's new place). */
  class Change(val offset: Int, val data: ByteArray)

  /**
   * The patch onto a base map: SegmentPatcher's way — the segments by their
   * old places, each grown or shrunk where it says (zeros in, bytes out) and
   * its data written over its new place. The patch's map name must be the
   * base's; anything that writes out of the map refuses, not corrupts.
   */
  fun applyTo(base: ByteArray, baseName: String?): ByteArray {
    if (mapName != null && baseName != null && !mapName.equals(baseName, ignoreCase = true)) {
      throw MapPatchException("the patch is for \"$mapName\", not \"$baseName\"")
    }
    var buf = base.copyOf()
    for (s in segments.sortedBy { it.oldOffset }) {
      if (s.newSize != s.oldSize) {
        val at = if (s.resizeAtEnd) s.newOffset + s.oldSize else s.newOffset
        val grow = s.newSize - s.oldSize
        if (grow > 0) {
          if (at < 0 || at > buf.size) throw MapPatchException("a segment grows out of the map")
          buf = insert(buf, at, grow)
        } else {
          if (at < 0 || at - grow > buf.size) throw MapPatchException("a segment shrinks out of the map")
          buf = remove(buf, at, -grow)
        }
      }
      for (c in s.changes) {
        val at = s.newOffset + c.offset
        if (at < 0 || at.toLong() + c.data.size > buf.size) {
          throw MapPatchException("a change writes past the map's end (at ${"0x%X".format(at)})")
        }
        System.arraycopy(c.data, 0, buf, at, c.data.size)
      }
    }
    return buf
  }

  private fun insert(b: ByteArray, at: Int, n: Int): ByteArray {
    val out = ByteArray(b.size + n)
    System.arraycopy(b, 0, out, 0, at)
    System.arraycopy(b, at, out, at + n, b.size - at)
    return out
  }

  private fun remove(b: ByteArray, at: Int, n: Int): ByteArray {
    val out = ByteArray(b.size - n)
    System.arraycopy(b, 0, out, 0, at)
    System.arraycopy(b, at + n, out, at, b.size - at - n)
    return out
  }

  companion object {
    /** A patch with no container: the bare (offset, size, data) triples of the old tools. */
    fun ofChanges(title: String?, author: String?, description: String?, changes: List<Change>): MapPatch =
      MapPatch(null, title, author, description, null,
        listOf(Segment(0, 0, 0, 0, true, changes)))
  }
}

/**
 * The patch files read. Everything little-endian, every string
 * null-terminated, exactly as Blamite's readers take them.
 */
object MapPatches {
  class PatchFormatException(msg: String) : Exception(msg)

  /**
   * An Assembly patch (.asmp): blocks of a 9-byte header — a four-character
   * name, a size that counts the header, a version — the first block "asmp"
   * holding a "titl" and any number of "segm".
   */
  fun readAsm(b: ByteArray): MapPatch {
    val blocks = Blocks(b, 0, b.size)
    val top = blocks.next() ?: throw PatchFormatException("empty")
    if (top.name != "asmp" || top.version != 0) {
      throw PatchFormatException("not an Assembly patch (${top.name} v${top.version})")
    }
    val inner = blocks.enter(top)
    var title: String? = null
    var author: String? = null
    var description: String? = null
    var output: String? = null
    var mapName: String? = null
    var build: String? = null
    val segments = mutableListOf<MapPatch.Segment>()
    while (true) {
      val blk = inner.next() ?: break
      val r = Reader(b, blk.dataStart, blk.dataStart + blk.dataSize)
      when (blk.name) {
        "titl" -> {
          if (blk.version > 3) throw PatchFormatException("a titl block of version ${blk.version}")
          r.i32() /* the map's numeric ID; the name below is what matters */
          mapName = r.asciiz()
          title = r.utf16()
          description = r.utf16()
          author = r.utf16()
          val shot = r.i32()
          if (shot > 0) r.skip(shot)
          if (blk.version >= 1) {
            if (blk.version >= 3) r.skip(8) else r.skip(4)  /* the meta poke base */
            r.skip(1)                                       /* the meta changes index */
          }
          output = if (blk.version >= 2) r.asciiz() else ""
          if (blk.version == 3) {
            r.skip(1) /* the PC flag */
            build = r.asciiz().ifEmpty { null }
          }
        }
        "segm" -> {
          if (blk.version > 0) throw PatchFormatException("a segm block of version ${blk.version}")
          val n = r.u8()
          repeat(n) {
            val oldOffset = r.u32()
            val oldSize = r.u32()
            val newOffset = r.u32()
            val newSize = r.u32()
            val resizeAtEnd = r.u8() != 0
            val changes = mutableListOf<MapPatch.Change>()
            repeat(r.u32()) {
              changes += MapPatch.Change(r.u32(), r.bytes(4))
            }
            repeat(r.u32()) {
              val at = r.u32()
              val size = r.i32()
              if (size < 0) throw PatchFormatException("a change of negative size")
              changes += MapPatch.Change(at, r.bytes(size))
            }
            segments += MapPatch.Segment(oldOffset, oldSize, newOffset, newSize, resizeAtEnd, changes)
          }
        }
        /* blfc (BLF containers), meta and locl (the deprecated kinds): Halo 2 has none to carry */
      }
    }
    if (segments.isEmpty()) throw PatchFormatException("no segment changes in it")
    return MapPatch(mapName?.ifEmpty { null }, title?.ifEmpty { null }, author, description, build, segments)
  }

  /**
   * An Ascension patch (.patchdat): bare (offset, size, data) triples to the
   * end of the file, nothing about which map.
   */
  fun readAscension(b: ByteArray): MapPatch {
    val changes = mutableListOf<MapPatch.Change>()
    val r = Reader(b, 0, b.size)
    while (r.left() >= 8) {
      val at = r.u32()
      val size = r.i32()
      if (size < 0 || r.left() < size) throw PatchFormatException("a change runs past the file's end")
      changes += MapPatch.Change(at, r.bytes(size))
    }
    if (changes.isEmpty()) throw PatchFormatException("no changes in it")
    return MapPatch.ofChanges("An Ascension patch", null, null, changes)
  }

  /**
   * An Alteration patch (.ascpatch): an author and a description each behind
   * their length byte, then the same triples.
   */
  fun readAlteration(b: ByteArray): MapPatch {
    if (b.isEmpty()) throw PatchFormatException("empty")
    val r = Reader(b, 0, b.size)
    val author = r.ascii(r.u8())
    val description = r.ascii(r.u8())
    val changes = mutableListOf<MapPatch.Change>()
    while (r.left() >= 8) {
      val at = r.u32()
      val size = r.i32()
      if (size < 0 || r.left() < size) throw PatchFormatException("a change runs past the file's end")
      changes += MapPatch.Change(at, r.bytes(size))
    }
    if (changes.isEmpty()) throw PatchFormatException("no changes in it")
    return MapPatch.ofChanges(null, author, description, changes)
  }

  /* ---- the container and its readers ---- */

  private class Block(val name: String, val version: Int, val dataStart: Int, val dataSize: Int)

  /** Blamite's ContainerReader: blocks of [name u32][size i32, header counted][version u8]. */
  private class Blocks(private val b: ByteArray, private var cursor: Int, private val end: Int) {
    fun next(): Block? {
      if (cursor + 9 > end) return null
      val name = String(byteArrayOf(b[cursor], b[cursor + 1], b[cursor + 2], b[cursor + 3]), Charsets.US_ASCII)
      val total = HaloMap.le32(b, cursor + 4)
      val version = b[cursor + 8].toInt() and 0xFF
      val size = total - 9
      if (size < 0 || cursor + 9 + size > end) return null
      val blk = Block(name, version, cursor + 9, size)
      cursor += 9 + size
      return blk
    }

    fun enter(blk: Block) = Blocks(b, blk.dataStart, blk.dataStart + blk.dataSize)
  }

  /** A bounded little-endian reader over the bytes. */
  private class Reader(private val b: ByteArray, private var p: Int, private val end: Int) {
    fun left() = end - p
    fun skip(n: Int) {
      if (n < 0 || p + n > end) throw PatchFormatException("the patch is truncated")
      p += n
    }
    fun u8(): Int {
      if (p + 1 > end) throw PatchFormatException("the patch is truncated")
      return b[p++].toInt() and 0xFF
    }
    fun u32(): Int {
      if (p + 4 > end) throw PatchFormatException("the patch is truncated")
      val v = HaloMap.le32(b, p)
      p += 4
      return v
    }
    fun i32(): Int = u32()
    fun bytes(n: Int): ByteArray {
      if (n < 0 || p + n > end) throw PatchFormatException("the patch is truncated")
      val out = b.copyOfRange(p, p + n)
      p += n
      return out
    }
    fun asciiz(): String {
      val sb = StringBuilder()
      while (p < end) {
        val c = b[p++].toInt() and 0xFF
        if (c == 0) return sb.toString()
        sb.append(c.toChar())
      }
      return sb.toString()
    }
    fun ascii(n: Int): String {
      val out = StringBuilder()
      repeat(n) {
        if (p >= end) return@repeat
        val c = b[p++].toInt() and 0xFF
        if (c != 0) out.append(c.toChar())
      }
      return out.toString()
    }
    fun utf16(): String {
      val sb = StringBuilder()
      while (p + 2 <= end) {
        val c = (b[p].toInt() and 0xFF) or (b[p + 1].toInt() and 0xFF shl 8)
        p += 2
        if (c == 0) return sb.toString()
        sb.append(c.toChar())
      }
      return sb.toString()
    }
  }
}
