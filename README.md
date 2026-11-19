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

## v0002B — the touch look actually works now

- **What was broken in 0002A, plainly**: a normal aim swipe built up to
  ~0.13 stick deflection, and Halo 2 ignores everything under ~0.28 —
  its own stick dead zone. Slow and medium swipes did nothing; only hard
  flicks got through.
- **The fix, on the four points of the spec**: every deflection we mean
  at all is lifted over the game's dead zone on its way out (the
  smallest movement goes as 0.28 turn at the least, the rim is still
  full tilt — and it goes for the move stick too); the look's deltas are
  in dp and the rate's ease is set by the frame's own time (the same
  feel at 60 Hz and 120 Hz, on any panel); a finger still for 40 ms
  stops the camera on the next frame; a quick drag of ~140 dp is a
  full-tilt turn, scaled by the sensitivity slider.
- **Modding instructions in the app**: the Mods screen says in plain
  words how it works — maps and character models live inside the game's
  map files; export the modded .map from Assembly (keep its file name)
  and import it here; a mod.txt or a ZIP works too.

## v0002A — Xash3D's touchpad look, the native screen, the mod loader

What 1.7.0's feedback asked for.

- **The look is a touchpad, Xash3D's way** — its `in_touch.c` calls the
  look "like a touchpad": every move of the finger turns the view by that
  movement (through `touch_yaw`/`touch_pitch`), no anchor, no rim, no
  dead zone. Through the Xbox's stick, the finger's speed is the
  deflection: each move adds to a turning rate that eases away as the
  finger stops, so the camera follows the finger at any pace and stops
  when it does. The ring stays where the finger landed, its knob showing
  the turning.
- **The native screen** — Halo 2 renders for the phone's whole panel: its
  anamorphic factor (`0x45DFDC`, the Halo 2 HD patch's own address; the
  camera FOV global beside it confirmed by kirklandsig/halo2-decompiled)
  is patched at game start to this screen's shape, and the Native aspect
  (the default) lays the frame over the whole screen — the widened view
  and the stretch cancel out: full screen, no bars, no crop, no
  distortion. "Native camera" in Mods & cheats, on by default,
  experimental (the HUD draws for the wider shape; a title update's XBE
  is not patched).
- **The mod loader** — Mods & cheats → Import a mod: a modded cache map
  (.map, Assembly's and the Xbox map tools' own form; keep the map's file
  name and it replaces it on the disc), a mod.txt of XBE patches, or a
  ZIP holding either. Toggles like the built-ins, removable, read at the
  next game start; import errors are named and a failed import leaves
  nothing behind.

## v1.7.0 — FSR 1 for real, the visible look stick, PanVK for the G100

What 1.6.0's feedback asked for.

- **FSR 1 actually runs** — it never did on a widescreen setup: the apply
  check demanded both axes scale alike, and Halo 2's anamorphic frame
  (640×480 pixels carrying 16:9) failed it every frame, silently, back to
  bilinear. The check now upscales any picture smaller than its place
  (EASU takes the axes per axis), and FSR 1 is the default upscale filter.
- **The look controls are visible** — the swipe-look's stick now draws
  where the finger lands (the same ring and knob as the move stick), and
  its anchor chases the finger past the rim, as mobile shooters' sticks
  do: long swipes keep turning without running off the glass.
- **The whole picture is the default again** — Auto (16:9 on the
  widescreen console): nothing cropped, nothing stretched, Halo 2's HUD
  whole. The game renders 16:9 at most; a panel taller than that must
  letterbox, crop or stretch — Fill (crop) and Stretch stay one tap away
  in Settings → Display. The one patch that renders wider (Grimdoomer's
  Halo 2 HD) is an XDelta patch for modded consoles, 16:9 at most, its
  author marks it incompatible with emulators.
- **PanVK for the Helio G100, one tap** — Settings → Graphics → GPU driver
  → For this phone: FristOneRR PanVK v3.7.0 (Mesa 27 on Arm's kbase
  kernel driver, for Valhall-v9 Mali — the G100's Mali-G57 MC2, proven on
  Helio G99/G100 phones) downloads and goes in use from its release. Play
  a minute with each driver, watch the FPS line, keep the faster one; the
  system driver is one tap back.

## v1.6.0 — the Halo 2 launcher update

What 1.5.0's feedback asked for: touch controls that behave like a mobile
game's, Halo 2 above 30 fps, a screen that fills, and the launcher's front
being the game, not the emulator.

- **Swipe to look** — a drag anywhere on the right of the screen, over no
  button, aims the camera the way mobile shooters do: the drag starts where
  the finger lands, short drags aim finely, long ones turn fast. The visible
  right stick still works beside it; Settings → Controls turns the zone off.
- **Floating move stick** — the left stick appears wherever the left thumb
  lands and returns when the finger lifts (Settings → Controls).
- **Look sensitivity of its own** — a separate slider with a wider range
  (0.3–3.0×), a smaller dead zone for the fine aim, and smoothing that
  removes the touchscreen's jitter. Touch arrives unbatched with the
  in-between samples of each move, so the camera follows the finger at the
  panel's own rate.
