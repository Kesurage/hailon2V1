package com.rfandango.haku_x.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater
import kotlin.io.path.createTempFile

/**
 * The map formats against the layouts in docs/mcc-map-research.md: every
 * fixture here is built by the test from those tables, so a wrong offset is
 * a wrong test, and both fail together.
 */
class MapFormatsTest {

  /* ---- fixtures ---- */

  /** An Xbox Halo 2 map header, as the disc's maps have it. */
  private fun xboxMap(build: String = "02.09.27.09809", internal: String = "coagulation"): ByteArray {
    val b = ByteArray(0x400)
    putStr(b, 0, "daeh")
    le32(b, 4, 8)                                   /* the version */
    le32(b, 0x8, 0x20000)                           /* file size */
    le32(b, 0x140, 1)                               /* multiplayer */
    putStr(b, 0x120, build)
    putStr(b, 0x198, internal)
    putStr(b, 0x1BC, "scenarios\\multi\\$internal\\$internal")
    return b
  }

  /** An MCC Update 1 map, compressed the way MCC stores them. */
  private fun mccU1Map(internal: String = "coagulation", plain: ByteArray = ByteArray(0x400)): ByteArray {
    val header = plain.copyOf(0x380)
    putStr(header, 0, "daeh")
    le32(header, 4, 13)                             /* the version */
    le16(header, 0x1C, 1)                           /* flags: compressed */
    putStr(header, 0x90, "02.10.10.0001")
    putStr(header, 0xB0, internal)
    putStr(header, 0xD0, "scenarios\\multi\\$internal\\$internal")

    /* the bytes past the header, in chunks of raw deflate behind a marker,
     * padded to 0x80, and the table after them (Saber's way) */
    val data = plain.drop(0x380).toByteArray()
    val out = ByteArrayOutputStream()
    out.write(header)
    val table = mutableListOf<Pair<Int, Int>>()     /* (size, offset) */
    var at = 0
    while (at < data.size) {
      val n = minOf(0x40000, data.size - at)
      val chunkStart = out.size()
      out.write(0x28); out.write(0x15)              /* the 5416 marker, LE */
      val def = Deflater(1, true)
      def.setInput(data, at, n)
      def.finish()
      val buf = ByteArray(n + 1024)
      val wrote = def.deflate(buf)
      out.write(buf, 0, wrote)
      table += (out.size() - chunkStart) to chunkStart
      while (out.size() % 0x80 != 0) out.write(0)
      at += n
    }
    val tableAt = out.size()
    for ((size, offset) in table) {
      write32(out, size); write32(out, offset)
    }
    val b = out.toByteArray()
    le32(b, 0x308, 0x40000)
    le32(b, 0x30C, 0x380)
    le32(b, 0x310, tableAt)
    le32(b, 0x314, table.size)
    return b
  }

  private fun write32(out: ByteArrayOutputStream, v: Int) {
    out.write(v and 0xFF); out.write(v shr 8 and 0xFF); out.write(v shr 16 and 0xFF); out.write(v shr 24 and 0xFF)
  }

  /* ---- the classifier ---- */

  @Test fun xboxMapIsRead() {
    val info = HaloMap.read(xboxMap())
    assertEquals(HaloMap.Kind.XBOX_H2, info.kind)
    assertEquals(8, info.version)
    assertEquals("02.09.27.09809", info.build)
    assertEquals("coagulation", info.internalName)
    assertEquals("multiplayer", info.typeText)
  }

  @Test fun theShippedMpsOwnBuildIsRead() {
    assertEquals(HaloMap.Kind.XBOX_H2, HaloMap.read(xboxMap(build = "02.08.28.09214")).kind)
  }

  @Test fun vistaIsItsOwn() {
    val info = HaloMap.read(xboxMap(build = "11081.07.04.30.0934.main"))
    assertEquals(HaloMap.Kind.VISTA_H2, info.kind)
    assertEquals("coagulation", info.internalName)
  }

  @Test fun anotherHaloIsItsOwn() {
    val b = xboxMap(build = "01.09.05.2276")
    le32(b, 4, 5)
    assertEquals(HaloMap.Kind.OTHER_HALO, HaloMap.read(b).kind)
  }

