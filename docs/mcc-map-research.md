# Halo 2 map formats — the research (for the MCC → Xbox converter)

Everything below was read out of Assembly's source (github.com/XboxChaos/Assembly,
Blamite library) so nothing here is guessed. The goal of the next builds: import
an MCC (Master Chief Collection, PC) Halo 2 *classic* map mod on the phone and
play it on the Xbox disc's engine. This build (0003A) ships the classification,
decompression and Assembly-patch application; the conversion itself is the part
still to write.

## The three Halo 2 map families

| | Xbox (the disc) | Vista (H2V, 2007 PC) | MCC classic |
|---|---|---|---|
| header version (int32 at 0x4, LE) | 8 | 8 | 13 |
| magic at 0x0 | `daeh` (little-endian "head") | `daeh` | `daeh` |
| build string (asciiz) | 0x120: `02.09.27.09809` (campaign/updated) or `02.08.28.09214` (the MP maps as shipped) | 0x120: `11081.07.04.30.0934.main` | U0: 0x128; U1: 0x90 |
| internal name (asciiz) | 0x198 | 0x198 | U0: 0x1C0; U1: 0xB0 |
| scenario name (asciiz) | 0x1BC | 0x1BC | U0: 0x1E4; U1: 0xD0 |
| type (int32) | 0x140: 0 singleplayer, 1 multiplayer, 2 mainmenu, 3 shared, 4 sp+shared, 5 sounds | 0x140 | U0: 0x148; U1: int16 at 0x18 |
| header size | 0x800 | 0x800 | U0: 0x1000; U1: 0x380 |
| compression | none | none | Saber zlib (below) |
| footer | last 4 bytes `toof`/`foot` | same | same |

Both MCC header revisions exist in the wild (Engines.xml: "Halo 2 MCC" and
"Halo 2 MCC Update 1"). **U1 is the current one.** Telling them apart: U1 has
compression fields in its header (uint32 `compression data chunk size` at
0x308 — the value 0x40000 — `compression data offset` 0x30C, `compression
chunk table offset` 0x310, `compression chunk table count` 0x314, and int16
`flags` at 0x1C whose bit 0 says compressed); U0 has the chunk table at a fixed
0x1000 with at most 0x400 entries and data from 0x3000.

## What is the SAME between MCC and Xbox (why conversion is possible)

