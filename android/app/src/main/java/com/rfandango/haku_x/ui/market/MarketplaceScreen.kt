package com.rfandango.haku_x.ui.market

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.Marketplace
import com.rfandango.haku_x.data.UserMods
import com.rfandango.haku_x.ui.common.Hint
import com.rfandango.haku_x.ui.common.SubScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The mod browser: the sources that need no account (this app's catalog,
 * GameBanana, the Internet Archive) plus Nexus Mods behind the player's own
 * API key — where the collection's whole scene lives. Every entry says how
 * well it can work here; a take is download → check (the importer reads what
 * the file really is: the disc's own build, the collection's — converted
 * here — an old tool's patch) → installed and turned on, and the take's word
 * says how each step came out. What lands is an ordinary imported mod: on,
 * off or gone from the Mods screen like any other.
 */
@Composable
fun MarketplaceScreen(gameKey: String, title: String, onBack: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbar = remember { SnackbarHostState() }
  val prefs = remember { context.getSharedPreferences("market", 0) }

  var source by remember { mutableStateOf(Marketplace.Source.CATALOG) }
  var query by remember { mutableStateOf("") }
  var adult by remember { mutableStateOf(prefs.getBoolean("adult", false)) }
  var nexusKey by remember { mutableStateOf(prefs.getString("nexus_key", "").orEmpty()) }
  var keyField by remember { mutableStateOf(nexusKey) }
  val mods = remember { mutableStateListOf<Marketplace.MarketMod>() }
  var loading by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var page by remember { mutableStateOf(0) }
  var opened by remember { mutableStateOf<Marketplace.MarketMod?>(null) }
  var busyFile by remember { mutableStateOf<String?>(null) }
  var dlProgress by remember { mutableStateOf<Pair<Long, Long>?>(null) }
  var result by remember { mutableStateOf<UserMods.ImportResult?>(null) }

  fun load(reset: Boolean) {
    val nexus = source == Marketplace.Source.NEXUS_MCC || source == Marketplace.Source.NEXUS_H2
    if (nexus && nexusKey.isBlank()) {
      mods.clear()
      error = null
      return /* the key row says the rest */
    }
    scope.launch {
      loading = true
      error = null
      if (reset) {
        page = 0
        mods.clear()
      }
      try {
        val list = withContext(Dispatchers.IO) {
          when (source) {
            Marketplace.Source.CATALOG -> Marketplace.catalog(context)
            Marketplace.Source.GAMEBANANA_H2 -> Marketplace.gamebanana(false, page)
            Marketplace.Source.GAMEBANANA_MCC -> Marketplace.gamebanana(true, page)
            Marketplace.Source.ARCHIVE -> Marketplace.archive(query, page)
            Marketplace.Source.NEXUS_MCC -> Marketplace.nexus("halothemasterchiefcollection", nexusKey, adult, page)
            Marketplace.Source.NEXUS_H2 -> Marketplace.nexus("halo2", nexusKey, adult, page)
          }
        }
        if (reset) mods.clear()
        mods += list.filter { adult || !it.adult }
          .filter { query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true) }
        if (mods.isEmpty()) error = context.getString(R.string.market_empty)
      } catch (e: Exception) {
        error = e.message ?: context.getString(R.string.market_error)
      } finally {
        loading = false
      }
    }
  }

  /* the first page as the screen opens, and whenever the source changes */
  LaunchedEffect(source, nexusKey) { load(true) }

  fun take(mod: Marketplace.MarketMod, file: Marketplace.MarketFile) {
    scope.launch {
      busyFile = file.name
      dlProgress = null
      try {
        val tmp = File(context.cacheDir, "market_${file.name.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
        var url = file.url
        /* a Nexus file's link is minted for the key's account, when asked */
        if (url.startsWith("nexus:")) {
          val parts = url.split(":")
          url = withContext(Dispatchers.IO) {
            Marketplace.nexusLink(parts[1], nexusKey, parts[2].toLong(), parts[3].toLong())
          }
        }
        withContext(Dispatchers.IO) {
          Marketplace.download(url, tmp) { got, total -> dlProgress = got to total }
        }
        /* the check and the install are the importer's own: it reads what
         * the file really is (the disc's build, the collection's — converted
         * here — an old tool's patch, a package of those) and installs it
         * over the disc, or refuses with its reason */
        val r = withContext(Dispatchers.IO) { UserMods.importFile(context, gameKey, file.name, tmp) }
        tmp.delete()
        result = r
        opened = null
      } catch (e: Exception) {
        snackbar.showSnackbar(e.message ?: context.getString(R.string.market_error))
      } finally {
        busyFile = null
        dlProgress = null
      }
    }
  }

  /* a source entry's files (and previews) come with its profile, when opened */
  fun open(mod: Marketplace.MarketMod) {
    if (mod.files.isNotEmpty()) {
      opened = mod
      return
    }
    scope.launch {
      busyFile = mod.id
      try {
        val full = withContext(Dispatchers.IO) {
          when {
            mod.id.startsWith("gamebanana-") -> Marketplace.gamebananaMod(mod.id.removePrefix("gamebanana-").toLong())
            mod.id.startsWith("archive-") -> Marketplace.archiveItem(mod.id.removePrefix("archive-"))
            mod.id.startsWith("nexus-") -> {
              val domain = if (source == Marketplace.Source.NEXUS_H2) "halo2" else "halothemasterchiefcollection"
              Marketplace.nexusMod(domain, nexusKey, mod.id.substringAfterLast('-').toLong())
            }
            else -> mod
          }
        }
        opened = full
      } catch (e: Exception) {
        snackbar.showSnackbar(e.message ?: "?")
      } finally {
        busyFile = null
      }
    }
  }

  SubScreen(
    title = stringResource(R.string.market_title), subtitle = title,
    onBack = { if (opened != null) opened = null else onBack() },
    snackbar = snackbar,
  ) { padding ->
    val detail = opened
    if (detail != null) {
      ModDetail(
        mod = detail, busyFile = busyFile,
        onTake = { file -> take(detail, file) },
        modifier = Modifier.padding(padding),
      )
      return@SubScreen
    }
    Column(Modifier.padding(padding).fillMaxSize()) {
      OutlinedTextField(
        value = query, onValueChange = { query = it },
        label = { Text(stringResource(R.string.market_search)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        singleLine = true,
      )
      Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        for (s in listOf(
          Marketplace.Source.CATALOG to stringResource(R.string.market_source_catalog),
          Marketplace.Source.GAMEBANANA_H2 to "GameBanana",
          Marketplace.Source.GAMEBANANA_MCC to stringResource(R.string.market_source_gb_mcc),
          Marketplace.Source.ARCHIVE to stringResource(R.string.market_source_archive),
          Marketplace.Source.NEXUS_MCC to "Nexus",
          Marketplace.Source.NEXUS_H2 to stringResource(R.string.market_source_nexus_h2),
        )) {
          FilterChip(
            selected = source == s.first,
            onClick = { source = s.first },
            label = { Text(s.second, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.padding(end = 6.dp),
          )
        }
      }
      Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
          .toggleable(value = adult, role = Role.Switch, onValueChange = {
            adult = it
            prefs.edit().putBoolean("adult", it).apply()
            load(true)
          }),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(stringResource(R.string.market_adult), Modifier.weight(1f))
        Switch(checked = adult, onCheckedChange = null)
      }
      if (source == Marketplace.Source.NEXUS_MCC || source == Marketplace.Source.NEXUS_H2) {
        Hint(stringResource(R.string.market_nexus_key_desc))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          OutlinedTextField(
            value = keyField, onValueChange = { keyField = it },
            label = { Text(stringResource(R.string.market_nexus_key)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
          )
          TextButton(onClick = {
            nexusKey = keyField.trim()
            prefs.edit().putString("nexus_key", nexusKey).apply()
          }) { Text(stringResource(R.string.market_nexus_save)) }
        }
        if (nexusKey.isBlank()) Hint(stringResource(R.string.market_nexus_none))
      }
      if (loading) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(Modifier.padding(end = 12.dp))
          Text(stringResource(R.string.market_loading))
        }
      }
      error?.let { Hint(it) }
      LazyColumn(Modifier.weight(1f)) {
        items(mods, key = { it.id }) { mod ->
          ListItem(
            headlineContent = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mod.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (mod.adult) {
                  Text(
                    stringResource(R.string.market_adult_tag),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 8.dp),
                  )
                }
              }
            },
            supportingContent = {
              Column {
                Text(
                  listOf(mod.source, mod.author.ifEmpty { null }).filterNotNull().joinToString(" · "),
                  style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val (compat, note) = modCompat(mod)
                Text(
                  "${compat}${if (note != null) " — $note" else ""}",
                  style = MaterialTheme.typography.bodySmall,
                  color = when {
                    compat == stringResource(R.string.market_compat_not) -> MaterialTheme.colorScheme.error
                    compat == stringResource(R.string.market_compat_partial) -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
              }
            },
            modifier = Modifier.clickable { open(mod) },
          )
        }
      }
    }
  }

  if (busyFile != null) {
    AlertDialog(
      onDismissRequest = {},
      title = { Text(stringResource(R.string.market_working)) },
      text = {
        Column {
          Text(stringResource(R.string.market_fetching, busyFile ?: ""))
          val p = dlProgress
          if (p != null && p.second > 0) {
            LinearProgressIndicator(
              progress = { (p.first.toFloat() / p.second).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
              "%.1f / %.1f MB".format(p.first / 1048576f, p.second / 1048576f),
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(top = 4.dp),
            )
          } else if (p != null) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
          }
        }
      },
      confirmButton = {},
    )
  }

  result?.let { r ->
    AlertDialog(
      onDismissRequest = { result = null },
      title = { Text(stringResource(if (r.mod != null) R.string.mods_import_ok else R.string.mods_import_failed)) },
      text = {
        Text(
          listOfNotNull(
            if (r.mod != null) context.getString(R.string.market_taken_line) else null,
            if (r.mod != null) r.mod.description.lineSequence().firstOrNull() else null,
            if (r.mod != null) context.getString(R.string.mods_imported, r.mod.name) else null,
            importErrorText(context, r),
            r.details,
          ).filter { it.isNotBlank() }.joinToString("\n\n")
        )
      },
      confirmButton = { TextButton(onClick = { result = null }) { Text(stringResource(R.string.common_ok)) } },
    )
  }
}

/** One mod, opened: its previews, its word, its files (each with its take). */
@Composable
private fun ModDetail(mod: Marketplace.MarketMod, busyFile: String?,
                      onTake: (Marketplace.MarketFile) -> Unit,
                      modifier: Modifier = Modifier) {
  LazyColumn(modifier.fillMaxSize()) {
    item {
      Text(
        mod.name, style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )
      Text(
        listOf(mod.source, mod.author.ifEmpty { null }).filterNotNull().joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
    }
    if (mod.images.isNotEmpty()) {
      item {
        Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState())) {
          Spacer(Modifier.width(12.dp))
          for (url in mod.images) {
            MarketImage(
              url,
              modifier = Modifier
                .padding(end = 8.dp)
                .size(width = 280.dp, height = 158.dp)
                .clip(RoundedCornerShape(8.dp)),
            )
          }
        }
      }
    }
    if (mod.description.isNotBlank()) {
      item {
        Text(
          mod.description.take(2000),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
    item {
      mod.pageUrl?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
      Hint(stringResource(R.string.market_take_hint))
    }
    if (mod.files.isEmpty()) {
      item { Hint(stringResource(R.string.market_files_on_open)) }
    }
    items(mod.files, key = { it.url }) { file ->
      val (compat, note) = Marketplace.compatFromFile(file.name)
      ListItem(
        headlineContent = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
          Text(sizeText(file.size) + " · " + compatLabel(compat) +
            (if (note != null) " — $note" else ""))
        },
        trailingContent = {
          TextButton(onClick = { onTake(file) }, enabled = busyFile == null) {
            Text(stringResource(R.string.market_download))
          }
        },
      )
    }
  }
}

/** An image off the network, decoded and kept in this screen's own cache. */
@Composable
private fun MarketImage(url: String, modifier: Modifier = Modifier) {
  var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
  LaunchedEffect(url) {
    if (ImageCache.get(url) == null) {
      withContext(Dispatchers.IO) {
        Marketplace.fetchBytes(url)?.let { bytes ->
          runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        }?.let { ImageCache.put(url, it) }
      }
    }
    bitmap = ImageCache.get(url)
  }
  Box(modifier, contentAlignment = Alignment.Center) {
    val b = bitmap
    if (b != null) {
      Image(
        b.asImageBitmap(), contentDescription = null,
        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
      )
    } else {
      CircularProgressIndicator(Modifier.size(28.dp))
    }
  }
}

private object ImageCache {
  private val map = ConcurrentHashMap<String, android.graphics.Bitmap>()
  fun get(url: String) = map[url]
  fun put(url: String, b: android.graphics.Bitmap) { if (map.size < 60) map[url] = b }
}

/** A mod's compatibility, in words, as well as it can be said from here. */
@Composable
private fun modCompat(mod: Marketplace.MarketMod): Pair<String, String?> = when {
  mod.compat != Marketplace.Compat.UNKNOWN -> compatLabel(mod.compat) to mod.compatNote
  mod.files.isNotEmpty() -> compatFromFileWords(mod.files.first().name)
  else -> stringResource(R.string.market_compat_unknown) to null
}

@Composable
private fun compatFromFileWords(name: String): Pair<String, String?> {
  val (c, note) = Marketplace.compatFromFile(name)
  return compatLabel(c) to note
}

@Composable
private fun compatLabel(c: Marketplace.Compat): String = stringResource(
  when (c) {
    Marketplace.Compat.COMPATIBLE -> R.string.market_compat_compatible
    Marketplace.Compat.PARTIAL -> R.string.market_compat_partial
    Marketplace.Compat.NOT -> R.string.market_compat_not
    else -> R.string.market_compat_unknown
  }
)

private fun importErrorText(context: android.content.Context, r: UserMods.ImportResult): String? =
  r.error?.let { err ->
    val id = when (err) {
      UserMods.ImportError.UNREADABLE -> R.string.mods_import_err_unreadable
      UserMods.ImportError.NOT_A_MOD -> R.string.mods_import_err_not_mod
      UserMods.ImportError.NOT_A_PATCH -> R.string.mods_import_err_not_patch
      UserMods.ImportError.EMPTY -> R.string.mods_import_err_empty
      UserMods.ImportError.NOT_HALO2_MAP -> R.string.mods_import_err_not_halo2
      UserMods.ImportError.MCC_MAP -> R.string.mods_import_err_mcc
      UserMods.ImportError.VISTA_MAP -> R.string.mods_import_err_vista
      UserMods.ImportError.BASE_MAP_MISSING -> R.string.mods_import_err_base_map
      UserMods.ImportError.DISC_UNREADABLE -> R.string.mods_import_err_disc
      UserMods.ImportError.PATCH_FAILED -> R.string.mods_import_err_patch
      UserMods.ImportError.NOTHING_USABLE -> R.string.mods_import_err_nothing
    }
    context.getString(id)
  }

private fun sizeText(n: Long): String =
  if (n >= 1024 * 1024) "%.1f MB".format(n / 1024f / 1024f) else "%d KB".format(n / 1024)