  @Test fun notAMapIsNot() {
    assertEquals(HaloMap.Kind.UNKNOWN, HaloMap.read(ByteArray(0x400)).kind)
    assertEquals(HaloMap.Kind.UNKNOWN, HaloMap.read(xboxMap().copyOf(0x100)).kind)
  }

  @Test fun mccU1IsReadCompressed() {
    val plain = xboxMap().let { it + ByteArray(0x1000) }
    val b = mccU1Map(plain = plain)
    val info = HaloMap.read(b)
    assertEquals(HaloMap.Kind.MCC_H2C, info.kind)
    assertEquals(13, info.version)
    assertEquals(true, info.mccU1)
    assertEquals(true, info.mccCompressed)
    assertEquals("coagulation", info.internalName)  /* read out of the uncompressed header */
  }

  @Test fun saberBringsAnMccMapBack() {
    val plain = xboxMap() + ByteArray(0x1000) { (it % 251).toByte() }
    val back = Saber.decompress(mccU1Map(plain = plain), true)
    /* the stored header carries the MCC's own version and names; the whole
     * of the map past it is the original's bytes */
    assertEquals("the same length back", plain.size, back.size)
    assertTrue("the map's bytes come back whole",
      back.copyOfRange(0x380, back.size).contentEquals(plain.copyOfRange(0x380, plain.size)))
    assertEquals("the MCC version it was stored with", 13, HaloMap.le32(back, 4))
    assertEquals("the compressed bit is cleared", 0, back[0x1C].toInt())
  }

  @Test fun saberTakesAFauxChunk() {
    /* a chunk stored raw, its size negated (the other tools' way) */
    val plain = xboxMap() + ByteArray(0x100) { (it % 7).toByte() }
    val stored = ByteArray(plain.size + 8)
    plain.copyInto(stored)
    le32(stored, 0x308, 0x40000)
    le32(stored, 0x30C, 0x380)
    val tableAt = plain.size
    le32(stored, 0x310, tableAt)
    le32(stored, 0x314, 1)
    le32(stored, tableAt, -(plain.size - 0x380))
    le32(stored, tableAt + 4, 0x380)
    val back = Saber.decompress(stored, true)
    assertEquals("the same length", plain.size, back.size)
    /* the stored header carries the compression fields; the chunk's own
     * bytes and the map's first 0x300 are what comes back */
    assertTrue("the chunk's bytes come back whole",
      back.copyOfRange(0x380, back.size).contentEquals(plain.copyOfRange(0x380, plain.size)))
    assertTrue("the header is the map's own",
      back.copyOfRange(0, 0x300).contentEquals(plain.copyOfRange(0, 0x300)))
  }

  @Test fun aBrokenChunkRefuses() {
    val b = mccU1Map()
    le32(b, 0x310, b.size + 0x1000)                /* the table out of the file */
    try {
      Saber.decompress(b, true)
      fail("a table past the file's end refuses")
    } catch (_: Saber.MapFormatException) {
    }
  }

  /* ---- the Assembly patch ---- */

  /** A .asmp written as the test needs it (the loader's way, backwards). */
  private fun asmp(mapName: String, title: String, author: String, segment: MapPatch.Segment): ByteArray {
    fun block(name: String, version: Int, body: ByteArrayOutputStream): ByteArray {
      val out = ByteArrayOutputStream()
      for (c in name) out.write(c.code)
      write32(out, body.size() + 9)
      out.write(version)
      body.writeTo(out)
      return out.toByteArray()
    }
    val titl = ByteArrayOutputStream()
    write32(titl, 0x4C2)                            /* a map ID */
    for (c in mapName) titl.write(c.code)
    titl.write(0)
    fun utf16(s: String) {
      for (c in s) {
        titl.write(c.code and 0xFF); titl.write(c.code shr 8)
      }
      titl.write(0); titl.write(0)
    }
    utf16(title); utf16("the test's"); utf16(author)
    write32(titl, 0)                                /* no screenshot */
    /* v1: the poke base (8 bytes at v3) and index; v2: the output name; v3: the PC flag and build */
    write32(titl, 0); write32(titl, 0)
    titl.write(0)                                   /* the meta changes index */
    titl.write(0)                                   /* an empty output name */
    titl.write(0)                                   /* not a PC patch */
    for (c in "02.09.27.09809") titl.write(c.code)
    titl.write(0)

    val segm = ByteArrayOutputStream()
    segm.write(1)
    write32(segm, segment.oldOffset); write32(segm, segment.oldSize)
    write32(segm, segment.newOffset); write32(segm, segment.newSize)
    segm.write(if (segment.resizeAtEnd) 1 else 0)
    val four = segment.changes.filter { it.data.size == 4 }
    write32(segm, four.size)
    for (c in four) {
      write32(segm, c.offset); segm.write(c.data)
    }
    val other = segment.changes.filter { it.data.size != 4 }
    write32(segm, other.size)
    for (c in other) {
      write32(segm, c.offset); write32(segm, c.data.size); segm.write(c.data)
    }

    val inner = ByteArrayOutputStream()
    inner.write(block("titl", 3, titl))
    inner.write(block("segm", 0, segm))
    return block("asmp", 0, inner)
  }

