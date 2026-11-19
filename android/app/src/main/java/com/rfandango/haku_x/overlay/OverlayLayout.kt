package com.rfandango.haku_x.overlay

import android.content.Context
import org.json.JSONObject
import java.io.File

/** How a control is drawn and how it takes touches. */
enum class ControlKind { FACE, GEM, PILL, STICK, DPAD, TRIGGER, CLICK, MENU }

/**
 * A button-ish control's look (the editor's Icon action cycles these): the
 * glossy jewel as on the controller (JEWEL), a flat dark disc (DISC), a thin
 * outline (RING), or the user's own picture (PHOTO, OverlayStore's icon file
 * for the control). Sticks, the D-pad and the menu keep their own looks.
 */
enum class ControlStyle { JEWEL, DISC, RING, PHOTO }

/** The on-screen controller's controls. */
enum class ControlId(val kind: ControlKind, val label: String, val title: String) {
  A(ControlKind.FACE, "A", "A button"),
  B(ControlKind.FACE, "B", "B button"),
  X(ControlKind.FACE, "X", "X button"),
  Y(ControlKind.FACE, "Y", "Y button"),
  WHITE(ControlKind.GEM, "", "White button"),
  BLACK(ControlKind.GEM, "", "Black button"),
  BACK(ControlKind.PILL, "BACK", "Back"),
  START(ControlKind.PILL, "START", "Start"),
  LEFT_TRIGGER(ControlKind.TRIGGER, "LT", "Left trigger"),
  RIGHT_TRIGGER(ControlKind.TRIGGER, "RT", "Right trigger"),
  LEFT_STICK(ControlKind.STICK, "", "Left stick"),
  RIGHT_STICK(ControlKind.STICK, "", "Right stick"),
  LEFT_STICK_CLICK(ControlKind.CLICK, "LS", "Left stick click"),
  RIGHT_STICK_CLICK(ControlKind.CLICK, "RS", "Right stick click"),
  DPAD(ControlKind.DPAD, "", "D-pad"),
  MENU(ControlKind.MENU, "", "Menu"),
}

/**
 * One control's place on screen, in pixels of the overlay; radius is its
 * size, opacity how much of the game it hides (0.1-1).
 */
class ControlSpec(
  val id: ControlId,
  var cx: Float,
  var cy: Float,
  var radius: Float,
  var visible: Boolean = true,
  var opacity: Float = OverlayStore.DEFAULT_OPACITY,
  var style: ControlStyle = ControlStyle.JEWEL,
) {
  fun copy() = ControlSpec(id, cx, cy, radius, visible, opacity, style)
}

/** All controls. */
class OverlayLayout(val controls: MutableMap<ControlId, ControlSpec>) {
  operator fun get(id: ControlId): ControlSpec = controls.getValue(id)
  fun copy() = OverlayLayout(controls.mapValuesTo(LinkedHashMap()) { it.value.copy() })
}

/**
 * The layout the user made, kept as a file in the app's storage so the
 * settings (main process) and the game (the core's process) read the same
 * one: positions as fractions of the overlay's width and height, sizes as
 * fractions of its height. Missing: the defaults for the screen at hand.
 */
object OverlayStore {
  private const val FILE = "overlay_layout.json"
  /** The layout file's version: one below drops back to the defaults. */
  private const val VERSION = 3
  /** Half see-through: the game stays visible under the controls. */
  const val DEFAULT_OPACITY = 0.5f

  /** A control's chosen picture (ControlStyle.PHOTO), kept small; null when none. */
  fun iconFile(context: Context, id: ControlId): File? =
    File(File(context.filesDir, "overlay_icons"), id.name + ".png").takeIf { it.isFile }

  /** Drops a control's picture (the editor's clear, or the style left PHOTO). */
  fun clearIcon(context: Context, id: ControlId) {
    iconFile(context, id)?.delete()
  }

