# Changelog

## v1.7.0

FSR 1 actually running, the look controls visible on the screen, a graphics driver to try for the Helio G100, and the whole picture back.

### FSR 1
- **FSR 1 now runs** — it never did on a widescreen setup: its apply check demanded both axes scale alike, and an anamorphic frame (640×480 pixels carrying a 16:9 picture, what Halo 2 renders) never passes it, so every frame quietly fell back to bilinear. The check now lets FSR upscale any picture smaller than its place — anamorphic included, which EASU takes per axis — and the log says why on the rare fallback.
- **FSR 1 is the default upscaling filter** (Settings → Graphics keeps Bilinear, FSR 1 and SGSR): at 1x resolution the frame is always smaller than its place on a phone, FSR's two light passes cost little on the Mali-G57, and the difference beside bilinear's blur is clear.

### Touch controls
- **The look is visible now** — the swipe-look's stick draws where the finger lands, the same clean ring and knob as the move stick's, instead of an invisible zone.
- **The anchor chases the finger** past the rim, as mobile shooters' sticks do: a long swipe keeps turning at full tilt without the finger running off the glass; back within the rim it aims as before.

### Display
- **The whole picture is the default again** (Auto: 16:9 on a widescreen console): nothing cropped, nothing stretched. Halo 2 renders 16:9 at most — on a taller phone panel Fill's crop took the top and bottom of the HUD with it, and the one patch that widens the game's rendering (Halo 2 HD) is an XDelta patch for real consoles, 16:9 at most itself, and its author marks it incompatible with emulators. Fill and Stretch stay one tap away in Settings → Display; on a panel taller than 16:9 one of the three always gives.

### Graphics driver
- **PanVK for the Helio G100, one tap** — Settings → Graphics → GPU driver → For this phone: FristOneRR's PanVK v3.7.0 (Mesa 27 on Arm's kbase driver, built for Valhall-v9 Mali GPUs — the G100's Mali-G57 MC2 among them, proven on Helio G99/G100 phones), downloaded and put in use from its release. Play a minute with each driver and watch the FPS line; keep the faster one — the system driver is one tap back.

## v1.6.0

The Halo 2 launcher update: touch controls that work like a mobile game's, Halo 2 at 60 fps out of the box, and a full screen that fills without stretching.

### Touch controls
- **Swipe to look** — a drag anywhere on the right of the screen, over no button, aims the camera, as mobile shooters do: the drag starts where the finger lands, short drags aim finely, long ones turn fast. The right stick's button still works beside it (Settings → Controls → Swipe to look)
- **Floating move stick** — the move stick appears wherever the left thumb lands, instead of only at its fixed place; it goes back when the finger lifts (Settings → Controls → Floating move stick)
- **Look sensitivity of its own** — the camera's sensitivity is separate from the move stick's, with a wider range (Settings → Controls), a smaller dead zone for fine aim, and smoothing that takes out the touchscreen's jitter
- **A smoother digitizer** — touch events are taken unbatched, and the in-between samples of each move reach the sticks: the camera follows the finger at the panel's own rate, not at the frames' batches
- **Mobile controls skin** — the sticks draw as a mobile game's, a clean ring with a knob, not the Xbox controller's cross-grooved cap; the face buttons sit in a proper diamond, the triggers read LT and RT, and the D-pad is small beside the buttons. The saved arrangement from before v1.6.0 gives way to the new one (the editor still changes everything)
- **The menu gestures keep out of the camera's way** — a swipe on the right of the screen can no longer open the menu mid-aim; the menu's swipe starts on the left and in the middle

### Halo 2
- **60 fps patch on from the start** — every Halo 2 release (NTSC, PAL and the regional ones) runs with its 60 fps patch enabled out of the box: up to 60 frames a second at the game's normal speed. Turn it off, or take 120 fps instead, in the library's Halo 2 card (Patches) — the choice is kept
- **The launcher's front** — the library opens on a Halo 2 card: Play, the 60 fps patch's switch, and this game's own settings and patches, one place, not the universal settings' rounds

### Display
- **Fill the screen by default** — the picture covers the whole screen with the spill cropped away, undistorted: no black bars and no stretching (Settings → Display → Aspect ratio keeps Auto, 4:3, 16:9, Stretch and Fill)
- **Widescreen by default** — the console reports itself widescreen, so Halo 2 renders 16:9 and fills with the least cropped away
- **The FPS line is the FPS alone** — the frame times left it

## v1.3.0

Xbox Live through Insignia, and game fixes.

