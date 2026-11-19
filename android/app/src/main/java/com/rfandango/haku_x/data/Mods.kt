package com.rfandango.haku_x.data

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import android.view.WindowManager
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * A mod (mods/README.md): its folder name, what it says about itself, its XBE
 * patches (file, line) and the vblank rate it asks for (0: the video mode's).
 */
data class ModInfo(
  val id: String,
  val name: String,
  val description: String,
  val author: String,
  val patches: List<Pair<String, String>>,
  val refresh: Int = 0,
)

/** A cheat (mods/README.md): its name, note, once, and its code lines as written. */
data class CheatInfo(val name: String, val note: String, val once: Boolean, val code: List<String>)

/** A disc's mods and cheats. */
class GameMods(val mods: List<ModInfo>, val cheats: List<CheatInfo>) {
  val isEmpty get() = mods.isEmpty() && cheats.isEmpty()
}

/**
 * The repository's mods/ folder, packed as the APK's assets/mods: a folder
 * per game key (GameOverrides.gameKey: title ID and certificate version,
 * 54430006.6), a cheats.txt and a folder per mod in it.
 */
object ModCatalog {
  private const val ROOT = "mods"
  private const val TAG = "hakuX-mods"
  /** Game key (upper case) -> its folder's name as the repository has it. */
  @Volatile private var folders: Map<String, String>? = null

  private fun folders(context: Context): Map<String, String> = folders ?: run {
    val map = context.assets.list(ROOT).orEmpty().associateBy { it.uppercase(Locale.ROOT) }
    folders = map
    map
  }

  private fun folder(context: Context, gameKey: String) = folders(context)[gameKey.uppercase(Locale.ROOT)]

  /** Whether the disc with this game key has mods or cheats (its own or imported). */
  fun has(context: Context, gameKey: String) =
    folder(context, gameKey) != null || UserMods.list(context, gameKey).isNotEmpty()

  /** The disc's mods (by folder name; the app's own, then the imported ones) and cheats (in file order). */
  fun load(context: Context, gameKey: String): GameMods {
    val dir = folder(context, gameKey)
    val mods = if (dir == null) mutableListOf() else
      am(context).list("$ROOT/$dir").orEmpty().sorted().mapNotNull { id ->
        text(am(context), "$ROOT/$dir/$id/mod.txt")?.let { parseMod(id, it) }
      }.toMutableList()
    mods += UserMods.list(context, gameKey)
    val cheats = dir?.let { text(am(context), "$ROOT/$it/cheats.txt") }?.let { parseCheats(it) }.orEmpty()
    return GameMods(mods, cheats)
  }

  private fun am(context: Context) = context.assets

  /** The mod.txt parser, for the imported mods too (UserMods). */
  fun parseModPublic(id: String, text: String): ModInfo = parseMod(id, text)

  private fun text(am: AssetManager, path: String): String? =
    runCatching { am.open(path).use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()

  private fun content(line: String) = line.trim().takeUnless { it.isEmpty() || it.startsWith("#") }

  private fun header(line: String): Pair<String, String>? {
    val colon = line.indexOf(':')
    if (colon <= 0) return null
    val key = line.substring(0, colon).trim().lowercase(Locale.ROOT)
    return if (key.all { it.isLetter() }) key to line.substring(colon + 1).trim() else null
  }

  private fun parseMod(id: String, text: String): ModInfo {
    val fields = mutableMapOf<String, String>()
    val patches = mutableListOf<Pair<String, String>>()
    var xbe: String? = null
    for (raw in text.lines()) {
      val line = content(raw) ?: continue
      if (line.startsWith("[") && line.endsWith("]")) {
        xbe = line.substring(1, line.length - 1).trim()
        continue
      }
      val x = xbe
      if (x != null) {
        patches += x to line
      } else {
        header(line)?.let { (k, v) -> fields[k] = v }
      }
    }
    return ModInfo(
      id = id,
      name = fields["name"]?.takeIf { it.isNotEmpty() } ?: id,
      description = fields["description"].orEmpty(),
      author = fields["author"].orEmpty(),
      patches = patches,
      refresh = fields["refresh"]?.toIntOrNull() ?: 0,
    )
  }

  private fun parseCheats(text: String): List<CheatInfo> {
    val out = mutableListOf<CheatInfo>()
    var name: String? = null
    var note = ""
    var once = false
    val code = mutableListOf<String>()
    fun flush() {
      name?.let { out += CheatInfo(it, note, once, code.toList()) }
      note = ""
      once = false
      code.clear()
    }
    for (raw in text.lines()) {
      val line = content(raw) ?: continue
      if (line.startsWith("[") && line.endsWith("]")) {
        flush()
        name = line.substring(1, line.length - 1).trim()
        continue
      }
      if (name == null) continue
      val h = header(line)
      when {
        h?.first == "note" -> note = h.second
        line.equals("once", ignoreCase = true) -> once = true
        else -> code += line
      }
    }
    flush()
    return out
  }

  /**
   * A mod's files (assets mods/<game>/<mod>/files/...) in the app's storage
   * for the core to read, extracted again after the app changed: their disc
   * paths and host files.
   */
  fun extract(context: Context, gameKey: String, mod: String): List<Pair<String, File>> {
    val dir = folder(context, gameKey) ?: return emptyList()
    val am = context.assets
    val src = "$ROOT/$dir/$mod/files"
    val dst = File(context.filesDir, "mods/${gameKey.uppercase(Locale.ROOT)}/$mod")
    val files = File(dst, "files")
    val stamp = File(dst, ".stamp")
    val app = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()
    if (stamp.isFile && stamp.readText() == app) {
      return files.walkTopDown().filter { it.isFile }.map { it.relativeTo(files).path to it }.toList()
    }
    dst.deleteRecursively()
    val out = mutableListOf<Pair<String, File>>()
    fun copy(asset: String, rel: String) {
      val children = am.list(asset).orEmpty()
      if (children.isEmpty()) {
        val target = File(files, rel)
        target.parentFile?.mkdirs()
        runCatching { am.open(asset).use { i -> target.outputStream().use { i.copyTo(it) } } }
          .onSuccess { out += rel to target }
          .onFailure { Log.w(TAG, "$asset: ${it.message}") }
        return
      }
      for (c in children) copy("$asset/$c", if (rel.isEmpty()) c else "$rel/$c")
    }
    if (am.list(src).orEmpty().isNotEmpty()) copy(src, "")
    dst.mkdirs()
    stamp.writeText(app)
    return out
  }
}

/**
 * One disc's choices: its mods on or off, its cheats on or off, by name. In
 * one JSON file ("<game>/mod/<id>" or "<game>/cheat/<name>" -> true) both
 * processes read afresh and write whole, renamed into place, as Controllers:
 * the game's menu toggles cheats in its own process, and shared preferences
 * served the Mods screen its cached copy, whose next toggle wrote the
 * game's changes away.
 */
class ModChoices(context: Context, private val game: String) {
  private val file = File(context.filesDir, FILE)
  private val app = context.applicationContext

  /**
   * A mod's state. The 60 fps patch and the native camera are on from the
   * start for Halo 2 — the game this launcher is for; the choice once made
   * (either way) is kept, like every other mod's.
   */
  fun mod(id: String) = read().optBoolean("$game/mod/$id", defaultOn(id))
  fun setMod(id: String, on: Boolean) = write("$game/mod/$id", on)
  fun cheat(name: String) = read().optBoolean("$game/cheat/$name", false)
  fun setCheat(name: String, on: Boolean) = write("$game/cheat/$name", on)

  private fun defaultOn(id: String) = isHalo2() && (id == "60fps" || id == NATIVE_CAMERA)

  /** This game is a Halo 2 release (its key's title ID is one of theirs). */
  private fun isHalo2() = game.substringBefore('.') in HALO2_TITLE_IDS

  private fun read(): JSONObject = synchronized(LOCK) {
    if (!file.exists()) {
      // the choices the shared preferences (per_game_mods) held before; the file is left as it was
      val old = app.getSharedPreferences(NAME, Context.MODE_PRIVATE).all
      return JSONObject().apply { for ((k, v) in old) if (v is Boolean) put(k, v) }
    }
    try {
      JSONObject(file.readText())
    } catch (_: Exception) {
      JSONObject()
    }
  }

  private fun write(key: String, on: Boolean) {
    synchronized(LOCK) {
      val o = read()
      if (on) o.put(key, true) else o.remove(key)
      val tmp = File(file.parentFile, "$FILE.${android.os.Process.myPid()}.tmp")
      tmp.writeText(o.toString())
      tmp.renameTo(file)
    }
  }

  companion object {
    const val NAME = "per_game_mods"
    private const val FILE = "mod_choices.json"
    private val LOCK = Any()
    /** The native camera patch's id (ModRun writes it for Halo 2 itself). */
    const val NATIVE_CAMERA = "native_camera"
    /** Halo 2's releases (the mods/ folders with its 60 fps patches): NTSC, PAL and the regional ones. */
    val HALO2_TITLE_IDS = setOf("4D530064", "4D53004B", "4D530051", "4D53006E")
  }
}

/**
 * The run's manifest for the core (native/core/mods.h): the enabled mods'
 * files and XBE patches, every cheat with its state. The cheats in their
 * order here are the in-game menu's (nativeSetCheat takes the index).
 */
object ModRun {
  private const val TAG = "hakuX-mods"

  /** The manifest written (null: the game has none), and the run's cheats. */
  fun write(context: Context, gameKey: String): Pair<File, List<CheatInfo>>? {
    val game = ModCatalog.load(context, gameKey)
    val native = gameKey.substringBefore('.') in ModChoices.HALO2_TITLE_IDS
    if (game.isEmpty && !native) return null
    val choices = ModChoices(context, gameKey)
    val lines = mutableListOf<String>()
    val on = game.mods.filter { choices.mod(it.id) }
    for (mod in on) {
      val files = if (mod.id.startsWith(UserMods.PREFIX)) {
        UserMods.files(context, gameKey, mod.id)
      } else {
        ModCatalog.extract(context, gameKey, mod.id)
      }
      for ((disc, file) in files) {
        lines += "file\t${disc.replace('/', '\\')}\t${file.absolutePath}"
      }
    }
    for (mod in game.mods) {
      // a mod that is off too: a savestate saved with it on gets the disc's bytes back
      val kind = if (mod in on) "patch" else "patchoff"
      for ((xbe, line) in mod.patches) lines += "$kind\t$xbe\t${mod.id}\t$line"
    }
    on.maxOfOrNull { it.refresh }?.takeIf { it > 0 }?.let { lines += "refresh\t$it" }
    /* Halo 2's native camera (experimental): the game's anamorphic factor
     * widened to this phone's screen, so it renders the view for the whole
     * panel — no bars, no crop, no stretch under the Native aspect */
    val onCamera = native && choices.mod(ModChoices.NATIVE_CAMERA)
    if (native) {
      for (line in nativeCameraLines(screenAspect(context))) {
        lines += "${if (onCamera) "patch" else "patchoff"}\tdefault.xbe\t${ModChoices.NATIVE_CAMERA}\t$line"
      }
    }
    for (c in game.cheats) {
      lines += "cheat\t${if (choices.cheat(c.name)) 1 else 0}\t${if (c.once) 1 else 0}\t${c.name}"
      for (l in c.code) lines += "code\t$l"
    }
    val file = File(context.filesDir, "native/mods.txt")
    file.parentFile?.mkdirs()
    file.writeText(lines.joinToString("\n", postfix = "\n"))
    Log.i(TAG, "$gameKey: ${on.size} of ${game.mods.size} mods on, ${game.cheats.size} cheats" +
      (if (native) ", native camera ${if (onCamera) "on" else "off"}" else ""))
    return file to game.cheats
  }

  /* The phone's panel, at its longest over its shortest: the window lies
   * along it in the game's landscape. */
  private fun screenAspect(context: Context): Float {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return 16f / 9f
    return if (android.os.Build.VERSION.SDK_INT >= 30) {
      val b = wm.maximumWindowMetrics.bounds
      maxOf(b.width(), b.height()).toFloat() / minOf(b.width(), b.height())
    } else {
      val m = android.util.DisplayMetrics()
      @Suppress("DEPRECATION") wm.defaultDisplay?.getRealMetrics(m)
      if (m.heightPixels > 0) maxOf(m.widthPixels, m.heightPixels).toFloat() / minOf(m.widthPixels, m.heightPixels)
      else 16f / 9f
    }
  }

  /**
   * Halo 2's anamorphic factor (the Halo 2 HD patch's own address, whose
   * way of it the camera FOV global beside it — g_47136c, confirmed by the
   * retail decompilation — vouches for): the aspect the game renders for,
   * over its 640x480 frame's own 4:3. Stock widescreen is 4/3 (a 16:9 view
   * squeezed into 4:3 pixels); this writes the phone's panel instead, and
   * the Native aspect shows the frame over the whole screen — the widened
   * view and the stretch cancel out: full screen, no crop, no distortion.
   * Two lines: the factor's stock value is written either way at its data
   * place (the one that doesn't match is skipped and logged, harmless).
   */
  private fun nativeCameraLines(aspect: Float): List<String> {
    val factor = aspect * 0.75f
    val bytes = floatBytes(factor)
    return listOf(
      "0x0045DFDC: AB AA AA 3F -> $bytes",  /* 4/3, the widescreen squeeze */
      "0x0045DFDC: 00 00 80 3F -> $bytes",  /* or a plain 1.0 there */
    )
  }

  /** A float as the patch format's bytes: little-endian, hex pairs. */
  private fun floatBytes(f: Float): String =
    f.toBits().toString(16).padStart(8, '0')
      .chunked(2).reversed().joinToString(" ") { it.uppercase(Locale.ROOT) }
}

/**
 * Mods brought by the player (Mods screen → Import): the same shape as the
 * app's own — a folder with a mod.txt (its name, description and XBE
 * patches, mods/README.md's format) and a files/ that mirrors the disc —
 * kept in the app's storage, one place per game. A modded Halo cache map
 * (.map, as Assembly or the Xbox map tools write them) becomes one itself:
 * keep the map's own file name and it replaces it on the disc.
 */
object UserMods {
  const val PREFIX = "u_"
  private const val TAG = "hakuX-mods"

  private fun dir(context: Context, gameKey: String) =
    File(context.filesDir, "mods_user/${gameKey.uppercase(Locale.ROOT)}")

  /** The imported mods of a game, by folder name. */
  fun list(context: Context, gameKey: String): List<ModInfo> {
    val d = dir(context, gameKey)
    return d.listFiles()?.orEmpty()
      ?.filter { it.isDirectory && File(it, "mod.txt").isFile }
      ?.sortedBy { it.name }
      ?.mapNotNull { ModCatalog.parseModPublic(it.name, File(it, "mod.txt").readText()) }
      .orEmpty()
  }

  /** A mod's files (its files/ tree): their disc paths and host files. */
  fun files(context: Context, gameKey: String, id: String): List<Pair<String, File>> {
    val root = File(dir(context, gameKey), "$id/files")
    if (!root.isDirectory) return emptyList()
    return root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).path to it }.toList()
  }

  /** Why an import was refused. */
  enum class ImportError { UNREADABLE, NOT_A_MOD, NOT_A_PATCH, EMPTY }

  /** import()'s outcome: the mod added (and on), or why not. */
  data class ImportResult(val mod: ModInfo? = null, val error: ImportError? = null)

  /**
   * The picked file in: a .map (a modded Halo cache map — its file name is
   * the map it replaces), a mod.txt of XBE patches, or a ZIP holding
   * either (a mod.txt with a files/ tree, or maps). Added and turned on;
   * the caller says what came of it.
   */
  fun import(context: Context, gameKey: String, uri: android.net.Uri): ImportResult {
    val name = queryName(context, uri) ?: return ImportResult(error = ImportError.UNREADABLE)
    val base = name.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9._ -]+"), "_").trim().ifEmpty { "mod" }
    val lower = name.lowercase(Locale.ROOT)
    val dir = dir(context, gameKey)
    try {
      when {
        lower.endsWith(".map") -> {
          val file = File(dir, "$PREFIX$base/files/maps/$name")
          copy(context, uri, file)
          writeModTxt(dir, "$PREFIX$base", name, "A modded cache map. It replaces maps\\$name on the disc — the game loads it by that name.")
        }
        lower.endsWith(".txt") -> {
          val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: return ImportResult(error = ImportError.UNREADABLE)
          if (!looksLikeAPatch(text)) return ImportResult(error = ImportError.NOT_A_PATCH)
          writeModTxt(dir, "$PREFIX$base", name, "XBE patches.")
          File(dir, "$PREFIX$base/mod.txt").writeText(text)
        }
        lower.endsWith(".zip") -> {
          val tmp = File(context.cacheDir, "import_mod.zip")
          copy(context, uri, tmp)
          ZipFile(tmp).use { zip ->
            val entries = zip.entries().asSequence().filter { !it.isDirectory }.toList()
            val modTxt = entries.firstOrNull { File(it.name).name.equals("mod.txt", ignoreCase = true) }
            if (modTxt != null) {
              /* the prefix its files/ (and any of its files) sit under */
              val pre = modTxt.name.substringBeforeLast('/').let { if (it.isEmpty()) "" else "$it/" }
              val target = File(dir, "$PREFIX$base")
              for (e in entries) {
                if (!e.name.startsWith(pre)) continue
                val rel = e.name.substring(pre.length)
                if (rel.isEmpty()) continue
                val out = File(target, rel)
                if (!out.canonicalPath.startsWith(target.canonicalPath + File.separator)) {
                  continue /* a path out of the package */
                }
                out.parentFile?.mkdirs()
                zip.getInputStream(e).use { input -> java.io.FileOutputStream(out).use { input.copyTo(it) } }
              }
              tmp.delete()
            } else {
              /* no mod.txt: the maps in it (at the root or one folder deep) */
              val maps = entries.filter {
                it.name.lowercase(Locale.ROOT).endsWith(".map") && it.name.count { c -> c == '/' } <= 1
              }
              if (maps.isEmpty()) {
                tmp.delete()
                return ImportResult(error = ImportError.NOT_A_MOD)
              }
              for (e in maps) {
                val out = File(dir, "$PREFIX$base/files/maps/${File(e.name).name}")
                out.parentFile?.mkdirs()
                zip.getInputStream(e).use { input -> java.io.FileOutputStream(out).use { input.copyTo(it) } }
              }
              tmp.delete()
              val names = maps.joinToString(", ") { File(it.name).name }
              writeModTxt(dir, "$PREFIX$base", name, "Modded cache maps: $names. Each replaces the map of its own name on the disc.")
            }
          }
        }
        else -> return ImportResult(error = ImportError.NOT_A_MOD)
      }
    } catch (e: Exception) {
      Log.e(TAG, "import $name: ${e.message}")
      /* a half-written mod is no mod: it goes, and the error is the answer */
      File(dir, "$PREFIX$base").deleteRecursively()
      return ImportResult(error = ImportError.UNREADABLE)
    }
    val mod = list(context, gameKey).firstOrNull { it.id == "$PREFIX$base" }
      ?: return ImportResult(error = ImportError.EMPTY)
    ModChoices(context, gameKey).setMod(mod.id, true)
    return ImportResult(mod)
  }

  /** An imported mod gone (its files too); true if it was on. */
  fun delete(context: Context, gameKey: String, id: String): Boolean {
    val wasOn = ModChoices(context, gameKey).mod(id)
    ModChoices(context, gameKey).setMod(id, false)
    File(dir(context, gameKey), id).deleteRecursively()
    return wasOn
  }

  /** A mod.txt written for an imported one (a map, or a zip's files): its own words kept for the patch kinds. */
  private fun writeModTxt(dir: File, id: String, name: String, description: String) {
    val f = File(dir, "$id/mod.txt")
    f.parentFile?.mkdirs()
    f.writeText("name: $name\ndescription: $description\nauthor: \n")
  }

  /** A text that patch lines are in: a [file] section and an address line, or no [file] and a 0x...: line at least. */
  private fun looksLikeAPatch(text: String): Boolean {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
    val hasSection = lines.any { it.startsWith("[") && it.endsWith("]") }
    val hasAddress = lines.any { it.startsWith("0x") && it.contains(':') }
    return hasSection && hasAddress
  }

  private fun copy(context: Context, uri: android.net.Uri, to: File) {
    to.parentFile?.mkdirs()
    context.contentResolver.openInputStream(uri)?.use { input ->
      java.io.FileOutputStream(to).use { output -> input.copyTo(output) }
    } ?: throw java.io.IOException("unreadable")
    if (to.length() == 0L) throw java.io.IOException("empty")
  }

  private fun queryName(context: Context, uri: android.net.Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
      if (it.moveToFirst()) it.getString(0) else null
    }
  }.getOrNull()
}
