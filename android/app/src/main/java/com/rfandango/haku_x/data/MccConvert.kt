package com.rfandango.haku_x.data

/**
 * An MCC (Master Chief Collection) Halo 2 classic map turned into the disc's
 * own Xbox format — the converter docs/mcc-map-research.md planned. The way
 * that needs no tag-walking: a map's addresses hang off one base (an address
 * is base + its file offset), which is observable from its own index header —
 * the tag group table sits right past that header, so its stored address and
 * its file offset fix the base. The collection kept the original address
 * space (its runtime holds a magic to convert these addresses to its own
 * heap, per Assembly's poking tables), so when a modded MCC map's base
 * equals the disc map's, its body can be kept whole, at its own offsets,
 * behind a header written the disc's way. The disc's own map calibrates the
 * check — nothing is assumed that the disc does not say.
 */
object MccConvert {
  class ConvertException(msg: String) : Exception(msg)

  /** A second-generation map's header, the fields the converter walks. */
  private class Head(
    val metaOffset: Int,       /* where the index header (its "tags" magic) sits */
    val headSize: Int,         /* the index area's size, as the header claims */
    val tagDataSize: Int,
    val mask: Long,            /* the tag-data area's address base; 0: none stored (Xbox) */
    val type: Int,
    val internalName: String,
    val scenarioName: String,
    val stringBlockOffset: Int, val stringCount: Int, val stringDataSize: Int,
    val stringIndexTableOffset: Int, val stringDataOffset: Int,
    val fileCount: Int, val fileDataOffset: Int, val fileDataSize: Int, val fileIndexTableOffset: Int,
    val rawTableOffset: Int, val rawTableSize: Int,
    val localeGlobalsOffset: Int, val localeGlobalsSize: Int,
    val buildDateHigh: Int, val buildDateLow: Int,
    val crc: Int, val checksum: Int,
    val lowDetailCount: Int, val lowDetailOffset: Int, val lowDetailSize: Int,
    val build: String,
  )

  /** An index header and the tables behind it, walked out of a map. */
  private class Index(
    val groupTableAddress: Long, val groupCount: Int,
    val tagTableAddress: Long, val tagCount: Int,
    /** Every tag: its data's address, in table order (all that is read of each). */
    val addresses: List<Long>,
  ) {
    /** The whole map's address base: an address is base + its file offset. */
    val base: Long get() = groupTableAddress - 0x20
    /** The index area's real size: its header, the group table, the tag table. */
    val headSize: Int get() = 0x20 + groupCount * 0xC + tagCount * 0x10
  }

  private const val TAGS = 0x73676174L /* "tags" as the file stores it, little-endian */

  /* ---- the headers read ---- */

  private fun xboxHead(b: ByteArray): Head {
    if (b.size < 0x800) throw ConvertException("the disc's map is smaller than its own header")
    return Head(
      metaOffset = le32(b, 0x10), headSize = le32(b, 0x14),
      tagDataSize = le32(b, 0x18), mask = 0L,
      type = le32(b, 0x140),
      internalName = HaloMap.asciiz(b, 0x198, 0x40), scenarioName = HaloMap.asciiz(b, 0x1BC, 0x40),
      stringBlockOffset = le32(b, 0x160), stringCount = le32(b, 0x164), stringDataSize = le32(b, 0x168),
      stringIndexTableOffset = le32(b, 0x16C), stringDataOffset = le32(b, 0x170),
      fileCount = le32(b, 0x2C0), fileDataOffset = le32(b, 0x2C4), fileDataSize = le32(b, 0x2C8),
      fileIndexTableOffset = le32(b, 0x2CC),
      rawTableOffset = -1, rawTableSize = 0,
      localeGlobalsOffset = -1, localeGlobalsSize = 0,
      buildDateHigh = le32(b, 0x178), buildDateLow = le32(b, 0x17C),
      crc = le32(b, 0x144), checksum = le32(b, 0x2D0),
      lowDetailCount = le32(b, 0x154), lowDetailOffset = le32(b, 0x158), lowDetailSize = le32(b, 0x15C),
      build = HaloMap.asciiz(b, 0x120, 0x40),
    )
  }