### Xbox Live
- **Insignia** — Settings → Network → Connection → Xbox Live: Insignia connects the console to Insignia, the revival of the original Xbox Live: Live games sign in, find matches and play online. If your Internet provider blocks Insignia's DNS server, point it at a computer running insigniaDNS instead
- **Xbox Live setup** — Settings → Network → Xbox Live setup walks you through it, one step at a time: the connection, the Xbox dashboard, registering the console with Insignia's Setup Assistant (downloaded for you), then making or recovering your account in the dashboard
- **Dashboard** — the library's Dashboard button (top left) starts the Xbox dashboard from the console's hard disk, where Xbox Live accounts are made and recovered. The first time, it installs the dashboard from a zip of its files (dashboard 5960) that you provide: hakuX doesn't include one
- **Title updates** — a game's Xbox Live update, once downloaded, starts with the game. Forza Motorsport's 60 and 120 fps mods cover its update too

### Games
- **Fable: The Lost Chapters** — the hero's face and arms are no longer black or noise
- **Morrowind** — no longer freezes when you leave a building: disc and hard-disk reads now take a little time, as on the console. The inventory's character is drawn properly: it no longer stands on red with a black band over its head, or leaves ghost copies of itself around it when you change gear after a load
- **Halo 2** — the fog no longer shows as stripes that move with the camera, and with the scope up the view around it is dimmed with black lines, not light blue
- **Munch's Oddysee** — no dark box over the ground under the characters' shadows
- **Project Zero II (PAL)** — no longer hangs on its first frame
- **Amped 2** — no longer crashes as its first level loads
- **Mortal Kombat: Shaolin Monks** — the models no longer explode on Mali-G57 GPUs
- **Burnout 2: Point of Impact** — no longer crashes as it starts
- **Def Jam: Fight for NY** — no longer freezes on one frame at the intro cutscene: while hakuX holds a game up (such as while its shaders compile), the game's clock now waits too
- **Double-S.T.E.A.L: The Second Clash** — no longer stops as its first scene begins, and the screen is no longer black, also after loading a savestate
- **Soldier of Fortune II: Double Helix** — no longer crashes when you skip an intro movie
- **Touge R** — races are no longer black under the HUD

### Performance
- **Network** — with the network on (system link or XLink Kai), the console no longer keeps most of a CPU core busy

### App
- **On-screen controls editor** — its toolbar wraps onto more rows on narrower screens, such as 4:3 handhelds, instead of pushing buttons off the edge

## v1.2.0

System link, stability and compatibility improvements.

### Network
- **System link over your network** — Settings → Network → System link → LAN: hakuX devices on the same Wi-Fi network find each other as Xbox consoles on a LAN do. Devices the network's broadcasts don't reach, on another subnet or a VPN such as ZeroTier or Tailscale, are added by address under Other consoles
- **XLink Kai** — system link over the Internet through an XLink Kai engine running on a computer on your network; its web page picks the game's arena
- **Console identity** — each console has its own serial number, network address and online key. A device whose hard disk was copied from another one offers to make itself a new identity, since two consoles with one identity can't see each other


### Games
- **Sega GT 2002** — the cars have their paint, reflections and sun highlights
- **Doom 3** — no longer crashes as its main menu loads
- **Fatal Frame II** — starts and reads its disc in the order it asks for
- **AirForce Delta Storm** — answers the controller and the on-screen controls (it ignored all input)
- **Silent Hill 2** — no longer crashes when it loads a room
- **Splinter Cell: Double Agent** — no longer closes on boot
- **Super Monkey Ball Deluxe** — its title movie plays and its movies aren't black
- **Ultimate Spider-Man** — no longer crashes a second into boot
- **Grand Theft Auto III** — no longer shows its bad-disc screen
- **Project Gotham Racing 2** — the attract replay no longer goes black
- **Dungeons & Dragons Heroes** — the tutorial's prompts show
- **Sound** — games that hold their voices while the sound engine updates them, or stop a voice as it changes, no longer freeze or play a stopped voice on

### Performance
- **Fewer stutters with Turnip** — frames reach the screen without a CPU wait in front of each one

### App
- **Controllers** — only the players with a controller are plugged into the console (player 1 always, for the on-screen controls), and a controller plugs in or out as it connects, disconnects or is assigned to another player, without restarting the game

## v1.1.0

GPUs beyond Adreno (experimental).

