package com.rfandango.haku_x.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The mod browser: the sources that can be read without an account or a
 * captcha — this app's own catalog (bundled, refreshed from its repository
 * when the network says so), GameBanana's open API (the classic Halo 2
 * section and the collection's), the Internet Archive's search — and Nexus
 * Mods behind the player's own API key, which is the one place the
 * collection's whole mod scene (and its adult half) is listed. Every source
 * names its own files; what a file really is, the importer says when it is
 * here.
 */
object Marketplace {
  private const val TAG = "hakuX-market"
  private const val TIMEOUT_MS = 20_000
  private const val UA = "hailon2V1 (Android; mod browser)"
  /** Where the catalog is also kept fresh from, between app updates. */
  private const val REPO_CATALOG =
    "https://raw.githubusercontent.com/Kesurage/hailon2V1/main/android/app/src/main/assets/market/catalog.json"

  class MarketException(msg: String) : Exception(msg)

  /** What a mod is to this game, as well as it can be said before it is here. */
  enum class Compat { COMPATIBLE, PARTIAL, NOT, UNKNOWN }

  /** One file a mod hands out. */
  class MarketFile(val name: String, val size: Long, val url: String)

  /** One mod a source lists. */
  class MarketMod(
    /** Stable, per source ("gamebanana-143118"). */
    val id: String,
    val source: String,
    val name: String,
    val author: String,
    val description: String,
    /** Its page on the source, when it has one. */
    val pageUrl: String?,
    val files: List<MarketFile>,
    val adult: Boolean,
    val compat: Compat,
    val compatNote: String?,
    /** Its preview images, as the source hands them out (banners, screenshots). */
    val images: List<String> = emptyList(),
  )

  /** The sources the browser reads. */
  enum class Source(val key: String) {
    CATALOG("catalog"), GAMEBANANA_H2("gb-h2"), GAMEBANANA_MCC("gb-mcc"),
    ARCHIVE("archive"), NEXUS_MCC("nexus"), NEXUS_H2("nexus-h2");
  }

  private fun compatOf(s: String?): Compat = when (s) {
    "compatible" -> Compat.COMPATIBLE
    "partial" -> Compat.PARTIAL
    "not" -> Compat.NOT
    else -> Compat.UNKNOWN
  }

  /** What a file's name says it is, as well as a name can say. */
  fun compatFromFile(name: String): Pair<Compat, String> {
    val lower = name.lowercase()
    return when {
      lower.endsWith(".asmp") -> Compat.COMPATIBLE to "an Assembly patch — built onto the disc's own map here"
      lower.endsWith(".patchdat") || lower.endsWith(".ascpatch") ->
        Compat.COMPATIBLE to "an old tool's patch — built onto the disc's own map here"
      lower.endsWith(".map") ->
        Compat.PARTIAL to "a Halo cache map — the importer reads which build it is (the collection's are converted, experimental)"
      else -> Compat.UNKNOWN to "what is inside decides"
    }
  }

  /** GameBanana's preview media: its banners and screenshots, icons left out. */
  private fun gbImages(r: JSONObject): List<String> {
    val out = mutableListOf<String>()
    val imgs = r.optJSONObject("_aPreviewMedia")?.optJSONArray("_aImages") ?: return out
    for (i in 0 until imgs.length()) {
      val im = imgs.optJSONObject(i) ?: continue
      val type = im.optString("_sType")
      val url = im.optString("_sUrl")
      if (url.startsWith("http") && type != "icon" && !url.endsWith(".gif")) out += url
    }
    return out.distinct().take(6)
  }

  /* ---- the catalog ---- */

  /** This app's catalog, as its repository has it when that can be fetched,
   *  else the copy bundled in the APK. */
  suspend fun catalog(context: Context): List<MarketMod> = withContext(Dispatchers.IO) {
    val bundled = context.assets.open("market/catalog.json").bufferedReader().use { it.readText() }
    val fresh = try {
      fetchText(REPO_CATALOG)?.takeIf { it.length > 10 }
    } catch (_: Exception) {
      null
    }
    for (text in listOfNotNull(fresh, bundled)) {
      try {
        return@withContext parseCatalog(text)
      } catch (_: Exception) {
      }
    }
    throw MarketException("the catalog would not read")
  }

