# Changelog

## v0003D

The browser's pictures actually load (0003C read a field GameBanana never
sends), the touch path stops touching SharedPreferences on every finger
event, and a broken mod has a way back.

- **The browser's no-image bug, found and fixed**: GameBanana's records
  carry previews as a CDN base plus the same picture at several widths
  (_sBaseUrl + _sFile530) — 0003C read a _sUrl that never comes, so no
  preview ever loaded. The list request asks for the media now too, rows
  carry 220px thumbnails, and the bundled catalog's 18 entries have their
  previews baked in (17 have any on GameBanana). Nexus's picture and the
  Archive's thumbnail show the same way.
- **Errors that say what happened, with a way out**: a 429 is the source
  rate-limiting (the Archive does, hard — not "did not answer"), a 401/403
  is the key's own matter, a 404 the thing being gone; every load error
  gets a Retry beside it, and a failed image ends in a quiet placeholder
  instead of an endless spinner.
- **The touch path, unblocked**: the finger's events read plain mirrored
  fields now — 0003C read SharedPreferences per event, fighting the game
  thread for the same lock (the drag-latency and misfire reports). The
  settings' screen and the in-game bars write the same preferences, so a
  change still lands instantly.
- **The tuning dialog grown a conscience**: each bar says what its number
  means ("full at N dp" — smaller is touchier), and Reset puts the defaults
  (1.0 / 1.35) back — 0003C offered a 6.0 look with no words and no way
  home from a bad value.
- **The swipe-up menu gesture starts in the bottom strip only** (was: the
  lower two-thirds — an aim reposition mid-fight could pause the game).
- **Speed, measured**: the in-game menu's Speed item shows the game's own
  frame rate as measured (a 5 s average behind it) and offers the honest
  one-tap answer — 1x + FSR at the next start. When the average stays
  under 24 fps at 2x or over, one toast per run says so and points there.
- **The safe mode**: a run that does not end cleanly (a crash, a kill, a
  force stop — or a mod that broke it) leaves its mark; the next launch of
  that same disc offers Start without mods — a clean boot, no mods, no
  cheats, with a word on screen that it is on. The clean exit clears the
  mark.
- **The in-game menu lists this run's mods** (each on or off, cheats
  marked) with the honest note: file mods are built in at boot, so they
  change from the next start, on the Mods screen.

## v0003C

Touch tuned live, the browser grown a face, and the 2x cost said plainly.

