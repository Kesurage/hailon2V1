# hailon2V1 — Port Plan (condensed roadmap)

Full report (with ranked root-cause research, 24 cited sources, risk register): see
`Halo2_Android_hakuX_H2_Roadmap.pdf` in the release assets of v0.1.0-skeleton.

## Premise corrections from research (2026-11)

- hakuX is NOT Xenia-based: it is an original-Xbox emulator in the xemu lineage.
  Halo 2 (2004) is an original-Xbox title; Xenia cannot run it. Forking hakuX is the
  only verified engine path that runs this game on Android.
- Upstream Mesa panvk has no Mali-G57 (Valhall v9) backend. The realistic "panvk on
  Helio G100" is the community panvk-on-kbase line (v9 = declared main target, stock
  kernel, no root), shipped as an opt-in experimental renderer with automatic fallback.
- "Cartography" (Project Cartographer) mods Halo 2 Vista (PC) — cannot attach to an
  emulated console. Online play = Insignia (OG Xbox Live replacement; Halo 2 matchmaking
  in public beta since 2024-03, already supported by hakuX 1.3.0) + XLink Kai system link.
- Assembly cannot open original-Xbox Halo 2 maps. Mod loader targets the OG Xbox scene's
  patched .map / xdelta packages (model swaps included), applied launch-time via
  copy-on-write over the dump (Dolphin Riivolution precedent).

## Phases

| Phase | Weeks | Exit criteria (on device) |
|---|---|---|
| P0 Foundation & baseline | 1-2 | Build pipeline (CI + local) produces APK; flicker reproduced on record against hakuX 1.3.0 |
| P1 Stable on Mali (v1) | 3-8 | 30+ min campaign, zero flicker at 1x; stable 30 fps; per-game profiles; mod loader UI shell validated |
| P2 FSR1 upscaling | 9-12 | <=4 ms added at 720p->1080p class, no flicker regression, visible sharpness win |
| P3 Mod loader | 13-18 | Two community model-swap packs apply and run; pristine dump intact when disabled |
| P4 Experimental panvk | 19-26 | Halo 2 runs 10 min under panvk without device loss, or blocker documented upstream |
| P5 Online & polish | rolling | Full Insignia matchmaking session on-device; fork rebased on latest upstream |

## Design decisions

- D1 Base: hakuX 1.3.0 fork (GPL-3.0-or-later), full fork, launcher = the fork's UI
- D2 Default renderer: stock Mali blob; panvk-kbase opt-in per game with watchdog fallback
- D3 FSR1 (EASU+RCAS) Vulkan compute pass, CAS fallback, off by default
- D4 Mod format: zip (manifest + patched .map or xdelta vs checksummed retail maps)
- D5 Per-app driver loading (adrenotools-style ICD injection), no root
- D6 GitHub Releases only, source published every release, GPL notices preserved
- D7 Fork name distinct from upstream (repo: hailon2V1)

## Target device

Infinix Hot 50 4G (X6882): Helio G100 (G99-class: 2x A76 + 6x A55), Mali-G57 MC2
(Valhall v9, Job Manager), 8 GB LPDDR4X, 1080x2460 120 Hz, Android 14 / XOS 14.5,
stock ~5.10 kernel, locked bootloader, no root — kernel-level driver replacement is
explicitly out of scope.