The whole meta area is the same 32-bit format — Assembly runs both through
the same `SecondGenCacheFile` and the same plugins (H2MCC's plugin dir falls
back to H2's):

- meta header (0x20): tag group table address/offset +0x0, group count +0x4,
  tag table address/offset +0x8, scenario datum +0xC, matg datum +0x10,
  tag count +0x18, `magic` +0x1C (the ascii "tags")
- tag group element (0xC): group magic, parent, grandparent
- tag element (0x10): group magic, datum index, **memory address**, data size
- tag block (0x8): count, pointer; tag reference (0x8): magic, datum;
  data reference (0x8): size, pointer
- the matg (map globals) languages array at +0x190, 9 languages × 0x1C
- locale index table element (0x8)

## What is DIFFERENT (what a converter must bridge)

1. **The header**: every field moved (table above). A converter keeps the
   Xbox map as the base file and only transplants the areas below, then
   rewrites the Xbox header's `tag data size`, `meta size`, `file size`.
2. **Pointer masks** (the hard part). Tag data pointers are "magic"ed virtual
   addresses; a pointer maps to a file offset as
   `offset = pointer - mask + areaStart` (Blamite `MetaOffsetConverter`).
   - MCC U1: the mask is *in the header* (`meta offset mask`, 0x2D0).
   - Xbox: no mask field — Blamite *derives* it at load (SecondGenCacheFile.
     LoadHeader): read the meta header at `meta offset` (0x10); tag table
     address at +8; the mask reference is the tag group table address at +0
     (if that field exists); `primaryMask = groupTableAddress - 0x20`;
     `tagTableOffset = tagTableAddress - primaryMask + metaOffset`; the first
     tag element's `memory address` (tag table + 0x8) gives the tag-data mask:
     `xboxMetaMask = firstTagAddress - tagDataOffset` (0x14).
   - BSPs have their OWN magics: the scenario's bsp table entries are bsp
     headers `{size, bsp address, lightmap address, magic}` — per-BSP masks.
   - The raw table (sounds etc.) is its own area: MCC has `raw table
     offset/size` in the header (U1 0x2F0/0x2F4); the Xbox map has a
     partition table (SecondGenHeader.Partitions) instead.
   A converter must remap EVERY pointer space (meta, each BSP, raw) from the
   MCC map's masks to the Xbox map's, after verifying the areas are parallel.
3. **Compression**: MCC maps ship Saber-zlib compressed (below). Decompress
   first.
4. Locale globals, sound gestalt datum: extra fields in MCC's header that the
   Xbox map keeps its own way.

## The converter sketch (next build)

Take the Xbox `maps\<internal>.map` off the disc as the base; decompress the
MCC map; **verify** the two are the same map (same tag count, same group
sequence, same tag names through each file table); if the mod adds tags or
raw data, refuse (v1). Transplant the tag data area and the index tables with
all pointers remapped (meta mask, per-BSP masks, raw table base), update the
Xbox header's sizes, and write the result as an imported file mod. Every step
bounds-checked; anything unexpected refuses with the reason, never a corrupt
map.

## Assembly patch formats (shipped in 0003A)

`AssemblyPatchLoader.cs` / `ContainerReader.cs`: a `.asmp` is a block
container. Block header = 9 bytes: uint32 ID (fourcc, LE), int32 size
*including the 9 header bytes*, byte version. The top block is `asmp` v0
holding:

- `titl` v0–3: int32 mapID; asciiz map internal name; UTF-16LE name,
  description, author (each null-terminated); int32 screenshot length +
  bytes; (v1+) uint32/uint64 meta poke base, int8 meta changes index;
  (v2+) asciiz output name; (v3) byte pc flag, asciiz build string.
- `segm` v0: byte count; each segment: uint32 oldOffset, oldSize, newOffset,
  newSize, byte resizeAtEnd, then data changes: uint32 count of 4-byte
  changes (each uint32 offset + 4 bytes), uint32 count of other changes
  (each uint32 offset, int32 size, bytes).
- `blfc`: BLF/mapinfo containers (ignored by us — Halo 2 has none).
- `meta`/`locl`: deprecated blocks (Ascension-era).

`OldPatchLoader.cs`: `.patchdat` (Ascension) is bare (uint32 offset, int32
size, data) triples to EOF, no header. `.ascpatch` (Alteration) is the same
with a prefix: byte author length + author, byte description length +
description. Neither names its target map — the file name must carry it.

`SegmentPatcher.cs`: apply segments sorted by oldOffset; if newSize !=
oldSize, insert (newSize - oldSize) zero bytes at (resizeAtEnd ?
newOffset + oldSize : newOffset); then write each data change at
newOffset + change.offset. Data writes that run off the file are a corrupt
patch — refuse.

`PatchApplier.cs` checks the patch's map internal name against the base
map's, nothing else.

## Saber zlib (MCC's map compression) — `SecondGenSaberZLib.cs`

Chunks of 0x40000 bytes, each stored as: int16 marker (0x1528, skipped on
read) + **raw deflate** (no zlib header), padded out to a 0x80 multiple.
- U1: data starts at the header size (0x380), the chunk table (int32 size,
  int32 offset per chunk, size 0 = end) sits after the last chunk, its
  offset/count/chunk-size are the header fields at 0x308–0x314, and bit 0 of
  the int16 flags at 0x1C is set while compressed.
- U0: the table is at 0x1000 (fixed 0x400 entries), data from 0x3000.
- "Faux" chunks (some tools): a negative size means stored raw — copy
  `-size` bytes from the offset (no marker to skip).
- After decompressing, clear the flags bit so the game loads it plain.

## XDVDFS (the disc image)

Sector 2048 bytes. Volume descriptor at partition+0x10000: 20-byte magic
`MICROSOFT*XBOX*MEDIA`, then root directory sector (uint32) and size
(uint32) at +0x14. Directories are dirent binary trees: each entry is
[left u16][right u16][start sector u32][file size u32][flags u8][name length
u8][name bytes]; left/right are offsets in *4-byte units* within the
directory block (0xFFFF = none), and a directory's entries' sectors are
absolute from the partition start. Flag bit 0x10 marks a directory. The
game partition starts at 0 in an XISO, or after the video partition in a
full dump (offsets: 0x18300000, 0xFD90000, 0x2080000 — `XboxDisc.PARTITIONS`
probes them).

## Where the MCC maps come from (the user's side)

Nexus Mods' Halo 2 sections are MCC-format full `.map` files (the collection
has no patch format in wide use — the Steam tooling makes maps, and modders
ship the finished map). Xbox-era mods (halomods and the old scene) ship as
Xbox `.map` files or Assembly/Ascension/Alteration patches, all of which
0003A's importer now understands on-device.