  private fun parseCatalog(text: String): List<MarketMod> {
    val root = JSONObject(text)
    val arr = root.optJSONArray("mods") ?: JSONArray()
    val out = mutableListOf<MarketMod>()
    for (i in 0 until arr.length()) {
      val m = arr.getJSONObject(i)
      val files = mutableListOf<MarketFile>()
      val fa = m.optJSONArray("files") ?: JSONArray()
      for (j in 0 until fa.length()) {
        val f = fa.getJSONObject(j)
        files += MarketFile(f.optString("name", "file"), f.optLong("size", 0), f.optString("url", ""))
      }
      if (files.isEmpty()) continue
      out += MarketMod(
        id = m.optString("id", ""), source = m.optString("source", "Catalog"),
        name = m.optString("name", ""), author = m.optString("author", ""),
        description = m.optString("description", ""), pageUrl = m.optString("page").ifEmpty { null },
        files = files, adult = m.optBoolean("adult", false),
        compat = compatOf(m.optString("compat")),
        compatNote = m.optString("compatNote").ifEmpty { null },
        images = m.optJSONArray("images")?.let { a ->
          (0 until a.length()).mapNotNull { i -> a.optString(i).ifEmpty { null } }
        } ?: emptyList(),
      )
    }
    if (out.isEmpty()) throw MarketException("the catalog is empty")
    return out
  }

  /* ---- GameBanana (its open API) ---- */

  private const val GB_H2 = 190L
  private const val GB_MCC = 7714L

  /** A game's mods, newest first (GameBanana's open API; no account). */
  suspend fun gamebanana(mcc: Boolean, page: Int): List<MarketMod> = withContext(Dispatchers.IO) {
    val game = if (mcc) GB_MCC else GB_H2
    val text = fetchText(
      "https://gamebanana.com/apiv11/Mod/Index?_aFilters%5BGeneric_Game%5D=$game&_nPage=${page + 1}&_nPerpage=50&_csvFields=_sName,_aSubmitter,_sText,_tsDateAdded,_bIsNsfw")
      ?: throw MarketException("GameBanana did not answer")
    val arr = JSONObject(text).optJSONArray("_aRecords")
      ?: throw MarketException("GameBanana's answer was not a list")
    val out = mutableListOf<MarketMod>()
    for (i in 0 until arr.length()) {
      val r = arr.getJSONObject(i)
      val id = r.optLong("_idRow")
      if (id == 0L) continue
      val cat = r.optJSONObject("_aCategory")
      val adult = r.optBoolean("_bIsNsfw", false) || cat?.optBoolean("_bIsNsfw", false) == true
      out += MarketMod(
        id = "gamebanana-$id", source = "GameBanana",
        name = r.optString("_sName", "?"), author = r.optJSONObject("_aSubmitter")?.optString("_sName").orEmpty(),
        description = r.optString("_sText").ifEmpty { r.optString("_sDescription") },
        pageUrl = r.optString("_sProfileUrl").ifEmpty { "https://gamebanana.com/mods/$id" },
        files = emptyList(),  /* its files come with its profile, when it is opened */
        adult = adult, compat = Compat.UNKNOWN,
        compatNote = "what its files say, when it is opened",
        images = gbImages(r),
      )
    }
    out
  }

  /** One GameBanana mod, with its files and their downloads. */
  suspend fun gamebananaMod(id: Long): MarketMod = withContext(Dispatchers.IO) {
    val text = fetchText("https://gamebanana.com/apiv11/Mod/$id/ProfilePage")
      ?: throw MarketException("GameBanana did not answer")
    val p = JSONObject(text)
    if (p.optBoolean("_bIsTrashed") || p.optBoolean("_bIsWithheld")) {
      throw MarketException("that mod is gone from GameBanana")
    }
    val files = mutableListOf<MarketFile>()
    val fa = p.optJSONArray("_aFiles") ?: JSONArray()
    for (j in 0 until fa.length()) {
      val f = fa.getJSONObject(j)
      files += MarketFile(f.optString("_sFile", "file"), f.optLong("_nFilesize", 0), f.optString("_sDownloadUrl", ""))
    }
    if (files.isEmpty()) throw MarketException("it has no files to take")
    val cat = p.optJSONObject("_aCategory")
    MarketMod(
      id = "gamebanana-$id", source = "GameBanana",
      name = p.optString("_sName", "?"), author = p.optJSONObject("_aSubmitter")?.optString("_sName").orEmpty(),
      description = p.optString("_sText").ifEmpty { p.optString("_sDescription") },
      pageUrl = "https://gamebanana.com/mods/$id", files = files,
      adult = p.optBoolean("_bIsNsfw", false) || cat?.optBoolean("_bIsNsfw", false) == true,
      compat = Compat.UNKNOWN, compatNote = null,
      images = gbImages(p),
    )
  }