- **Touch, tuned while you play**: the in-game menu (swipe up) has Touch sensitivity now — two seek bars, move stick and look, felt on the next drag, nothing restarted. The settings' ranges went up besides: the move stick to 3.0, the look to 6.0 (the settings screen's sliders and the in-game bars are the same settings).
- **The browser grown a face**: a mod opens full-screen — its previews (GameBanana's banners and screenshots, Nexus's picture, the Archive's thumbnail) scroll across the top, its description, its files each with its own compatibility line and Take. A take now shows its progress (of how many MB) and its result says the whole pipeline: downloaded → checked → installed over the disc and turned on.
- **Every mod at once**: the Mods screen has All on / All off under its list — the game's whole mod set in two taps, beside each mod's own switch (which was already there).
- **2x resolution, honestly**: 2x shades 4x the pixels (3x, 9x) — that cost is the GPU's own, not a bug. What this build does about it: FXAA is skipped on purpose at 2x and over (the scale has already smoothed the edges; the pass was shading 4x the pixels for nothing), and the settings say the tradeoff plainly — the Speed preset (1x + FSR) is the close-looking fast answer.
- Said plainly, again: Steam Workshop/depots need Steam itself (not had on a phone), and the sites without public APIs (LoversLab, ModDB) are not in the browser — Nexus (your own key), GameBanana and the Internet Archive are what a phone can honestly reach, and none needs a captcha.

## v0003B

MCC mods supported: **a Master Chief Collection map now converts to the disc's own format on the phone**, and the mod browser opens the modding sites to it.

- **The converter** (MccConvert): an MCC map's body is kept whole behind an Xbox header — no tag surgery. Its safety is not faith: both maps' address bases are read from their own tag tables and must agree (the disc's own map calibrates the check, every conversion runs against the disc's map of the same name), the collection's areas must clear the disc's header (its index area alone moves, with its two addresses rewritten), and the result is re-parsed and its tags compared entry-by-entry before anything is installed. Every refusal names its reason. **Experimental** until the game itself has vouched for it — a converted mod says so under its description, and turns off like any other.
- **The mod browser** (Mods → Browse mods): this app's own catalog (18 verified entries to start, refreshed from the repository between releases), **GameBanana's open API** (the classic Halo 2 section and the collection's), **the Internet Archive's search**, and **Nexus Mods behind your own API key** — the one place the collection's whole scene (adult half included, behind the Show adult content toggle) is listed. Every entry carries a compatibility line (works here / partial / not for this game / reads on arrival), downloads land through the importer's own classification, and what lands is an ordinary imported mod: on, off or gone from the Mods screen.
- Steam Workshop needs Steam itself — on a phone that is not had; Nexus, GameBanana and the Archive are what a phone can honestly reach, and none of them ask for a captcha.

## v0003A

The mod manager rebuilt: the importer knows what it is holding, builds modders' patches onto the disc's own maps **on the phone** (no PC, no Assembly), and says exactly why when something will not go.

- **Assembly patches, applied on the phone.** A `.asmp` (Assembly's own format) — and the older scene's `.patchdat` (Ascension) and `.ascpatch` (Alteration) — is read, its target map found **on your own disc**, extracted, the patch applied byte for byte (the exact algorithm Assembly uses, ported from its source), and the rebuilt map installed as a mod. Small downloads beat 100 MB maps; the patches you find on the old modding sites now just work. A patch for a map that isn't on the disc, or cut against another build of the game, refuses with the reason — the disc's map is never touched.
- **The importer reads every .map for what it is**, instead of installing whatever arrived: the disc's own Xbox build (9214/9809 — installed, and only where a map of that name really is on the disc), Halo 2 Vista (the 2007 PC port — refused, another engine build), the **Master Chief Collection's** format (recognized, its Saber-zlib compression decoded — the conversion to this disc's format is the next build; the research that makes it possible is in the repository, docs/mcc-map-research.md: the tag data is the same 32-bit format in both), Halo 1/3 maps (refused), and not-a-map (refused).
- **ZIP imports grown to match**: every .map and patch in a package is classified and handled on its own; one mod comes out of it, with the leftovers listed in its description. A mod.txt tree still imports as before.
- **The Mods screen says more**: each imported mod lists the disc files it replaces with their sizes; when two mods that are both on replace the same disc file, a line says so (the lower one in the list wins, as the game reads them); the import runs on its own thread with a proper result dialog instead of a one-line toast; and each import's outcome — installed, or refused with its reason and specifics — is spelled out.
- **How to mod** rewritten for the phone: no PC in the recipe anymore.

## v0002F

Smooth motion, 0002D's touch back, the presenter as it was on PanVK, and mods that say what they did.

- **Smooth motion** (was "frame generation 2x", Settings → Graphics, on by default): a 30 fps game on a 120 Hz panel no longer holds each frame four refreshes — the gap is filled with a *ramp* of blends from the frame before to the new one, a step of the way between per refresh (quarters on a 120 Hz panel, thirds on 90, the halfway blend and the frame on 60). Motion is monotone instead of stepped, and the flicker fix of 0002E stands: no blending at all while the game runs at or near the panel's rate.
- **The touch is 0002D's again.** 0002E's cube-root response with 1.6x sensitivity and a vertical gain ran away from a light drag — ruined, as reported. The square-root response and the 115 dp full-tilt drag are back, the sensitivity 1.35x (a one-time migration moves the defaults; a value you set yourself stays), and nothing else about the look changed.
- **The PanVK present waits on the CPU again**, as it did before 0002E: the exemption that helped Turnip made the stutter *worse* on the Hot 50 4G's PanVK (an Infinix Hot 50 4G, Helio G100, Mali-G57 MC2 — noted, and the phone is what these builds are tuned for). Whatever its present returns, the wait is the safe side there.
- **Split mouths and twitching arms**: the vertex-alignment workaround that Arm's own Mali driver needs (a copy of misaligned vertex data on the render thread, Halo 2's characters among it) no longer runs under PanVK — it was measured on Arm's blob, and Mesa's driver fetches vertices its own way. If a face or a limb still splits, it is the 60 fps mod's own in-betweens drawing characters between the game's 30 updates — which is why that mod is now **off by default** (below), and one tap brings it back on a device that holds 60.
- **The 60 fps mod is off by default**: on a Mali-G57 MC2 the game at 60 dips and stutters worse than a locked 30 with smooth motion does, and its interpolated characters show in cutscenes. Where a device holds 60 and the in-betweens are wanted: Mods & cheats → 60 fps.
- **Pixel edges**: FXAA is on by default (Settings → Graphics still has it) — a 480p frame stretched over a 720p panel shows bare stairs without it.
- **Mods say what they did.** The Mods screen now reads the last run's log and says under each mod: **applied**, or **left off — this disc is another release of the game** (every patch's bytes are checked before applying, and a mod that doesn't hold is left off whole, as of 0002E; now it says so where you toggle it). That was the invisible half of "mods don't work": a mod made for another disc release applies nothing, silently, and now it names the reason.

## v0002E

Frame generation that knows when to blend, the presenter free-running on PanVK, an aim that tracks the thumb, and mods that hold.

- **Frame generation no longer flickers.** It blended after *any* refresh the game left empty — a game grazing the screen's rate (55-59 fps on 60 Hz) showed a ghosted double-exposure on and off through whole frames, each ghost delaying its frame a refresh: worse than nothing. It now keeps a smoothed measure of how far behind the display the game runs, and blends only while the game is *solidly* behind (a sustained gap, half rate and lower); a game at or near the panel's rate never blends, exactly as if it were off. The wasted frames go with it: a frame that arrived while its predecessor's blend was pending showed the newest frame only as a ghost — that path is closed.
- **No more stutter on PanVK** (Settings → Graphics → GPU driver): the presenter waited on the CPU for each of its passes to finish before presenting — a wait that only Qualcomm's own driver needs (Turnip and PanVK hand the compositor a sync file and return at once). On the Helio G100's Mali-G57 the window's thread sat behind the game's GPU work and missed refresh after refresh while the GPU was busy: hiccups, whole frames late. PanVK is now exempt from the wait, as Turnip always was.
- **The aim tracks the thumb.** The last word in "feels like a right stick" was the game's own turn curve: Halo 2 turns roughly cubic in the stick's deflection, so a medium drag sat where the game barely turns. The look's response now bends by the *cube root* — the bend and the game's curve cancel, and the camera turns in proportion to how fast the thumb moves. With the default sensitivity up to 1.6x, a full-tilt turn at a drag of ~800 dp/s held (was ~1370), and a 1.25x vertical gain (the game turns slower up and down, and there is less room to drag there), a medium swipe aims like it means it. The slider stays in Settings.
- **Mods hold, or don't apply at all.** A mod whose patch places hold other bytes on the disc (another release of the game) used to apply whichever of its lines happened to match — a half-patched game, its code and its data disagreeing, glitches of their own. A mod is now left off whole, with one line in the log saying so; and mods are found for a disc whose certificate version differs from the folder's (Halo 2's pressings differ in it), each patch still checked byte for byte. Cheats never cross releases — their addresses hold for one alone, and a cheat has no bytes to check.
- **Modded maps load.** Halo 2 caches its maps on the console's cache drives (X:, Y:, Z:) and keeps loading them from the cache, not the disc — a modded map over the disc's file showed only once that cache was cleared, the first thing a modded Xbox asked of its player. The cache drives are cleared now whenever the files the mods put over the disc change between runs (a map mod added, removed, or its file changed), so what the game cached is always what the mods give it. Savestates are safe: a state carries its own cache drives and puts them back as they were.

## v0002D

Frame generation 2x, the menu out of the thumbs' way, and the aim that was too slow.

- **Frame generation 2x** (Settings → Graphics, on by default): while the game runs below the screen's rate, every refresh it leaves empty shows the blend of the frames either side of it — the picture halfway between, one refresh early — and the frame itself follows in the gap. A 30 fps game on a 90 Hz panel shows 90 pictures a second, half of them the in-betweens: motion looks twice as smooth. The game's own frames, and their input, are untouched (this is the DOA2 120 fps mod's blend, at the presenter, for every game). A game that keeps up with the display never blends and nothing changes for it.
- **The FSR update**: FSR 1's sharpening (RCAS) is a setting of its own now — Soft, Normal (AMD's default), Strong (Settings → Graphics → FSR sharpening) — and FSR runs under frame generation too: the blended in-between is upscaled by EASU and sharpened by RCAS like any frame.
- **The menu stays out of the thumbs' way**: a swipe up from the bottom opened it, and a forward walk with the left stick read as one — mid-fight, every time. The swipe gestures are off by default (Settings has them still), and where they may start is outside the thumbs' zones alone. In their place, the **options button**: a small button at the top middle, one tap and the menu opens.
- **The aim was too slow** — Halo 2's turn speed is progressive, and a plain dead-zone lift left a medium drag at a deflection the game barely turns at. The look's response now bends (its square root): a medium drag turns as a good push of the stick, a flick is still the full rim, rest is still rest. With the default sensitivity up to 1.2× and the full-tilt drag down to 115 dp, a medium swipe aims like it means it — and not too fast either; the slider is still there in Settings.
- **Moving and aiming no longer cost frames**: the move stick's knob was redrawn — its display list recorded again, its gradients built anew — at every touch sample, at the panel's whole rate; on this phone's Mali driver the window's frames take turns with the game's on the GPU, so following the thumb cost the game frames. The knob now moves in fine steps (a couple of pixels at its size — the input itself stays exact), so it re-records only as it crosses one, and the overlays redraw at most 45 times a second whatever the GPU is doing (the busy-wait gate already did this on Turnip; now it holds everywhere).

## v0002C

Free look, PUBG's way: nothing drawn, no fps cost.

- **What was broken in 0002B, plainly**: the look drew a ring where the finger landed and redrew it **every frame** through the overlay's uncached gradient path — the same path this code's own comment warns took ~80% of a render core on a Galaxy Tab S9 while a stick was held. That was the lag and the dropped frames while aiming. And the ring was still "a stick to fight".
- **The right side is now a plain invisible touchpad** — from 42% across the screen, the whole height: drag anywhere a button isn't to aim, PUBG / COD Mobile's way. Nothing is drawn for the look: no ring, no knob, nothing.
- **The right stick's ring is not shown while free look is on** (Settings → *Free look*). Turn it off and the anchored stick is back (with 0002B's dead-zone lift, it works).
- **Aiming now redraws zero pixels**: the look state left the drawing hash and the pump; the turn-rate pump wakes only while the rate actually changes and stops dead when the finger rests or lifts. A held-still finger costs literally nothing.
- The camera stops the instant the finger lifts or rests (80 ms — tolerant of digitizers that batch samples); every movement still reaches the game above its dead zone (0002B's fix, kept).

## v0002B

The touch look actually works now.

- **What was broken in 0002A, plainly**: a normal aim swipe built up to ~0.13 stick deflection, and Halo 2 ignores everything under ~0.28 — its own stick dead zone. Slow and medium swipes did nothing at all; only hard flicks got through.
- **The fix, on the four points of the spec**: every deflection we mean at all is lifted over the game's dead zone on its way out (`lift`: the smallest movement goes as 0.28 at the least, the rim is still full tilt — and it goes for the move stick too, a gentle push now walks); the look's deltas are in dp and the turn rate's ease is set by the frame's own time (the same feel at 60 Hz and 120 Hz, on any panel); a finger still for 40 ms stops the camera on the next frame (no drift, no spin); a quick drag of ~140 dp held over the ease is a full-tilt turn, scaled by the sensitivity slider.
- **Modding instructions in the app**: the Mods screen now says in plain words how it works — maps and character models live inside the game's map files; export the modded `.map` from Assembly (keep its file name) and import it here; a `mod.txt` or a ZIP works too; Assembly's own `.mod` project files are for Assembly itself.

## v0002A

Xash3D's touchpad look, the native screen (the game rendered for the phone's whole panel), and the mod loader.

### Touch controls
- **The look is a touchpad, Xash3D's way** (its `in_touch.c`: the look is "like a touchpad" — every move of the finger turns the view by that movement, through `touch_yaw`/`touch_pitch`; no anchor, no rim, no dead zone). Through the Xbox's stick it is the finger's speed that is the deflection: each move adds to a turning rate that eases away as the finger stops (~100 ms), so the camera follows the finger at any pace and stops when it does. A finger-width drag aims that much, always. The ring stays where the finger landed, its knob showing the turning.

### The native screen
- **Halo 2 renders for the phone's whole panel.** The game's anamorphic factor — the Halo 2 HD patch's own address `0x45DFDC`, the camera FOV global beside it (`g_47136c = 1.f`) confirmed by the retail decompilation (kirklandsig/halo2-decompiled, 75.7% matched) — is patched at game start to this screen's own shape (its aspect × 3⁄4, computed from the panel at every start), and the **Native** aspect (the default, was "Stretch") lays the frame over the whole screen: the widened view and the stretch cancel out — **full screen, no bars, no crop, no distortion**. On by default as "Native camera" in Mods & cheats. Experimental, honestly so: the HUD is drawn for the wider shape too, and a title update's own XBE is not patched (a patch line whose bytes don't match is skipped and logged — nothing breaks, it just doesn't apply).
- Fit (the whole 16:9 picture) and Fill (crop) stay one tap away in Settings → Display.

### Mods
- **The mod loader**: Mods & cheats → **Import a mod** — a modded Halo cache map (`.map`, as Assembly and the Xbox map tools write them; keep the map's own file name and it replaces it on the disc — PC map mods' usual form), a `mod.txt` of XBE patches (mods/README.md's format), or a ZIP holding either (a mod.txt with its `files/` tree, or maps). Imported mods toggle like the built-ins, are removed with their button, and the game reads them at their next start. Import errors are named (not a mod / no patches in the text / unreadable / empty), and a failed import leaves nothing behind.

### Also
- FSR 1 (fixed in 1.7.0) keeps running under the native screen: the anamorphic frame upscaled by EASU, sharpened by RCAS.

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
