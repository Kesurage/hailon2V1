# <img src="docs/logo.png" width="56" align="top" alt="hailon2V1 logo"> hailon2V1

**Halo 2 on Android — a fork of [hakuX](https://github.com/rfandango/hakuX) (xemu lineage,
GPL-3.0-or-later), targeted at the Infinix Hot 50 4G (Helio G100, Mali-G57 MC2, Android 14).**

This repository is Phase 0 of the roadmap: the launcher shell (Kotlin/Compose), the module
boundaries, the launch-request contract, JNI stubs, the four port services as typed stubs,
docs, and CI. The upstream emulator core is wired in the next step (see `core-hakux/README.md`).

Working in the shell as of v0.1.1: launcher icon + in-app logo, and real dump import —
the Library screen copies an ISO/XISO into app storage via SAF (original dump untouched),
probes the XDVDFS "MICROSOFT*XBOX" signature, persists the list, and supports removal.

Lineage: `xemu -> izzy2lost/xemu -> hakuX -> hailon2V1 (this fork)`.
Upstream needs no BIOS/MCPX ROM/HDD image; games are user-dumped ISO/XISO images.

## Modules (roadmap Figure 1)

| Module | Layer | Phase |
|---|---|---|
| `app/` | 1 — Launcher UI (Kotlin/Compose) | P0-P1 |
| `core-hakux/` | 3 — upstream core integration point | P0 |
| `native-glue/` | JNI bridge UI <-> core | P0 |
| `services/malicompat/` | 2 — Mali flicker fixes | P1 |
| `services/fsr/` | 2 — FSR1 (EASU+RCAS) upscaler | P2 |
| `services/modloader/` | 2 — .map overlay + xdelta mods | P3 |
| `services/driverloader/` | 2 — per-app Vulkan ICD injection (panvk-kbase) | P4 |

## Build

```bash
# JDK 17+, Android SDK (platform 35, build-tools 34/35)
./gradlew :app:assembleDebug --no-daemon
# APK: app/build/outputs/apk/debug/app-debug.apk
```

CI builds a debug APK on every push to `main` (artifact) and attaches it to the release
when a `v*` tag is pushed.

## Docs

- `ROADMAP.md` — the full plan (phases, design decisions, risks; PDF with sources in releases)
- `docs/ARCHITECTURE.md` + `docs/ARCHITECTURE.png` — layer contract
- `docs/FLICKER_TRIAGE.md` — no-code flicker triage for Mali devices
- `docs/MOD_FORMAT.md` — Phase 3 mod package spec (draft)

## License

GPL-3.0-or-later, inherited from hakuX/xemu. Every release publishes complete buildable
source; upstream notices are preserved. Free, GitHub Releases only — any paid copy is a scam.