  /* ---- the Internet Archive (its open search) ---- */

  /** The Archive's search over Halo 2 mod uploads. */
  suspend fun archive(query: String, page: Int): List<MarketMod> = withContext(Dispatchers.IO) {
    val q = URLEncoder.encode(
      "(halo 2) AND (mod OR mods OR map) AND mediatype:software" +
        (if (query.isBlank()) "" else " AND ($query)"), "UTF-8")
    val text = fetchText(
      "https://archive.org/advancedsearch.php?q=$q&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=description" +
        "&fl%5B%5D=downloads&rows=30&page=${page + 1}&output=json")
      ?: throw MarketException("the Internet Archive did not answer")
    val docs = JSONObject(text).optJSONObject("response")?.optJSONArray("docs")
      ?: throw MarketException("the Archive's answer was not a list")
    val out = mutableListOf<MarketMod>()
    for (i in 0 until docs.length()) {
      val d = docs.getJSONObject(i)
      val id = d.optString("identifier")
      if (id.isEmpty()) continue
      out += MarketMod(
        id = "archive-$id", source = "Internet Archive",
        name = d.optString("title", id), author = "",
        description = when (val desc = d.opt("description")) {
          is String -> desc
          is JSONArray -> (0 until desc.length()).joinToString("\n") { desc.optString(it) }
          else -> ""
        },
        pageUrl = "https://archive.org/details/$id", files = emptyList(),
        adult = false, compat = Compat.UNKNOWN,
        compatNote = "what its files say, when it is opened",
        images = listOf("https://archive.org/services/img/$id"),
      )
    }
    out
  }

  /** One Archive item, with its files (archive.org/download/<item>/<file>). */
  suspend fun archiveItem(id: String): MarketMod = withContext(Dispatchers.IO) {
    val text = fetchText("https://archive.org/metadata/$id")
      ?: throw MarketException("the Internet Archive did not answer")
    val root = JSONObject(text)
    val server = root.optJSONArray("servers")?.optJSONObject(0)?.optString("server").orEmpty()
    val dir = root.optString("dir", "/$id")
    val files = mutableListOf<MarketFile>()
    val fa = root.optJSONArray("files") ?: JSONArray()
    for (i in 0 until fa.length()) {
      val f = fa.getJSONObject(i)
      val name = f.optString("name")
      if (name.isEmpty() || f.optString("source") != "original") continue
      val size = f.optLong("size", 0)
      files += MarketFile(name, size, "https://archive.org/download/$id/$name")
    }
    if (files.isEmpty()) throw MarketException("it has no files to take")
    MarketMod(
      id = "archive-$id", source = "Internet Archive",
      name = root.optJSONObject("metadata")?.optString("title", id) ?: id, author = "",
      description = root.optJSONObject("metadata")?.optString("description", "").orEmpty(),
      pageUrl = "https://archive.org/details/$id", files = files,
      adult = false, compat = Compat.UNKNOWN, compatNote = null,
    )
  }

  /* ---- Nexus Mods (the player's own key; the collection's whole scene) ---- */

  private const val NEXUS_API = "https://api.nexusmods.com/v1"

  /** A Nexus game's mods (adult ones only when asked; the key's account
   *  decides what it may see). */
  suspend fun nexus(domain: String, key: String, adult: Boolean, page: Int): List<MarketMod> =
    withContext(Dispatchers.IO) {
      val text = fetchText(
        "$NEXUS_API/games/$domain/mods.json?include_adult=${if (adult) "true" else "false"}" +
          "&sort=downloads&order=desc&page=${page + 1}&results_per_page=50",
        headers = mapOf("APIKEY" to key, "Accept" to "application/json"))
        ?: throw MarketException("Nexus Mods did not answer — the key, or the account")
      val arr = JSONArray(text)
      val out = mutableListOf<MarketMod>()
      for (i in 0 until arr.length()) {
        val m = arr.getJSONObject(i)
        val id = m.optLong("mod_id")
        out += MarketMod(
          id = "nexus-$domain-$id", source = "Nexus Mods",
          name = m.optString("name", "?"), author = m.optString("author", ""),
          description = m.optString("summary", ""),
          pageUrl = "https://www.nexusmods.com/$domain/mods/$id", files = emptyList(),
          adult = m.optBoolean("adult_content", false),
          compat = Compat.UNKNOWN, compatNote = "what its files say, when it is opened",
          images = m.optString("picture_url").ifEmpty { null }?.let { listOf(it) } ?: emptyList(),
        )
      }
      out
    }

