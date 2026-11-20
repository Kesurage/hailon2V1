package com.rfandango.haku_x.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The MCC→Xbox conversion against synthetic pairs: a disc map and a
 * collection map of the same name, built the way the format tables in
 * docs/mcc-map-research.md say — one address base each, the group table
 * right past the index header, the tag table past that, and every tag's
 * address at base + its file offset. What the converter is checked to keep:
 * the body whole, the addresses unmoved, and the disc's own build up front.
 */
class MccConvertTest {

  private val K = 0x9C000000L   /* the shared address base both maps are built onto */
  private val GROUPS = 3
  private val TAGS = 8
  private val HEAD = 0x20 + GROUPS * 0xC + TAGS * 0x10  /* 0xC4 */

  private fun le32(b: ByteArray, at: Int, v: Int) {
    b[at] = (v and 0xFF).toByte(); b[at + 1] = (v shr 8 and 0xFF).toByte()
    b[at + 2] = (v shr 16 and 0xFF).toByte(); b[at + 3] = (v shr 24 and 0xFF).toByte()
  }

  private fun le32(b: ByteArray, at: Int, v: Long) = le32(b, at, v.toInt())

  private fun put(b: ByteArray, at: Int, s: String) {
    for (i in s.indices) b[at + i] = s[i].code.toByte()
    b[at + s.length] = 0
  }

  /** The index header, group table and tag table at metaOffset, under base. */
  private fun writeIndex(b: ByteArray, metaOffset: Int, base: Long, tagDataStart: Int,
                         groups: Int = GROUPS, tags: Int = TAGS) {
    le32(b, metaOffset, base + metaOffset + 0x20)              /* group table address */
    le32(b, metaOffset + 4, groups)
    le32(b, metaOffset + 8, base + metaOffset + 0x20 + groups * 0xC)  /* tag table address */
    le32(b, metaOffset + 0xC, 0x11)                            /* scenario datum */
    le32(b, metaOffset + 0x10, 0x22)                           /* matg datum */
    le32(b, metaOffset + 0x18, tags)
    le32(b, metaOffset + 0x1C, 0x73676174)                     /* "tags" */
    for (g in 0 until groups) {
      val at = metaOffset + 0x20 + g * 0xC
      le32(b, at, 0x100 + g)                                   /* the group's magic */
      le32(b, at + 4, 0)
      le32(b, at + 8, 0)
    }
    for (t in 0 until tags) {
      val at = metaOffset + 0x20 + groups * 0xC + t * 0x10
      val dataAt = tagDataStart + t * 0x100
      le32(b, at, 0x100 + t % groups)                          /* the tag's group */
      le32(b, at + 4, 0x8000 + t)                              /* its datum index */
      le32(b, at + 8, base + dataAt)                           /* its data's address */
      le32(b, at + 0xC, 0x100)                                 /* its data's size */
    }
  }

  /** The disc's own map: Xbox header at 0, the index area at 0x1000. */
  private fun discMap(name: String = "coagulation", base: Long = K): ByteArray {
    val metaOffset = 0x1000
    val tagDataStart = metaOffset + HEAD
    val b = ByteArray(tagDataStart + TAGS * 0x100 + 0x400)
    put(b, 0, "daeh"); le32(b, 4, 8); le32(b, 8, b.size)
    le32(b, 0x10, metaOffset); le32(b, 0x14, HEAD); le32(b, 0x18, TAGS * 0x100)
    le32(b, 0x1C, HEAD + TAGS * 0x100); le32(b, 0x140, 1)
    put(b, 0x120, "02.09.27.09809")
    put(b, 0x198, name); put(b, 0x1BC, "scenarios\\multi\\x\\x")
    le32(b, 0x160, 0x1800)                                     /* the string block */
    le32(b, 0x164, 4); le32(b, 0x168, 0x40)
    le32(b, 0x16C, 0x1840); le32(b, 0x170, 0x1850)
    le32(b, 0x2C0, TAGS); le32(b, 0x2C4, 0x1900); le32(b, 0x2C8, 0x100); le32(b, 0x2CC, 0x1A00)
    writeIndex(b, metaOffset, base, tagDataStart)
    for (t in 0 until TAGS) {                                  /* tag data with a pattern */
      val at = tagDataStart + t * 0x100
      for (i in 0 until 0x100) b[at + i] = (t * 7 + i).toByte()
    }
    return b
  }