  @Test fun anAsmpIsParsedAndApplied() {
    val seg = MapPatch.Segment(0x100, 0x40, 0x100, 0x40, false, listOf(
      MapPatch.Change(0x8, byteArrayOf(1, 2, 3, 4)),
      MapPatch.Change(0x10, byteArrayOf(9, 9, 9)),
    ))
    val patch = MapPatches.readAsm(asmp("coagulation", "Gravity hammers", "the test", seg))
    assertEquals("coagulation", patch.mapName)
    assertEquals("Gravity hammers", patch.title)
    assertEquals("the test", patch.author)
    assertEquals("02.09.27.09809", patch.build)

    val base = xboxMap() + ByteArray(0x1000)
    val out = patch.applyTo(base, "coagulation")
    assertEquals("the same size when nothing grows", base.size, out.size)
    assertEquals(1, out[0x108].toInt())
    assertEquals(9, out[0x110].toInt())
  }

  @Test fun anAsmpThatGrowsASegmentShiftsTheTail() {
    val seg = MapPatch.Segment(0x200, 0x10, 0x200, 0x20, false, listOf(
      MapPatch.Change(0, byteArrayOf(1, 2, 3, 4)),
    ))
    val patch = MapPatches.readAsm(asmp("coagulation", "Grown", "the test", seg))
    val base = xboxMap() + ByteArray(0x100) { 0x55 }
    val out = patch.applyTo(base, "coagulation")
    assertEquals("grown by 0x10", base.size + 0x10, out.size)
    assertEquals("the write lands at the segment's new place", 1, out[0x200].toInt())
    assertEquals("the tail moved", 0x55, out[0x400 + 0x10].toInt())
  }

  @Test fun aPatchForAnotherMapRefuses() {
    val seg = MapPatch.Segment(0, 4, 0, 4, false, listOf(MapPatch.Change(0, byteArrayOf(1, 1, 1, 1))))
    val patch = MapPatches.readAsm(asmp("zanzibar", "Wrong", "the test", seg))
    try {
      patch.applyTo(xboxMap(), "coagulation")
      fail("a patch for another map refuses")
    } catch (e: MapPatchException) {
      assertTrue(e.message!!.contains("zanzibar"))
    }
  }

  @Test fun aChangePastTheEndRefuses() {
    val seg = MapPatch.Segment(0, 4, 0, 4, false, listOf(MapPatch.Change(0x100000, byteArrayOf(1, 1, 1, 1))))
    val patch = MapPatches.readAsm(asmp("coagulation", "Out", "the test", seg))
    try {
      patch.applyTo(xboxMap() + ByteArray(0x100), "coagulation")
      fail("a change out of the map refuses")
    } catch (_: MapPatchException) {
    }
  }

  @Test fun aTruncatedPatchRefuses() {
    val seg = MapPatch.Segment(0, 4, 0, 4, false, listOf(MapPatch.Change(0, byteArrayOf(1, 1, 1, 1))))
    val whole = asmp("coagulation", "Cut", "the test", seg)
    try {
      MapPatches.readAsm(whole.copyOf(whole.size / 2))
      fail("half a patch refuses")
    } catch (_: MapPatches.PatchFormatException) {
    }
  }

  /* ---- the older tools' patches ---- */

