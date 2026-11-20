package com.rfandango.haku_x.ui.mods

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.ModCatalog
import com.rfandango.haku_x.data.ModChoices
import com.rfandango.haku_x.data.ModInfo
import com.rfandango.haku_x.data.ModRun
import com.rfandango.haku_x.data.UserMods
import com.rfandango.haku_x.ui.common.ActionItem
import com.rfandango.haku_x.ui.common.ConfirmDialog
import com.rfandango.haku_x.ui.common.DeleteButton
import com.rfandango.haku_x.ui.common.EmptyText
import com.rfandango.haku_x.ui.common.Hint
import com.rfandango.haku_x.ui.common.Section
import com.rfandango.haku_x.ui.common.SubScreen
import com.rfandango.haku_x.ui.common.SwitchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One disc's mods and cheats: the app's own, the phone's (Halo 2's native
 * camera) and the imported ones — each on or off, the imported ones also
 * removable. Mods take the next start; cheats also turn on and off in the
 * game's menu while it runs.
 */
@Composable
fun ModsScreen(gameKey: String, title: String, onBack: () -> Unit, onBrowse: (String, String) -> Unit) {
  val context = LocalContext.current
  var game by remember(gameKey) { mutableStateOf(ModCatalog.load(context, gameKey)) }
  val choices = remember(gameKey) { ModChoices(context, gameKey) }
  val mods = remember(gameKey) { mutableStateMapOf<String, Boolean>().apply { game.mods.forEach { put(it.id, choices.mod(it.id)) } } }
  val cheats = remember(gameKey) {
    mutableStateMapOf<String, Boolean>().apply { game.cheats.forEach { put(it.name, choices.cheat(it.name)) } }
  }
  val isHalo2 = gameKey.substringBefore('.') in ModChoices.HALO2_TITLE_IDS
  var nativeCamera by remember(gameKey) { mutableStateOf(choices.mod(ModChoices.NATIVE_CAMERA)) }
  /** What the last run did with each mod (ModRun.lastStatus, from its log):
   * applied, or left off entirely — this disc is another release of the
   * game, the reason "mods don't work" was invisible until now. */
  val lastRun = remember(gameKey) { ModRun.lastStatus(context, gameKey) }
  val statusApplied = stringResource(R.string.mods_status_applied)
  val statusLeftOff = stringResource(R.string.mods_status_left_off)
  fun statusLine(id: String): String? = when (lastRun?.get(id)) {
    true -> statusApplied
    false -> statusLeftOff
    null -> null
  }
  var deleting by remember { mutableStateOf<ModInfo?>(null) }
  val snackbar = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  fun reload() {
    game = ModCatalog.load(context, gameKey)
    game.mods.forEach { if (!mods.containsKey(it.id)) mods[it.id] = choices.mod(it.id) }
  }

  var importing by remember { mutableStateOf(false) }
  var result by remember { mutableStateOf<UserMods.ImportResult?>(null) }
  /* the files the mods on carry, and where two of them replace the same disc
   * file (the later one in the list wins, as the core reads them) */
  var filesOf by remember(gameKey) { mutableStateOf<Map<String, String>>(emptyMap()) }
  var conflicts by remember(gameKey) { mutableStateOf<List<String>>(emptyList()) }
  val enabledIds = game.mods.filter { mods[it.id] == true }.map { it.id }.toSet() +
    (if (isHalo2 && nativeCamera) setOf(ModChoices.NATIVE_CAMERA) else emptySet())
  LaunchedEffect(gameKey, enabledIds) {
    withContext(Dispatchers.IO) {
      val byMod = game.mods.filter { it.id in enabledIds }.associate { mod ->
        val id = mod.id
        (id to (if (id.startsWith(UserMods.PREFIX)) UserMods.files(context, gameKey, id)
        else ModCatalog.extract(context, gameKey, id)))
      }
      conflicts = byMod.flatMap { (id, fs) -> fs.map { it.first.lowercase() to id } }
        .groupBy({ it.first }, { it.second })
        .filter { it.value.size > 1 }
        .map { (path, ids) ->
          "$path (${ids.joinToString { id -> game.mods.first { m -> m.id == id }.name }})"
        }
      filesOf = game.mods.filter { it.id.startsWith(UserMods.PREFIX) }.associate { mod ->
        mod.id to UserMods.files(context, gameKey, mod.id)
          .joinToString("\n") { (disc, f) -> "${disc.replace('/', '\\')} — ${sizeText(f.length())}" }
      }
    }
  }

  val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch {
      importing = true
      val r = withContext(Dispatchers.IO) { UserMods.import(context, gameKey, uri) }
      importing = false
      if (r.mod != null) reload()
      result = r
    }
  }

  /** What the import came to, in words: the outcome and its specifics. */
  fun importText(r: UserMods.ImportResult): String {
    val head = when (r.error) {
      null -> context.getString(R.string.mods_imported, r.mod?.name ?: "")
      UserMods.ImportError.UNREADABLE -> context.getString(R.string.mods_import_err_unreadable)
      UserMods.ImportError.NOT_A_MOD -> context.getString(R.string.mods_import_err_not_mod)
      UserMods.ImportError.NOT_A_PATCH -> context.getString(R.string.mods_import_err_not_patch)
      UserMods.ImportError.EMPTY -> context.getString(R.string.mods_import_err_empty)
      UserMods.ImportError.NOT_HALO2_MAP -> context.getString(R.string.mods_import_err_not_halo2)
      UserMods.ImportError.MCC_MAP -> context.getString(R.string.mods_import_err_mcc)
      UserMods.ImportError.VISTA_MAP -> context.getString(R.string.mods_import_err_vista)
      UserMods.ImportError.BASE_MAP_MISSING -> context.getString(R.string.mods_import_err_base_map)
      UserMods.ImportError.DISC_UNREADABLE -> context.getString(R.string.mods_import_err_disc)
      UserMods.ImportError.PATCH_FAILED -> context.getString(R.string.mods_import_err_patch)
      UserMods.ImportError.NOTHING_USABLE -> context.getString(R.string.mods_import_err_nothing)
    }
    return listOf(head, r.details.orEmpty()).filter { it.isNotEmpty() }.joinToString("\n\n")
  }

  SubScreen(title = stringResource(R.string.mods_title), subtitle = title, onBack = onBack, snackbar = snackbar) { padding ->
    Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
      if (game.isEmpty && !isHalo2) {
        EmptyText(stringResource(R.string.mods_none))
      }
      if (isHalo2) {
        Section(stringResource(R.string.mods_section_this_phone))
        Hint(stringResource(R.string.mods_native_camera_desc))
        SwitchItem(
          title = stringResource(R.string.mods_native_camera),
          summary = statusLine(ModChoices.NATIVE_CAMERA),
          checked = nativeCamera,
          onChange = { v ->
            choices.setMod(ModChoices.NATIVE_CAMERA, v)
            nativeCamera = v
          },
        )
      }
      val own = game.mods.filter { !it.id.startsWith(UserMods.PREFIX) }
      val imported = game.mods.filter { it.id.startsWith(UserMods.PREFIX) }
      if (own.isNotEmpty()) {
        Section(stringResource(R.string.mods_section_mods))
        Hint(stringResource(R.string.mods_mods_desc))
        for (mod in own) {
          val by = if (mod.author.isNotEmpty()) stringResource(R.string.mods_by, mod.author) else ""
          SwitchItem(
            title = mod.name,
            summary = listOf(mod.description, by, statusLine(mod.id) ?: "")
              .filter { it.isNotEmpty() }.joinToString("\n").ifEmpty { null },
            checked = mods[mod.id] == true,
            onChange = { v ->
              choices.setMod(mod.id, v)
              mods[mod.id] = v
            },
          )
        }
      }
      Section(stringResource(R.string.mods_section_imported))
      Hint(stringResource(R.string.mods_howto_title))
      Hint(stringResource(R.string.mods_howto_maps))
      Hint(stringResource(R.string.mods_howto_patches))
      Hint(stringResource(R.string.mods_import_desc))
      if (conflicts.isNotEmpty()) {
        Hint(stringResource(R.string.mods_conflict, conflicts.joinToString("\n")))
      }
      for (mod in imported) {
        UserModRow(
          mod = mod,
          checked = mods[mod.id] == true,
          summary = listOfNotNull(
            filesOf[mod.id]?.ifEmpty { null },
            statusLine(mod.id),
          ).joinToString("\n").ifEmpty { null },
          onChange = { v ->
            choices.setMod(mod.id, v)
            mods[mod.id] = v
          },
          onDelete = { deleting = mod },
        )
      }
      ActionItem(
        title = stringResource(if (importing) R.string.mods_importing else R.string.mods_import),
        summary = null,
        icon = null,
      ) {
        if (!importing) import.launch(arrayOf("*/*"))
      }
      ActionItem(
        title = stringResource(R.string.market_browse),
        summary = stringResource(R.string.market_browse_desc),
        icon = null,
      ) {
        onBrowse(gameKey, title)
      }
      if (game.cheats.isNotEmpty()) {
        Section(stringResource(R.string.mods_section_cheats))
        Hint(stringResource(R.string.mods_cheats_desc))
        for (cheat in game.cheats) {
          SwitchItem(
            title = cheat.name,
            summary = cheat.note.ifEmpty { null },
            checked = cheats[cheat.name] == true,
            onChange = { v ->
              choices.setCheat(cheat.name, v)
              cheats[cheat.name] = v
            },
          )
        }
      }
    }
  }

  deleting?.let { mod ->
    ConfirmDialog(
      title = stringResource(R.string.mods_delete),
      text = stringResource(R.string.mods_delete_text, mod.name),
      confirm = stringResource(R.string.common_delete),
      onConfirm = {
        UserMods.delete(context, gameKey, mod.id)
        mods.remove(mod.id)
        deleting = null
        reload()
      },
      onDismiss = { deleting = null },
    )
  }

  result?.let { r ->
    AlertDialog(
      onDismissRequest = { result = null },
      title = { Text(stringResource(if (r.mod != null) R.string.mods_import_ok else R.string.mods_import_failed)) },
      text = { Text(importText(r)) },
      confirmButton = {
        TextButton(onClick = { result = null }) { Text(stringResource(R.string.common_ok)) }
      },
    )
  }
}

/** A size in people's words (28.4 MB, 412 KB). */
private fun sizeText(n: Long): String =
  if (n >= 1024 * 1024) "%.1f MB".format(n / 1024f / 1024f) else "%d KB".format(n / 1024)

/** An imported mod: toggled with its switch, removed with its button. */
@Composable
private fun UserModRow(
  mod: ModInfo,
  checked: Boolean,
  summary: String? = null,
  onChange: (Boolean) -> Unit,
  onDelete: () -> Unit,
) {
  ListItem(
    headlineContent = { Text(mod.name) },
    supportingContent = listOf(mod.description, summary.orEmpty()).filter { it.isNotEmpty() }
      .joinToString("\n").ifEmpty { null }?.let { { Text(it) } },
    trailingContent = {
      Row {
        Switch(checked = checked, onCheckedChange = null)
        DeleteButton(onClick = onDelete)
      }
    },
    modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
  )
}