  /** One Nexus mod's files. */
  suspend fun nexusMod(domain: String, key: String, modId: Long): MarketMod = withContext(Dispatchers.IO) {
    val text = fetchText("$NEXUS_API/games/$domain/mods/$modId.json", headers = mapOf("APIKEY" to key))
      ?: throw MarketException("Nexus Mods did not answer — the key, or the account")
    val m = JSONObject(text)
    val filesText = fetchText("$NEXUS_API/games/$domain/mods/$modId/files.json", headers = mapOf("APIKEY" to key))
      ?: throw MarketException("its file list would not come")
    val fa = JSONObject(filesText).optJSONArray("files") ?: JSONArray()
    val files = mutableListOf<MarketFile>()
    for (i in 0 until fa.length()) {
      val f = fa.getJSONObject(i)
      val fid = f.optLong("file_id")
      files += MarketFile(f.optString("name", "file"), f.optLong("size", 0), "nexus:$domain:$modId:$fid")
    }
    if (files.isEmpty()) throw MarketException("it has no files to take")
    MarketMod(
      id = "nexus-$domain-$modId", source = "Nexus Mods",
      name = m.optString("name", "?"), author = m.optString("author", ""),
      description = m.optString("summary", ""),
      pageUrl = "https://www.nexusmods.com/$domain/mods/$modId",
      files = files, adult = m.optBoolean("contains_adult_content", false),
      compat = Compat.UNKNOWN, compatNote = null,
      images = m.optString("picture_url").ifEmpty { null }?.let { listOf(it) } ?: emptyList(),
    )
  }

  /** A Nexus file's own download link (the key's account may take it). */
  suspend fun nexusLink(domain: String, key: String, modId: Long, fileId: Long): String =
    withContext(Dispatchers.IO) {
      val text = fetchText("$NEXUS_API/games/$domain/mods/$modId/files/$fileId/download_link.json",
        headers = mapOf("APIKEY" to key))
        ?: throw MarketException("the download link would not come — the key, the account, or its patience")
      val arr = JSONArray(text)
      for (i in 0 until arr.length()) {
        val link = arr.getJSONObject(i).optString("link")
        if (link.startsWith("http")) return@withContext link
      }
      throw MarketException("the download link was not in the answer")
    }

  /* ---- the download itself ---- */

  /** A file taken to disk, with how far it has come (of how much). */
  suspend fun download(url: String, to: File, headers: Map<String, String> = emptyMap(),
                       progress: (Long, Long) -> Unit = { _, _ -> }): File = withContext(Dispatchers.IO) {
    val conn = open(url, headers)
    try {
      if (conn.responseCode != HttpURLConnection.HTTP_OK) {
        throw MarketException("the download answered ${conn.responseCode}")
      }
      val total = conn.contentLengthLong
      to.parentFile?.mkdirs()
      var got = 0L
      conn.inputStream.use { input ->
        FileOutputStream(to).use { output ->
          val buf = ByteArray(64 * 1024)
          while (true) {
            val n = input.read(buf)
            if (n < 0) break
            output.write(buf, 0, n)
            got += n
            progress(got, total)
          }
        }
      }
      if (to.length() == 0L) throw MarketException("the download came out empty")
      to
    } finally {
      conn.disconnect()
    }
  }

  /* ---- the plumbing ---- */

  /** An image's bytes (the previews), or null whatever went wrong. */
  fun fetchBytes(url: String): ByteArray? = try {
    val conn = open(url, emptyMap())
    try {
      if (conn.responseCode != HttpURLConnection.HTTP_OK) null
      else conn.inputStream.use { it.readBytes() }
    } finally {
      conn.disconnect()
    }
  } catch (_: Exception) {
    null
  }

  private fun fetchText(url: String, headers: Map<String, String> = emptyMap()): String? {
    val conn = open(url, headers)
    try {
      if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
      return conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
      conn.disconnect()
    }
  }

  private fun open(url: String, headers: Map<String, String>): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
      connectTimeout = TIMEOUT_MS
      readTimeout = TIMEOUT_MS
      setRequestProperty("User-Agent", UA)
      for ((k, v) in headers) setRequestProperty(k, v)
    }
}