  fun load(context: Context, w: Float, h: Float): OverlayLayout {
    val layout = defaults(w, h)
    val file = File(context.filesDir, FILE)
    if (!file.isFile) {
      return layout
    }
    try {
      val root = JSONObject(file.readText())
      /* An arrangement from before this version (the Duke's, with its offset
       * button columns) reads as the new one's places instead: drop it, the
       * mobile arrangement is the default the editor starts from again. */
      if (root.optInt("version", 0) < VERSION) {
        return layout
      }
      val controls = root.optJSONObject("controls") ?: return layout
      for (id in ControlId.entries) {
        val c = controls.optJSONObject(id.name) ?: continue
        val spec = layout[id]
        spec.cx = (c.optDouble("x", (spec.cx / w).toDouble()).toFloat() * w).coerceIn(0f, w)
        spec.cy = (c.optDouble("y", (spec.cy / h).toDouble()).toFloat() * h).coerceIn(0f, h)
        spec.radius = (c.optDouble("r", (spec.radius / h).toDouble()).toFloat() * h).coerceIn(minRadius(h), maxRadius(h))
        spec.visible = id == ControlId.MENU || c.optBoolean("v", true)
        spec.opacity = c.optDouble("o", DEFAULT_OPACITY.toDouble()).toFloat().coerceIn(MIN_OPACITY, 1f)
        spec.style = c.optString("s", ControlStyle.JEWEL.name).let {
          try { ControlStyle.valueOf(it) } catch (_: Exception) { ControlStyle.JEWEL }
        }
      }
    } catch (_: Exception) {
      return defaults(w, h)
    }
    return layout
  }

  fun save(context: Context, layout: OverlayLayout, w: Float, h: Float) {
    val controls = JSONObject()
    for ((id, spec) in layout.controls) {
      controls.put(id.name, JSONObject().apply {
        put("x", (spec.cx / w).toDouble())
        put("y", (spec.cy / h).toDouble())
        put("r", (spec.radius / h).toDouble())
        put("v", spec.visible)
        put("o", spec.opacity.toDouble())
        put("s", spec.style.name)
      })
    }
    val root = JSONObject().apply {
      put("version", VERSION)
      put("controls", controls)
    }
    val file = File(context.filesDir, FILE)
    val tmp = File(context.filesDir, "$FILE.tmp")
    tmp.writeText(root.toString())
    tmp.renameTo(file)
  }

  /** Back to the defaults (the file goes). */
  fun reset(context: Context) {
    File(context.filesDir, FILE).delete()
  }

  const val MIN_OPACITY = 0.1f
  fun minRadius(h: Float) = h * 0.025f
  fun maxRadius(h: Float) = h * 0.35f

  /**
   * A mobile game's arrangement on a landscape screen: the move stick low
   * left (it floats to wherever the left thumb lands), the face buttons a
   * clean diamond right of centre, the D-pad small beside them, the right
   * stick hidden — the right of the screen is the camera (a drag anywhere
   * over no control aims), its click (Halo 2's scope) a button of its own
   * under the diamond. The triggers sit in the top corners, white and black
   * small at the top middle, and the menu jewel with back and start at the
   * bottom centre.
   */
  fun defaults(w: Float, h: Float): OverlayLayout {
    val c = LinkedHashMap<ControlId, ControlSpec>()
    fun put(id: ControlId, x: Float, y: Float, r: Float, visible: Boolean = true) {
      c[id] = ControlSpec(id, x, y, r, visible)
    }
    put(ControlId.LEFT_STICK, h * 0.28f, h * 0.62f, h * 0.15f)
    put(ControlId.LEFT_STICK_CLICK, h * 0.08f, h * 0.88f, h * 0.042f)
    put(ControlId.DPAD, w * 0.46f, h * 0.80f, h * 0.082f)

    val face = h * 0.064f
    val d = h * 0.134f
    val fx = w - h * 0.27f
    val fy = h * 0.50f
    put(ControlId.Y, fx, fy - d, face)
    put(ControlId.X, fx - d, fy, face)
    put(ControlId.B, fx + d, fy, face)
    put(ControlId.A, fx, fy + d, face)
    put(ControlId.RIGHT_STICK_CLICK, fx - h * 0.33f, fy + h * 0.30f, h * 0.048f)
    put(ControlId.RIGHT_STICK, w - h * 0.30f, h * 0.68f, h * 0.13f, visible = false)

    val gem = h * 0.040f
    put(ControlId.WHITE, w / 2 - h * 0.13f, h * 0.10f, gem)
    put(ControlId.BLACK, w / 2 + h * 0.13f, h * 0.10f, gem)

    val pill = h * 0.042f
    put(ControlId.MENU, w / 2, h * 0.92f, h * 0.05f)
    put(ControlId.BACK, w / 2 - h * 0.17f, h * 0.92f, pill)
    put(ControlId.START, w / 2 + h * 0.17f, h * 0.92f, pill)

    put(ControlId.LEFT_TRIGGER, h * 0.14f, h * 0.13f, h * 0.064f)
    put(ControlId.RIGHT_TRIGGER, w - h * 0.14f, h * 0.13f, h * 0.064f)
    return OverlayLayout(c)
  }
}
