# hailon2V1

An original-Xbox emulator for Android, built from [hakuX](HAKUX-README.md)
(GPL-3.0+, by rfandango — full credit and thanks), tuned for the
**Infinix Hot 50 4G** (Helio G100, Mali-G57 MC2).

The engine is real: the game's x86 code runs on a FEX-based JIT, the Xbox
kernel is reimplemented (no BIOS, no MCPX ROM, no hard disk image needed),
and the NV2A's push buffer is drawn through Vulkan. hakuX's own Android app
(`android/`) is the front end.

## Why this fork exists

The stock hakuX v1.3.0 APK, on a Mali-G57 phone:

- **flickered** — v1.3.0 kept its swapchain image arrays at 8, but Android
  drivers (MediaTek's among them) may report more images than asked for;
  presenting an image outside the array flickers. Master fixes this.
- **lost meshes / the device** on Mali-G57 — master adds the `vb_align`
  quirk (misaligned vertex attribute fetches; found from an Infinix X6858
  owner's diag log), the `no_wbuf` quirk (W-buffered draws hard-stopping
  the GPU) and the timeline-semaphore wait fix (device loss on Arm drivers
  before r40).

So this fork builds hakuX **master** (a7cdb5a, post-1.3.0) rather than the
v1.3.0 release, and adds:

- **Empty-library diagnostics** — when the games folder has no playable
  image, the library now says what the folder holds and which formats are
  playable (`.iso`, `.xiso`, `.cso`, `.cci`), instead of a bare
  "No supported games found".
- **A stable committed signing key** (`android/signing/`, public by design)
  so CI builds update one another.
- **CI that builds the whole engine from source** (`.github/workflows/
  engine.yml`): core (NDK 29, arm64) → packed into the app → APKs, and a
  GitHub Release on every `v*` tag. The in-app updater follows this
  repository's releases.

## v1.4.0 — the launcher release

- **AMD FidelityFX Super Resolution 1.0** (EASU + RCAS, from AMD's
  FidelityFX-FSR, MIT — `licenses/fsr1.license.txt`) as a third upscaling
  filter: Settings → Graphics → Upscaling filter. On the Helio G100's
  two-core Mali-G57, **1x resolution + FSR 1** is the speed preset — the
  frame is edge-rebuilt at the panel's size instead of bilinear-blurred —
  and 2x/3x + FSR is the quality one. FSR replaces FXAA while it runs
  (RCAS sharpens).
- **Touch sticks fixed**: the dead zone used to jump straight from zero to
  the raw deflection; it now ramps smoothly from its edge, and full
  deflection is reached a little inside the rim, where a thumb actually
  lands. The overlay editor (move, resize, hide, per-control opacity) and
  the jewel-style icons are hakuX's own and stay.
- **HUD = FPS only**: the debug hitch monitor is off by default now (the
  toggle remains in the settings); the FPS counter stays.
- Full-screen with the notch (`shortEdges`) and the panel's native
  resolution path were already hakuX master's — nothing was lost in the fork.
- **Custom Vulkan drivers** (a panvk build among them) load through
  Settings → GPU driver on phones with `/dev/mali0` — the stock MediaTek
  driver remains the default; panvk is an experiment for the G57, not a
  requirement.

### To do (not in this release)

- Motion-blur-off and per-game 60 fps mods (the engine's mod loader already
  ships: Settings per game → Mods & cheats; `tools/mods/` has Halo 2 120 fps
  tooling upstream).
- MSAA as a quality option; per-game FSR sharpness.
- A panvk driver package for the G57 to try from the settings.

The earlier hailon2V1 launcher skeleton (a UI mock with stubs) is kept under
`launcher/` (its workflow only runs on changes there).

## Installing

Grab an APK from [Releases](https://github.com/Kesurage/hailon2V1/releases).
`hailon2V1-*-debug.apk` installs **beside** the stock hakuX (its application
id ends in `.debug`), so both can be compared; it is shrunk and optimized
like a release build. `hailon2V1-*-release.apk` is the plain release build.

Games: your own dumps, as ISO/XISO (CSO/CCI too). Point the app at the
folder holding them (first-run setup, or Settings → Games Folder).

## Building from source

CI does it (see the workflow), and the steps are hakuX's own
[android/README.md](android/README.md): fetch the third-party sources
(`tools/ci/fetch-nc-deps.sh`), build the core with the NDK, pack it with
`tools/native/app-libs.sh`, then `android/gradlew assembleDebug`.

## License

GPL-3.0 or later, as hakuX ([COPYING](COPYING), [LICENSE](LICENSE),
[licenses/](licenses/)). The complete corresponding source of any APK built
from this repository is this repository itself.