  /** A collection map: Update 1's 0x380 header (or the old 0x1000 one), the
   *  index area right past it, the same base the disc's map is built onto. */
  private fun mccMap(u1: Boolean, name: String = "coagulation", base: Long = K, modByte: Int = 0x5A,
                     groups: Int = GROUPS, tags: Int = TAGS): ByteArray {
    val headerSize = if (u1) 0x380 else 0x1000
    val metaOffset = headerSize
    val head = 0x20 + groups * 0xC + tags * 0x10
    val tagDataStart = metaOffset + head
    val stringsAt = 0x2000
    val b = ByteArray(tagDataStart + tags * 0x100 + 0x1000)
    put(b, 0, "daeh"); le32(b, 4, 13); le32(b, 8, b.size)
    le32(b, 0x144, 0)
    put(b, 0x198, ""); put(b, 0x1BC, "")                        /* not the Xbox fields */
    if (u1) {
      le32(b, 0x10, metaOffset); le32(b, 0x14, head + tags * 0x100)
      le32(b, 0x18, 1); le32(b, 0x1C, 0)                        /* type, flags: plain */
      le32(b, 0x20, tags); le32(b, 0x24, stringsAt + 0x100); le32(b, 0x28, 0x100); le32(b, 0x2C, stringsAt + 0x200)
      le32(b, 0x30, 4); le32(b, 0x34, stringsAt + 0x20); le32(b, 0x38, 0x40); le32(b, 0x3C, stringsAt)
      le32(b, 0x48, 0x1AF5); le32(b, 0x4C, 0xBEEF)              /* a build date */
      put(b, 0x90, "02.10.10.0001")
      put(b, 0xB0, name); put(b, 0xD0, "scenarios\\multi\\x\\x")
      le32(b, 0x2D0, base + tagDataStart)                       /* the mask: the tag data's address */
      le32(b, 0x2D4, head); le32(b, 0x2D8, tags * 0x100)
      le32(b, 0x2F0, 0); le32(b, 0x2F4, 0)                      /* no raw table */
      le32(b, 0x2E4, 0); le32(b, 0x2E8, 0)                      /* no locale globals */
      le32(b, 0x2F8, 0x12345678)                                /* checksum */
    } else {
      le32(b, 0xC, metaOffset); le32(b, 0x10, head); le32(b, 0x14, tags * 0x100)
      le32(b, 0x18, head + tags * 0x100); le32(b, 0x1C, base + tagDataStart)
      le32(b, 0x148, 1); le32(b, 0x14C, 0)
      put(b, 0x128, "02.10.10.0001")
      put(b, 0x1C0, name); put(b, 0x1E4, "scenarios\\multi\\x\\x")
      le32(b, 0x164, stringsAt); le32(b, 0x168, 4); le32(b, 0x16C, 0x40)
      le32(b, 0x170, stringsAt); le32(b, 0x174, stringsAt + 0x20)
      le32(b, 0x2E8, tags); le32(b, 0x2EC, stringsAt + 0x100); le32(b, 0x2F0, 0x100); le32(b, 0x2F4, stringsAt + 0x200)
      le32(b, 0x304, 0); le32(b, 0x308, 0)
      le32(b, 0x30C, 0x12345678)
    }
    writeIndex(b, metaOffset, base, tagDataStart, groups, tags)
    for (t in 0 until tags) {
      val at = tagDataStart + t * 0x100
      for (i in 0 until 0x100) b[at + i] = (modByte + i).toByte()   /* the mod's own bytes */
    }
    return b
  }

  /* ---- what comes out ---- */

  @Test fun aU0MapConvertsWhole() {
    val out = MccConvert.convert(mccMap(u1 = false), false, discMap())
    val back = HaloMap.read(out)
    assertEquals(HaloMap.Kind.XBOX_H2, back.kind)
    assertEquals("the disc's build", "02.09.27.09809", back.build)
    assertEquals("the same map", "coagulation", back.internalName)
    /* the body kept whole: the index area where the MCC map had it */
    assertEquals(0x1000, HaloMap.le32(out, 0x10))
    assertEquals("the index area's real size", HEAD, HaloMap.le32(out, 0x14))
    assertEquals(TAGS * 0x100, HaloMap.le32(out, 0x18))
    assertEquals(HEAD + TAGS * 0x100, HaloMap.le32(out, 0x1C))
    assertEquals(out.size, HaloMap.le32(out, 0x8))
    /* every tag where it was, at the address it had */
    val tagTableAt = 0x1000 + 0x20 + GROUPS * 0xC
    for (t in 0 until TAGS) {
      assertEquals(K + 0x1000 + HEAD + t * 0x100, HaloMap.le32(out, tagTableAt + t * 0x10 + 8).toLong() and 0xFFFFFFFFL)
    }
    /* the mod's bytes, whole */
    for (t in 0 until TAGS) {
      assertEquals("tag $t's data", 0x5A, out[0x1000 + HEAD + t * 0x100].toInt())
    }
  }

