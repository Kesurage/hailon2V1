package com.rfandango.haku_x

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.max

class SwipeUpGestureRecognizer(
  /** false: a swipe up (from the bottom); true: a swipe down (from the top). */
  private val down: Boolean = false,
  private val minDistancePx: () -> Float,
  private val touchSlopPx: () -> Float,
  private val canStartAt: (x: Float, y: Float) -> Boolean,
  private val onTriggered: () -> Unit,
) {
  private var activePointerId = INVALID_POINTER_ID
  private var startX = 0f
  private var startY = 0f
  private var tracking = false
  private var triggered = false

  fun onTouchEvent(event: MotionEvent): Boolean {
    if (triggered) {
      when (event.actionMasked) {
        MotionEvent.ACTION_UP,
        MotionEvent.ACTION_CANCEL -> reset()
        MotionEvent.ACTION_POINTER_UP -> {
          if (event.getPointerId(event.actionIndex) == activePointerId) {
            reset()
          }
        }
      }
      return true
    }

    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        reset()
        val x = event.getX(0)
        val y = event.getY(0)
        if (canStartAt(x, y)) {
          activePointerId = event.getPointerId(0)
          startX = x
          startY = y
          tracking = true
        }
      }

      MotionEvent.ACTION_POINTER_DOWN -> {
        reset()
      }

      MotionEvent.ACTION_MOVE -> {
        if (!tracking) {
          return false
        }

        val pointerIndex = event.findPointerIndex(activePointerId)
        if (pointerIndex < 0) {
          reset()
          return false
        }

        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val dx = x - startX
        val dy = y - startY
        // The gesture's direction: up the screen, or down from the top edge.
        val along = if (down) dy else -dy
        // Moved against the gesture (up when swiping down, down when swiping
        // up) by more than the slop: not the gesture.
        val against = if (down) -dy else dy
        val slop = touchSlopPx()

        if (against > slop || abs(dx) > max(slop * 5f, along * 0.9f + slop)) {
          reset()
          return false
        }

        if (along >= minDistancePx() &&
          along >= abs(dx) * 1.35f) {
          triggered = true
          tracking = false
          onTriggered()
          return true
        }
      }

      MotionEvent.ACTION_POINTER_UP -> {
        if (event.getPointerId(event.actionIndex) == activePointerId) {
          reset()
        }
      }

      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> reset()
    }

    return false
  }

  fun reset() {
    activePointerId = INVALID_POINTER_ID
    startX = 0f
    startY = 0f
    tracking = false
    triggered = false
  }

  private companion object {
    const val INVALID_POINTER_ID = -1
  }
}