  @Test fun anAscensionPatchIsApplied() {
    val base = xboxMap() + ByteArray(0x100)
    val out = ByteArrayOutputStream()
    write32(out, 0x120); write32(out, 4); out.write(7); out.write(7); out.write(7); out.write(7)
    write32(out, 0x140); write32(out, 2); out.write(3); out.write(3)
    val patch = MapPatches.readAscension(out.toByteArray())
    assertNull("an old patch names no map", patch.mapName)
    val done = patch.applyTo(base, null)
    assertEquals(7, done[0x120].toInt())
    assertEquals(3, done[0x140].toInt())
  }

  @Test fun anAlterationPatchCarriesItsAuthor() {
    val out = ByteArrayOutputStream()
    out.write(4); out.write('m'.code); out.write('o'.code); out.write('d'.code); out.write('d'.code)
    out.write(2); out.write('h'.code); out.write('i'.code)
    write32(out, 0x100); write32(out, 1); out.write(9)
    val patch = MapPatches.readAlteration(out.toByteArray())
    assertEquals("modd", patch.author)
    val done = patch.applyTo(xboxMap() + ByteArray(0x100), null)
    assertEquals(9, done[0x100].toInt())
  }

  /* ---- the disc filesystem ---- */

  /** An XISO with a maps folder holding one map, in a temp file. */
  private fun disc(mapName: String, mapBytes: ByteArray): Pair<File, Long> {
    val sector = 2048
    val rootSector = 20
    val mapsSector = 21
    /* past the volume descriptor (0x10000, sector 32) — the map would sit on it */
    val mapSector = 40
    val size = mapSector * sector + mapBytes.size + 4 * sector
    val f = createTempFile("disc", ".iso").toFile()
    RandomAccessFile(f, "rw").use { raf ->
      val img = ByteArray(size)
      fun dirent(at: Int, sectorLba: Int, entrySize: Int, flags: Int, name: String) {
        val b = img
        b[at] = 0xFF.toByte(); b[at + 1] = 0xFF.toByte()      /* no left */
        b[at + 2] = 0xFF.toByte(); b[at + 3] = 0xFF.toByte()  /* no right */
        le32(b, at + 4, sectorLba)
        le32(b, at + 8, entrySize)
        b[at + 12] = flags.toByte()
        b[at + 13] = name.length.toByte()
        for (i in name.indices) b[at + 14 + i] = name[i].code.toByte()
      }
      /* the volume descriptor: the magic, the root's sector and size */
      putStr(img, 0x10000, "MICROSOFT*XBOX*MEDIA")
      le32(img, 0x10000 + 20, rootSector)
      le32(img, 0x10000 + 24, 4 * 2048)
      /* the root: one folder, maps */
      dirent(rootSector * sector, mapsSector, 2048, 0x10, "maps")
      /* maps: one file */
      dirent(mapsSector * sector, mapSector, mapBytes.size, 0, mapName)
      mapBytes.copyInto(img, mapSector * sector)
      raf.write(img)
    }
    return f to 0L
  }

  @Test fun theDiscsFilesRead() {
    val map = xboxMap() + ByteArray(0x2000)
    val (f, partition) = disc("coagulation.map", map)
    RandomAccessFile(f, "r").use { raf ->
      val reader = Xdvdfs.Reader(raf.channel, partition)
      val maps = reader.listDir("maps")
      assertNotNull(maps)
      assertEquals(1, maps!!.size)
      assertEquals("coagulation.map", maps[0].name)
      assertTrue(!maps[0].isDir)
      val back = reader.readFile("maps/coagulation.map")
      assertTrue("the map's bytes come back whole", back!!.contentEquals(map))
      assertNull("a missing file is null", reader.readFile("maps/nothere.map"))
      assertNull("a missing folder is null", reader.listDir("sound"))
    }
    f.delete()
  }

  /* ---- little-endian writes ---- */

  private fun le16(b: ByteArray, at: Int, v: Int) {
    b[at] = (v and 0xFF).toByte(); b[at + 1] = (v shr 8 and 0xFF).toByte()
  }

  private fun le32(b: ByteArray, at: Int, v: Int) {
    b[at] = (v and 0xFF).toByte(); b[at + 1] = (v shr 8 and 0xFF).toByte()
    b[at + 2] = (v shr 16 and 0xFF).toByte(); b[at + 3] = (v shr 24 and 0xFF).toByte()
  }

  private fun putStr(b: ByteArray, at: Int, s: String) {
    for (i in s.indices) b[at + i] = s[i].code.toByte()
  }
}