  @Test fun aU1MapMovesItsIndexArea() {
    /* a real index area is far bigger than the fixture's: enough tags that
     * the tag data clears the Xbox header's room, as real maps do */
    val groups = GROUPS
    val tags = 100
    val head = 0x20 + groups * 0xC + tags * 0x10
    val mcc = mccMap(u1 = true, groups = groups, tags = tags)
    val out = MccConvert.convert(mcc, true, discMap())
    assertEquals(HaloMap.Kind.XBOX_H2, HaloMap.read(out).kind)
    val metaOffset = HaloMap.le32(out, 0x10)
    assertTrue("the index area moved past the Xbox header", metaOffset >= 0x800)
    assertTrue("it moved to the file's end", metaOffset >= mcc.size)
    /* the moved header's own addresses point at the moved tables, under the base */
    assertEquals(K + metaOffset + 0x20, HaloMap.le32(out, metaOffset).toLong() and 0xFFFFFFFFL)
    assertEquals(
      K + metaOffset + 0x20 + groups * 0xC,
      HaloMap.le32(out, metaOffset + 8).toLong() and 0xFFFFFFFFL)
    assertEquals(0x73676174, HaloMap.le32(out, metaOffset + 0x1C))
    /* the tags keep the addresses they had — the tag data never moved */
    val tagTableAt = metaOffset + 0x20 + groups * 0xC
    val oldTagTableAt = 0x380 + 0x20 + groups * 0xC
    for (t in 0 until tags) {
      assertEquals(
        HaloMap.le32(mcc, oldTagTableAt + t * 0x10 + 8).toLong() and 0xFFFFFFFFL,
        HaloMap.le32(out, tagTableAt + t * 0x10 + 8).toLong() and 0xFFFFFFFFL)
    }
    /* the tag data sits where it always did, the mod's bytes on it */
    assertEquals(0x5A, out[0x380 + head].toInt())
    /* the dead index area under the Xbox header is zeroed */
    assertEquals(0, out[0x380].toInt())
  }

  @Test fun anotherBaseRefuses() {
    try {
      MccConvert.convert(mccMap(u1 = false, base = K + 0x10000), false, discMap())
      fail("a map of another base refuses")
    } catch (e: MccConvert.ConvertException) {
      assertTrue(e.message!!, e.message!!.contains("address base"))
    }
  }

  @Test fun anotherMapsNameRefuses() {
    try {
      MccConvert.convert(mccMap(u1 = false, name = "zanzibar"), false, discMap())
      fail("a map of another name refuses")
    } catch (e: MccConvert.ConvertException) {
      assertTrue(e.message!!, e.message!!.contains("zanzibar"))
    }
  }

  @Test fun aBrokenIndexRefuses() {
    val mcc = mccMap(u1 = false)
    le32(mcc, 0x1000 + 0x1C, 0)                            /* the tags magic gone */
    try {
      MccConvert.convert(mcc, false, discMap())
      fail("a map with no tags magic refuses")
    } catch (e: MccConvert.ConvertException) {
      assertTrue(e.message!!, e.message!!.contains("tags"))
    }
  }

  @Test fun aTagOutsideTheFileRefuses() {
    val mcc = mccMap(u1 = false)
    le32(mcc, 0x1000 + 0x20 + GROUPS * 0xC + 8, (K + 0x1000000).toInt())  /* tag 0 far away */
    try {
      MccConvert.convert(mcc, false, discMap())
      fail("a tag outside the file refuses")
    } catch (e: MccConvert.ConvertException) {
      assertTrue(e.message!!, e.message!!.contains("one base"))
    }
  }

  @Test fun aTableElsewhereRefuses() {
    val mcc = mccMap(u1 = false)
    le32(mcc, 0x1000 + 8, (K + 0x1000 + 0x1000).toInt())   /* the tag table's address moved */
    try {
      MccConvert.convert(mcc, false, discMap())
      fail("tables that sit elsewhere refuse")
    } catch (e: MccConvert.ConvertException) {
      assertTrue(e.message!!, e.message!!.contains("somewhere other"))
    }
  }

  @Test fun anMccMapOverItsStringAreaRefuses() {
    /* its string data would sit inside the Xbox header's room */
    val mcc = mccMap(u1 = true)
    le32(mcc, 0x34, 0x400)                                  /* string data at 0x400 */
    try {
      MccConvert.convert(mcc, true, discMap())
      fail("a string area inside the header's room refuses")
    } catch (e: MccConvert.ConvertException) {
      assertTrue(e.message!!, e.message!!.contains("inside the Xbox header"))
    }
  }
}
