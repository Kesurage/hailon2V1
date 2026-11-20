package com.rfandango.haku_x

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import com.rfandango.haku_x.data.AppPrefs
import com.rfandango.haku_x.data.Game
import com.rfandango.haku_x.data.PreviousVersion
import com.rfandango.haku_x.data.SaveStateStore
import com.rfandango.haku_x.data.Storage
import com.rfandango.haku_x.data.hdd.Dashboard
import com.rfandango.haku_x.data.hdd.HddStore
import com.rfandango.haku_x.input.InputCapture
import com.rfandango.haku_x.ui.HakuXApp
import com.rfandango.haku_x.ui.theme.AppTheme
import com.rfandango.haku_x.ui.theme.HakuXTheme
import com.rfandango.haku_x.ui.theme.appInDarkTheme
import java.io.File

/**
 * The app's one screen host (Compose: setup, library, settings), and the
 * entry for frontends that launch a game directly (a VIEW intent with the
 * image, or a rom_path extra): those go straight to the core.
 */
class LauncherActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val prefs = AppPrefs(this)

    val usableFolders = prefs.gamesFolders.filter { Storage.hasReadPermission(this, it) }
    val folderReady = usableFolders.isNotEmpty()
    val lastGame = prefs.lastGameUri
    val lastGameUsable = prefs.lastGamePath?.let { File(it).isFile } == true ||
      (lastGame != null && (Storage.hasReadPermission(this, lastGame) || folderReady))
    prefs.dropStale(lastGameUsable, usableFolders)
    Thread { HddStore(this).deleteLeftovers() }.start()
    // the first start after the update from the xemu-based release: its data comes over first
    val needsMigration = !prefs.previousMigrated && PreviousVersion(this).find() != null

    if (folderReady && launchExternal()) {
      return
    }

    // Insignia's setup started the Setup Assistant or the dashboard, now closed: it opens again
    val openInsignia = prefs.insigniaWizardResume.also { if (it) prefs.insigniaWizardResume = false }

    AppTheme.load(this)
    var windowDark = AppTheme.isDark(this)
    windowTheme(windowDark)
    setContent {
      val dark = appInDarkTheme()
      if (dark != windowDark) {
        SideEffect {
          windowDark = dark
          windowTheme(dark)
        }
      }
      HakuXTheme(dark) {
        HakuXApp(
          needsMigration = needsMigration,
          needsSetup = !folderReady,
          onLaunchGame = { game: Game ->
            Log.i(TAG, "launch: path=${game.relativePath} uri=${game.uri} size=${game.sizeBytes}")
            Storage.persistPermission(this, game.uri)
            startCore(game.uri, game.relativePath, game.title, launchedExternally = false)
          },
          onLaunchState = { game: Game, store: SaveStateStore, slot: Int ->
            Log.i(TAG, "launch: path=${game.relativePath} into savestate slot $slot")
            Storage.persistPermission(this, game.uri)
            /* the state's frame on screen while the core starts (NativeCoreActivity holds it) */
            val held = RelaunchActivity.heldFrame(this)
            runCatching { store.thumb(slot).copyTo(held, overwrite = true) }.onFailure { held.delete() }
            startCore(game.uri, game.relativePath, game.title, launchedExternally = false,
                      state = store.state(slot).absolutePath)
          },
          onLaunchDashboard = {
            Log.i(TAG, "launch: the dashboard")
            /* not remembered as the last game: no disc */
            startActivity(Intent(this, NativeCoreActivity::class.java)
              .putExtra(NativeCoreActivity.EXTRA_DVD_URI, Dashboard.URI))
            finish()
          },
          onLaunchSetup = { uri: Uri ->
            Log.i(TAG, "launch: $uri (Insignia's setup)")
            /* not a library game: not remembered as the last one */
            startActivity(Intent(this, NativeCoreActivity::class.java)
              .putExtra(NativeCoreActivity.EXTRA_DVD_URI, uri.toString()))
            finish()
          },
          openInsignia = openInsignia,
        )
      }
    }
  }

  /** The system bars' icons and the window behind Compose, light or dark as the app's theme is. */
  private fun windowTheme(dark: Boolean) {
    val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
    window.setBackgroundDrawable(ColorDrawable(if (dark) DARK_WINDOW else Color.WHITE))
  }

  /**
   * The core runs in its own process; this one is done until the next
   * launch. When the run before this same disc did not end cleanly — a
   * crash, a kill, a force stop — the offer comes first: mods are built
   * into a game at its start, and one that breaks the game has no way back
   * but a clean boot (0003D's safe mode).
   */
  private fun startCore(
    uri: Uri,
    relativePath: String?,
    title: String?,
    launchedExternally: Boolean,
    state: String? = null,
  ) {
    if (runEndedCleanly(uri)) {
      startCoreCore(uri, relativePath, title, launchedExternally, state, noMods = false)
      return
    }
    Log.i(TAG, "launch: the run before this disc did not end cleanly — offering the no-mods start")
    android.app.AlertDialog.Builder(this)
      .setTitle(R.string.safe_mode_title)
      .setMessage(getString(R.string.safe_mode_body, title ?: getString(R.string.safe_mode_this_game)))
      .setPositiveButton(R.string.safe_mode_no_mods) { _, _ ->
        startCoreCore(uri, relativePath, title, launchedExternally, state, noMods = true)
      }
      .setNegativeButton(R.string.safe_mode_normal) { _, _ ->
        startCoreCore(uri, relativePath, title, launchedExternally, state, noMods = false)
      }
      .setNeutralButton(R.string.safe_mode_cancel, null)
      .show()
  }

  /** The run watchdog's marker, as NativeCoreActivity keeps it (by the disc's URI). */
  private fun runEndedCleanly(uri: Uri): Boolean {
    val h = java.security.MessageDigest.getInstance("MD5")
      .digest(uri.toString().toByteArray())
      .joinToString("") { b -> "%02x".format(b) }
    return !java.io.File(java.io.File(filesDir, "native/runs"), h).exists()
  }

  private fun startCoreCore(
    uri: Uri,
    relativePath: String?,
    title: String?,
    launchedExternally: Boolean,
    state: String?,
    noMods: Boolean,
  ) {
    AppPrefs(this).rememberLaunch(uri, relativePath, title)
    startActivity(Intent(this, NativeCoreActivity::class.java).apply {
      putExtra(NativeCoreActivity.EXTRA_DVD_URI, uri.toString())
      putExtra(EXTRA_LAUNCHED_EXTERNALLY, launchedExternally)
      if (state != null) putExtra(NativeCoreActivity.EXTRA_LOADSTATE, state)
      if (noMods) putExtra(NativeCoreActivity.EXTRA_NO_MODS, true)
      /* a frontend's: the game alone in the app's task (a library left in it
       * goes), so that what's under it when it closes is the frontend */
      if (launchedExternally) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    })
    finish()
  }

  /* A frontend's launch while the library is open (singleTop). */
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (AppPrefs(this).gamesFolders.any { Storage.hasReadPermission(this, it) }) {
      launchExternal()
    }
  }

  /** The game a frontend asked for, straight to the core; false: none asked. */
  private fun launchExternal(): Boolean {
    val external = externalGame() ?: return false
    Storage.persistPermission(this, external, write = false)
    startCore(external, null, null, launchedExternally = true)
    return true
  }

  private fun externalGame(): Uri? {
    intent?.data?.let { return it }
    val path = intent?.getStringExtra("rom_path") ?: return null
    val file = File(path)
    return if (file.isFile) Uri.fromFile(file) else null
  }

  /* Controllers' events to the controller settings first (InputCapture). */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean =
    InputCapture.dispatch(event) || super.dispatchKeyEvent(event)

  override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
    InputCapture.dispatch(event) || super.dispatchGenericMotionEvent(event)

  companion object {
    private const val TAG = "hakuX-library"
    /** The dark theme's background (ui/theme Theme.kt), behind Compose. */
    private const val DARK_WINDOW = 0xFF121212.toInt()
    /** Set when a frontend launched the game (no library to go back to). */
    const val EXTRA_LAUNCHED_EXTERNALLY = "launched_externally"
  }
}