### GPUs
- **Mali, Xclipse and PowerVR (experimental)** — hakuX runs on GPUs other than Adreno: features they lack are replaced (BC and 4444 textures decoded up front, standard border colours, the push block in a buffer, D32S8 depth), and known driver bugs are worked around (`hw/xbox/hle/vk/quirks.c`). Burnout 3 and Mercenaries run cleanly on a Pixel 8 (Mali-G715)
- **Vulkan 1.1 drivers** — GPUs whose drivers stop at Vulkan 1.1 (Valhall Mali on older or vendor-capped drivers, Qualcomm's own older drivers, early Xclipse, PowerVR BXM) run through a compatibility layer (`hw/xbox/hle/vk/compat.c`)
- **Older GPUs without descriptor indexing (experimental)** — Bifrost Mali (G71, G72, G76, G52) and PowerVR Rogue: each draw binds its own textures, and depth is clamped in the shader where the GPU can't
- **Adreno** — unchanged: every replacement is off where the GPU has the feature.

### App
- **GPU driver** — a screen of its own (Settings → GPU driver): the system driver and the stored packages in one list, with their versions and authors. Packages are checked when added and can be deleted (deleting the one in use goes back to the system driver)
- **Turnip recommended** — a Qualcomm phone running its own driver older than Vulkan 1.3 suggests a Turnip driver once, from the library
- **Back key** — the device's back key opens the in-game menu, from any input, including the Retroid Pocket 5's own Back. A controller with both Back and Select keeps Back for the menu; on one without a Select key, Back is the Xbox Back button
- **Dark theme** — Settings → Display → App theme: the phone's, light or dark; the in-game menus follow it, in the app's green rather than Android's teal
- **Controller navigation** — the app works with a controller: the focused item stands out (a game is outlined in green), A selects, B goes back, and Y or Select opens a game's menu. The search field opens the keyboard only on A, the library has a refresh button, the mapping sliders move with left and right, release notes scroll, and X deletes a savestate from the in-game Load state list


## v1.0.0

A new core replaces xemu's QEMU-based one.

### New core
- **CPU** — the Xbox's x86 code runs on a FEX JIT (FEXCore fork), and the Xbox kernel is reimplemented natively (`native/kernel`): no QEMU
- **GPU** — the NV2A push buffer is decoded into Vulkan (HLE, `hw/xbox/hle`). Custom drivers (Turnip) load through adrenotools; driver-specific workarounds are quirks (`hw/xbox/hle/vk/quirks.c`, `debug.hakux.quirks`)
- **Audio** — the MCPX APU runs on the core's own services (`native/dev/apu`), out through AAudio
- **Savestates** — per game, in slots, with the hard disk's files
- **Mods and cheats** — per game, from `mods/` (library long-press "Mods & cheats"): XBE patches and replaced disc files; 60 and 120 fps mods for 13 games (`mods/README.md`, generators in `tools/mods`)
- **Controllers** — each physical controller routed to a player with its own mapping; the on-screen controller adds to player 1

### New features
- **Updates** — hakuX looks for a newer release on GitHub when it starts and on demand (Settings → Updates); one found shows its notes and installs from there
- **Frontends** — a game launched from ES-DE or another frontend returns to it when it closes
- **Console clock** — follows the phone's time and time zone, or a time set by hand (Settings → Console → Clock)
- **Start errors** — a game that can't start says why (the file can't be read, isn't an Xbox disc, or its program can't load) and goes back to the library
- **Mods** — Halo 2's 60 fps mod draws characters, vehicles and objects at 60; DOA2's 120 fps mod keeps the select screen's legend

### Performance
- **No pipeline-cache stutter** — each game keeps its own pipeline cache, written only after new compiles; the one shared by every game was rewritten every few seconds and froze the picture for 0.2–0.8 s (Ninja Gaiden Black)
- **Audio** — about 32 ms of latency instead of 128 ms, and less CPU for the sound engine
- **GPU emulation** — faster push-buffer decoding, texture unswizzling (NEON) and hashing, fewer lookups per draw
- **Savestates** — compressed on several threads (a Halo 2 save: 9.6 s to 3.9 s)
- **Idle frames** — the screen isn't redrawn when nothing changed, and the on-screen controller redraws only what changed
- **Disc reads** — a large read waits only its own thread, not the whole game
- **Library** — folders scanned in one pass (38 games: 270 ms to 63 ms)

### App
- **On-screen controller** — the menu button is a green dot
- **Large screens** — Android 16 keeps the landscape lock (the app is declared a game)
- **Storage** — JIT symbol maps left by earlier builds (gigabytes on some devices) are deleted at start
- **Backups** — hakuX's data is left out of Android's cloud backups and device transfers
- **Library** — savestate thumbnails and driver ZIPs load off the main thread

