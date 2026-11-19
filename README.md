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