  private fun mccHead(b: ByteArray, u1: Boolean): Head {
    if (u1) {
      if (b.size < 0x380) throw ConvertException("the map is smaller than its own header")
      return Head(
        metaOffset = le32(b, 0x10), headSize = le32(b, 0x2D4),
        tagDataSize = le32(b, 0x2D8), mask = le32(b, 0x2D0).toLong() and 0xFFFFFFFFL,
        type = le16(b, 0x18),
        internalName = HaloMap.asciiz(b, 0xB0, 0x40), scenarioName = HaloMap.asciiz(b, 0xD0, 0x40),
        stringBlockOffset = -1, stringCount = le32(b, 0x30), stringDataSize = le32(b, 0x38),
        stringIndexTableOffset = le32(b, 0x3C), stringDataOffset = le32(b, 0x34),
        fileCount = le32(b, 0x20), fileDataOffset = le32(b, 0x24), fileDataSize = le32(b, 0x28),
        fileIndexTableOffset = le32(b, 0x2C),
        rawTableOffset = le32(b, 0x2F0), rawTableSize = le32(b, 0x2F4),
        localeGlobalsOffset = le32(b, 0x2E4), localeGlobalsSize = le32(b, 0x2E8),
        buildDateHigh = le32(b, 0x48), buildDateLow = le32(b, 0x4C),
        crc = 0, checksum = le32(b, 0x2F8),
        lowDetailCount = 0, lowDetailOffset = 0, lowDetailSize = 0,
        build = HaloMap.asciiz(b, 0x90, 0x40),
      )
    }
    if (b.size < 0x1000) throw ConvertException("the map is smaller than its own header")
    return Head(
      metaOffset = le32(b, 0xC), headSize = le32(b, 0x10),
      tagDataSize = le32(b, 0x14), mask = le32(b, 0x1C).toLong() and 0xFFFFFFFFL,
      type = le32(b, 0x148),
      internalName = HaloMap.asciiz(b, 0x1C0, 0x40), scenarioName = HaloMap.asciiz(b, 0x1E4, 0x40),
      stringBlockOffset = le32(b, 0x164), stringCount = le32(b, 0x168), stringDataSize = le32(b, 0x16C),
      stringIndexTableOffset = le32(b, 0x170), stringDataOffset = le32(b, 0x174),
      fileCount = le32(b, 0x2E8), fileDataOffset = le32(b, 0x2EC), fileDataSize = le32(b, 0x2F0),
      fileIndexTableOffset = le32(b, 0x2F4),
      rawTableOffset = le32(b, 0x304), rawTableSize = le32(b, 0x308),
      localeGlobalsOffset = le32(b, 0x2F8), localeGlobalsSize = le32(b, 0x2FC),
      buildDateHigh = le32(b, 0x178), buildDateLow = le32(b, 0x17C),
      crc = le32(b, 0x14C), checksum = le32(b, 0x30C),
      lowDetailCount = le32(b, 0x158), lowDetailOffset = le32(b, 0x15C), lowDetailSize = le32(b, 0x160),
      build = HaloMap.asciiz(b, 0x128, 0x40),
    )
  }

  /* ---- the index header and tables walked ---- */

  /**
   * A map's index header at metaOffset, its group table right past it and
   * its tag table past that, both checked against the addresses the header
   * stores for them (the one-base layout every Halo 2 map follows).
   */
  private fun index(b: ByteArray, metaOffset: Int): Index {
    if (metaOffset < 0 || metaOffset + 0x20 > b.size) throw ConvertException("its index header is out of the file")
    if (le32(b, metaOffset + 0x1C).toLong() and 0xFFFFFFFFL != TAGS) {
      throw ConvertException("no \"tags\" magic at its index header — not a whole map?")
    }
    val groupTableAddress = le32(b, metaOffset).toLong() and 0xFFFFFFFFL
    val groupCount = le32(b, metaOffset + 4)
    val tagTableAddress = le32(b, metaOffset + 8).toLong() and 0xFFFFFFFFL
    val tagCount = le32(b, metaOffset + 0x18)
    if (groupCount <= 0 || groupCount > 4096) throw ConvertException("its tag group count ($groupCount) is not sane")
    if (tagCount <= 0 || tagCount > 200000) throw ConvertException("its tag count ($tagCount) is not sane")
    /* the tables sit right past the index header, and the addresses the
     * header stores for them must say exactly that */
    val tagTableAt = metaOffset + 0x20 + groupCount * 0xC
    if (tagTableAddress - (groupTableAddress - 0x20) != 0x20L + groupCount * 0xC) {
      throw ConvertException("its tables sit somewhere other than its index header says")
    }
    if (tagTableAt + tagCount * 0x10 > b.size) throw ConvertException("its tag table runs out of the file")
    val addresses = ArrayList<Long>(tagCount)
    for (i in 0 until tagCount) {
      val address = le32(b, tagTableAt + i * 0x10 + 8).toLong() and 0xFFFFFFFFL
      if (address == 0L) throw ConvertException("its tag table holds a nothing (tag $i)")
      addresses += address
    }
    return Index(groupTableAddress, groupCount, tagTableAddress, tagCount, addresses)
  }

