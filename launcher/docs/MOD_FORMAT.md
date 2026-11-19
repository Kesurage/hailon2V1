# hakuX-H2 mod package format (draft, Phase 3)

A mod is a zip applied **at launch** via a copy-on-write overlay over the dump. The
pristine dump is never modified; disabling a mod restores retail behavior exactly.

```
mymod.zip
  manifest.json          required
  maps/<name>.map        full replacement map(s), or
  patches/<name>.xdelta  delta against a known retail map checksum
  preview.png            optional
```

## manifest.json

```json
{
  "id": "org.halohax.masterchief-swap",
  "name": "Master Chief model swap",
  "version": "1.0",
  "targetTitleId": "0x4D530064",
  "author": "...",
  "loadOrder": 10,
  "entries": [
    { "type": "xdelta", "from": "maps/a10.map", "sha256Before": "<hex>", "to": "patches/a10.xdelta" },
    { "type": "replace", "from": "maps/a30.map", "sha256Before": "<hex>", "to": "maps/a30.map" }
  ]
}
```

Rules:
- Every entry carries the SHA-256 of the retail file it touches. Loader refuses to apply
  on mismatch (title updates change maps) and fails safe to unmodded launch.
- Character (bipd) model swaps arrive as patched maps built with Prometheus-Alpha —
  Assembly does not open original-Xbox Halo 2 maps; compatibility goal is format-level
  (load what the OG Xbox scene distributes), not a port of Assembly.
- Load order: lower `loadOrder` applies first; later mods override earlier ones per file.
