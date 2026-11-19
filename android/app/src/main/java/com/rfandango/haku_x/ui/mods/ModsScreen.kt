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
import androidx.compose.material3.ListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch

/**
 * One disc's mods and cheats: the app's own, the phone's (Halo 2's native
 * camera) and the imported ones — each on or off, the imported ones also
 * removable. Mods take the next start; cheats also turn on and off in the
 * game's menu while it runs.
 */
@Composable
fun ModsScreen(gameKey: String, title: String, onBack: () -> Unit) {
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

  val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
    if (uri == null) return@rememberLauncherForActivityResult
    val r = UserMods.import(context, gameKey, uri)
    reload()
    val msg = when (r.error) {
      null -> context.getString(R.string.mods_imported, r.mod?.name ?: "")
      UserMods.ImportError.UNREADABLE -> context.getString(R.string.mods_import_err_unreadable)
      UserMods.ImportError.NOT_A_MOD -> context.getString(R.string.mods_import_err_not_mod)
      UserMods.ImportError.NOT_A_PATCH -> context.getString(R.string.mods_import_err_not_patch)
      UserMods.ImportError.EMPTY -> context.getString(R.string.mods_import_err_empty)
    }
    scope.launch { snackbar.showSnackbar(msg) }
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
      for (mod in imported) {
        UserModRow(
          mod = mod,
          checked = mods[mod.id] == true,
          onChange = { v ->
            choices.setMod(mod.id, v)
            mods[mod.id] = v
          },
          onDelete = { deleting = mod },
        )
      }
      ActionItem(
        title = stringResource(R.string.mods_import),
        summary = null,
        icon = null,
      ) {
        import.launch(arrayOf("*/*"))
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
}

/** An imported mod: toggled with its switch, removed with its button. */
@Composable
private fun UserModRow(mod: ModInfo, checked: Boolean, onChange: (Boolean) -> Unit, onDelete: () -> Unit) {
  ListItem(
    headlineContent = { Text(mod.name) },
    supportingContent = mod.description.ifEmpty { null }?.let { { Text(it) } },
    trailingContent = {
      Row {
        Switch(checked = checked, onCheckedChange = null)
        DeleteButton(onClick = onDelete)
      }
    },
    modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
  )
}