## v0.3.1

### New Features
- **Persistent log capture** — logs kept per session, rotated

### Improvements
- **Texture decompression** — S3TC decoded on several threads; 3D textures no longer use native BC (it corrupted BC3)
- **Surface eviction** — lazy, without a GPU finish per eviction, and no download for VRAM data that is never read
- **Texture cache** — no thrashing from an aggressive memory budget trim
- **Profiling** — all instrumentation behind `NV2A_PERF_LOG` (off by default)
- **XISO conversion** — extract-xiso replaces the Rust xdvdfs converter (no Rust/Cargo dependency)

## v0.3.0

### New Features
- **Per-game settings** — overrides per game in the settings UI
- **Xbox dashboard** — dashboard management, HDD tools, Insignia networking (NAT)
- **Texture dump and replacement**
- **Game compatibility quirks** — a per-game layer (Fable's cycle-breaker)
- **Debug log export** — from the settings
- **Skip boot animation** toggle

### Improvements
- **Vulkan** — native BC texture upload when the GPU has it, batched render sync, NEON blits, descriptor management without bindless textures
- **Vertex shaders** — the emulator stub replaced; uniform uploads optimized
- **Audio** — aligned with x1_box
- **Emulator process** — runs separately again (`:xemu`)
- **XISO** — the original ISO kept after conversion

### Bug Fixes
- Surface eviction VRAM corruption and stale texture sampling
- Vulkan pipeline exhaustion and eviction crashes
- A crash on a quick emulation restart (RCU)
- The 30 fps trap
- XISO conversion launching the original file when SAF renamed the output

## v0.2.1

### Bug Fixes
- **Fix HDD corruption** — restore block device flush on app terminate and background to persist QCOW2 metadata. Cache partition clear now uses synchronous writes
- **Fix game freeze on TB flush** — disable post-flush rewarm on Android which could hang on self-modified code
- **Fix TB cache prewarm crash** — disable startup prewarm on Android. Add build hash to tb_cache.bin for auto-invalidation on new builds
- **Fix on-screen controller hidden on some devices** — filter out virtual/internal devices (accelerometers, gyroscopes) that falsely report as gamepads

### Improvements
- **Simplified FPS overlay** — shows only FPS count
- **Unified log tags** — all logcat output uses hakuX prefix. Filter with: `adb logcat | grep hakuX`
- **HDD cache clear on background thread** — prevents ANR on large images

## v0.2.0

### New Features
- **OpenGL ES renderer** — experimental alternative to Vulkan, selectable in Graphics settings
- **EEPROM editor** — change Xbox language, video standard, resolution modes, aspect ratio, refresh rate
- **Settings index** — reorganized into 4 sections: hakuX Data, Graphics, Debug, EEPROM
- **System file pickers** — change MCPX ROM, Flash ROM, HDD image from settings
- **HDD export** — save a copy of the Xbox HDD to any folder
- **Games folder in settings** — moved from library screen to settings
- **Game search** — search bar with live filtering in game library
- **Pull-to-refresh** — swipe down to rescan games folder
- **XISO progress bar** — real progress tracking across copy/convert/save phases
- **XISO integrity check** — verifies XISO before launch, auto-rebuilds from ISO if corrupt

### Improvements
- **Controller overlay** — D-pad/stick repositioned, LS/RS as separate buttons, menu button, swipe-up gesture, trigger axis fix (-1.0 for release), per-pointer tracking
- **Pause menu** — actually pauses emulation, simplified UI
- **NEON optimizations** — S3TC decompression and texture swizzle (benefits both renderers)
- **XISO conversion** — staged in temp directory, clean up on interruption, write permission check
- **FPS overlay** — shows driver info or "OpenGL ES" label, frame pacing, shader stats
- **Clear code cache** button in Debug settings
- **Graceful VK texture LRU handling** — skip instead of crash when cache exhausted

### Bug Fixes
- Fix TB cache crash when FP settings change between sessions
- Fix pause/resume not working from in-game menu
- Fix GLES geometry shader crash (gl_PointSize unavailable without extension)
- Fix GLES shader version detection (was hardcoded to 320, now auto-detected)
- Fix shader link failure crash (graceful fallback instead of assert)
- Fix shared GLSL changes breaking VK (gated behind renderer check)

### Rebranding
- Package renamed to `com.rfandango.haku_x`
- Debug builds install alongside release (`com.rfandango.haku_x.debug`)

## v0.1.0

- Initial release