  /** A map's tags, each checked to its own file under one base. */
  private fun checkOneBase(b: ByteArray, metaOffset: Int, ix: Index): Long {
    val base = ix.base - metaOffset
    val indexEnd = metaOffset + ix.headSize
    for (i in ix.addresses.indices) {
      val at = ix.addresses[i] - base
      if (at < indexEnd || at >= b.size) {
        throw ConvertException("tag $i's address does not land in this map under one base")
      }
    }
    return base
  }

  /* ---- the conversion ---- */

  /**
   * The modded MCC map (decompressed) behind the disc's own map of the same
   * name: the body kept whole at its own offsets — its addresses are only
   * the disc's to honor if the two maps were built onto the same base, which
   * both maps' own tables are read to check — and an Xbox header written
   * over the front, the disc's build and the collection's own sizes. The
   * index area alone may move (the collection's current maps start it right
   * after a 0x380 header, which the disc's 0x800 header would sit on). Any
   * check that refuses leaves with its reason; nothing half-converted is
   * returned.
   */
  fun convert(mcc: ByteArray, u1: Boolean, disc: ByteArray): ByteArray {
    val m = try {
      mccHead(mcc, u1)
    } catch (e: ConvertException) {
      throw ConvertException("the MCC map: ${e.message}")
    }
    val d = try {
      xboxHead(disc)
    } catch (e: ConvertException) {
      throw ConvertException("the disc's map: ${e.message}")
    }
    if (m.internalName.isEmpty() || !m.internalName.equals(d.internalName, ignoreCase = true)) {
      throw ConvertException("the MCC map is \"${m.internalName}\", the disc's is \"${d.internalName}\"")
    }
    val mi = try {
      index(mcc, m.metaOffset)
    } catch (e: ConvertException) {
      throw ConvertException("the MCC map: ${e.message}")
    }
    val di = try {
      index(disc, d.metaOffset)
    } catch (e: ConvertException) {
      throw ConvertException("the disc's map: ${e.message}")
    }
    val mBase = checkOneBase(mcc, m.metaOffset, mi)
    val dBase = checkOneBase(disc, d.metaOffset, di)
    if (mBase != dBase) {
      throw ConvertException(
        "the MCC map's address base (0x%08X) is not the disc's (0x%08X) — this one cannot be carried over whole"
          .format(mBase, dBase))
    }
    /* the MCC map's stored mask is the address its tag data starts at; under
     * the base both maps share, that must land inside the map, past its
     * tables — a mask that disagrees says the map is not what it seems */
    val mTagDataStart = (m.mask - mBase).toInt()
    if (m.mask != 0L && (mTagDataStart < m.metaOffset + mi.headSize || mTagDataStart >= mcc.size)) {
      throw ConvertException("its address base and its stored mask disagree")
    }
    if (m.tagDataSize <= 0 || mTagDataStart + m.tagDataSize > mcc.size + 0x1000) {
      throw ConvertException("its tag data area runs past the file")
    }

    /* the areas that must survive clear of the Xbox header written over
     * [0, 0x800): every one the MCC header names, the index area, and the
     * tag data */
    val xheader = 0x800
    fun under(s: String, at: Int) = if (at in 1 until xheader) {
      throw ConvertException("its $s starts at 0x${at.toString(16)}, inside the Xbox header's own room")
    } else Unit
    under("tag data", mTagDataStart)
    if (m.rawTableSize > 0) under("raw table", m.rawTableOffset)
    if (m.localeGlobalsSize > 0) under("locale globals", m.localeGlobalsOffset)
    under("string index table", m.stringIndexTableOffset)
    under("string data", m.stringDataOffset)
    under("string block", m.stringBlockOffset)
    under("file table", m.fileDataOffset)
    under("file index table", m.fileIndexTableOffset)

    /* the body kept whole; the index area alone may need to move */
    var out = mcc.copyOf()
    var metaOffset = m.metaOffset
    if (m.metaOffset in 1 until xheader) {
      val size = (mi.headSize + 0xFF) and 0xFF.inv()
      metaOffset = (out.size + 0x1FF) and 0x1FF.inv()
      if (metaOffset + size > out.size) out = out.copyOf(metaOffset + size)
      /* the index header and its tables, moved; the two addresses in the
       * header point at the tables, so they are written for the new place */
      System.arraycopy(mcc, m.metaOffset, out, metaOffset, mi.headSize)
      le32(out, metaOffset, (mBase + metaOffset + 0x20))
      le32(out, metaOffset + 8, (mBase + metaOffset + 0x20 + mi.groupCount * 0xC))
      /* the old index area under the Xbox header is dead; zeroed, not left */
      val deadEnd = minOf(m.metaOffset + mi.headSize, xheader)
      if (m.metaOffset < deadEnd) {
        java.util.Arrays.fill(out, m.metaOffset, deadEnd, 0.toByte())
      }
    }

    /* the Xbox header: the disc's build (the engine reads it), the
     * collection's own sizes, names and tables, all at the Xbox offsets */
    le32(out, 0x8, out.size)                       /* file size */
    le32(out, 0x10, metaOffset)
    le32(out, 0x14, mi.headSize)                   /* the index area's real size */
    le32(out, 0x18, m.tagDataSize)
    le32(out, 0x1C, mi.headSize + m.tagDataSize)   /* the meta area's whole size */
    le32(out, 0x140, m.type)
    le32(out, 0x144, d.crc)
    put(out, 0x120, d.build)
    put(out, 0x198, m.internalName)
    put(out, 0x1BC, m.scenarioName)
    le32(out, 0x154, m.lowDetailCount); le32(out, 0x158, m.lowDetailOffset); le32(out, 0x15C, m.lowDetailSize)
    le32(out, 0x164, m.stringCount)
    le32(out, 0x168, m.stringDataSize)
    le32(out, 0x16C, m.stringIndexTableOffset)
    le32(out, 0x170, m.stringDataOffset)
    if (m.stringBlockOffset > 0) {
      le32(out, 0x160, m.stringBlockOffset)        /* its own string block */
    } else {
      /* the current collection headers carry no string block; the disc's
       * own set list stands in (the sets are the engine's, not the map's) */
      le32(out, 0x160, d.stringBlockOffset)
    }
    le32(out, 0x178, m.buildDateHigh); le32(out, 0x17C, m.buildDateLow)
    le32(out, 0x2C0, m.fileCount)
    le32(out, 0x2C4, m.fileDataOffset)
    le32(out, 0x2C8, m.fileDataSize)
    le32(out, 0x2CC, m.fileIndexTableOffset)
    le32(out, 0x2D0, m.checksum)
    /* the magic and version an Xbox map opens with */
    put(out, 0, "daeh")
    le32(out, 4, 8)

    /* the result checked as a whole: it must read back as the disc's own
     * build, say the same map's name, and hold the very tags it was given,
     * at the very addresses — a conversion that moved anything is no good */
    val back = HaloMap.read(out)
    if (back.kind != HaloMap.Kind.XBOX_H2 || !back.build.startsWith("02.")) {
      throw ConvertException("the converted map does not read back as the disc's build (${back.kind} ${back.build})")
    }
    if (!back.internalName.equals(d.internalName, ignoreCase = true)) {
      throw ConvertException("the converted map lost its name (\"${back.internalName}\")")
    }
    val oi = try {
      index(out, metaOffset)
    } catch (e: ConvertException) {
      throw ConvertException("the converted map: ${e.message}")
    }
    if (oi.tagCount != mi.tagCount || oi.groupCount != mi.groupCount) {
      throw ConvertException("the converted map's tables came back different")
    }
    for (i in mi.addresses.indices) {
      if (oi.addresses[i] != mi.addresses[i]) {
        throw ConvertException("the converted map's tag $i moved — its addresses must not")
      }
    }
    return out
  }

  /* ---- little helpers ---- */

  private fun le16(b: ByteArray, at: Int): Int = HaloMap.le16(b, at)
  private fun le32(b: ByteArray, at: Int): Int = HaloMap.le32(b, at)
  private fun le32(b: ByteArray, at: Int, v: Long) {
    val i = v.toInt()
    b[at] = (i and 0xFF).toByte(); b[at + 1] = (i shr 8 and 0xFF).toByte()
    b[at + 2] = (i shr 16 and 0xFF).toByte(); b[at + 3] = (i shr 24 and 0xFF).toByte()
  }
  private fun le32(b: ByteArray, at: Int, v: Int) = le32(b, at, v.toLong())

  private fun put(b: ByteArray, at: Int, s: String) {
    for (i in 0 until s.length.coerceAtMost(0x40)) b[at + i] = s[i].code.toByte()
    if (at + s.length < b.size) b[at + s.length] = 0
  }
}