- **A mobile controls skin** — the sticks draw as a mobile game's (a clean
  ring and a knob — the Xbox cap's cross grooves read as a crosshair on
  glass), the face buttons form a proper diamond, the triggers read LT/RT,
  the D-pad is small beside the buttons, and Halo 2's scope (right stick
  click) has its own button. Pre-1.6.0 saved arrangements give way to the
  new default; the editor still moves, sizes, hides and icons everything.
- **Halo 2 at 60 fps out of the box** — every Halo 2 release (NTSC `4D530064`,
  PAL `4D53004B`, and the regional `4D530051` / `4D53006E`) starts with its
  60 fps patch enabled: up to 60 frames a second at the game's normal speed,
  the engine's real patch (the frame limiter's vblank floor lifted, the game
  time kept at 30 ticks a second). The library's Halo 2 card switches it, or
  120 fps, under Patches; the choice is kept.
- **The launcher's front** — the library opens on a Halo 2 card: Play, the
  60 fps switch, and this game's own settings and patches in one place,
  instead of the universal settings' rounds.
- **Fill by default** — the picture covers the whole screen with the spill
  cropped (undistorted): no black bars on launch. The console reports itself
  widescreen by default, so Halo 2 renders 16:9 and fills with the least
  cropped away. Aspect ratio and screen format stay in Settings → Display.
- **FPS-only HUD** — the frame times are gone; the line reads `FPS: 60`.

### To do (not in this release)

- Motion-blur-off mod; MSAA as a quality option; per-game FSR sharpness.
- A panvk driver package for the G57 — **done in v1.7.0** (For this phone: FristOneRR PanVK v3.7.0).

## v1.5.0 — what 1.4.0's feedback asked for

- **Fill (crop) aspect mode** — the real answer to "full screen doesn't fill
  and stretch distorts": Settings → Display → Picture shape → *Fill (crop the
  edges)*. Both axes are covered at the same scale (no distortion), the spill
  over the panel's shape is cropped, centred. `stretch` stays for those who
  want every pixel squashed.
- **Custom icon controls** — the editor bar's new Icon actions (Settings →
  On-screen controls, or the in-game editor): cycle a button's look between
  the jewel, a flat disc, a thin ring, or **your own picture** (Photo… from
  the gallery, kept in the app's storage, drawn circle-cropped at the
  control's place and size). Sticks, the D-pad and the menu keep their own
  looks.
- **Touch sensitivity you calibrate** — the 1.4.0 curve was too aggressive;
  it is back to neutral (linear from the dead zone's edge to the rim — the
  1.3.1-style jump is still gone) with two new sliders in Settings →
  Controls: **Stick sensitivity** (0.5–1.6×) and **dead zone** (5–30%).
- **Swipe gestures** — swipe up from the bottom of the picture *or down from
  the top* opens the menu (a switch in Settings → Controls turns it off);
  it never starts on a control, so playing with thumbs on the edges is safe.
- **FSR 1 made honest** — the shaders were proven valid (glslang), the
  RCAS sharpness set to AMD's sample default (clearly visible), and when
  FSR can't apply (a scale where the picture isn't smaller than the screen,
  or a stretched shape) the core now logs why instead of silently falling
  back, and the settings say FSR applies "always true at scale 1x".
- **Performance preset** — one tap in Settings → Graphics: scale 1x + FSR 1
  + FXAA off.

### To do (not in this release)

- Motion-blur-off and per-game 60 fps mods — **done in v1.6.0** (Halo 2's 60
  fps patch is on from the start, and the library's Halo 2 card switches it).
- MSAA as a quality option; per-game FSR sharpness.
- A panvk driver package for the G57 — **done in v1.7.0** (For this phone: FristOneRR PanVK v3.7.0).

## v1.4.0 — FSR 1.0, touch sticks, HUD

- **AMD FidelityFX Super Resolution 1.0** (EASU + RCAS, from AMD's
  FidelityFX-FSR, MIT — `licenses/fsr1.license.txt`) as a third upscaling
  filter: Settings → Graphics → Upscaling filter. On the Helio G100's
  two-core Mali-G57, **1x resolution + FSR 1** is the speed preset — the
  frame is edge-rebuilt at the panel's size instead of bilinear-blurred —
  and 2x/3x + FSR is the quality one. FSR replaces FXAA while it runs
  (RCAS sharpens).
- **Touch sticks**: the dead zone no longer jumps (a smooth ramp from its
  edge to the rim).
- **HUD = FPS only**: the debug hitch monitor is off by default now (the
  toggle remains in the settings); the FPS counter stays.
- Full-screen with the notch (`shortEdges`) and the panel's native
  resolution path were already hakuX master's — nothing was lost in the fork.
- **Custom Vulkan drivers** (a panvk build among them) load through
  Settings → GPU driver on phones with `/dev/mali0` — the stock MediaTek
  driver remains the default; panvk is an experiment for the G57, not a
  requirement.

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
