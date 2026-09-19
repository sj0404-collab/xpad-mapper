package com.xpad.mapper

import android.content.Context
import android.util.AttributeSet
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

/**
 * View, который удерживает фокус и ловит все события геймпада.
 * Стандартный приём для чтения кнопок/осей с контроллера.
 */
class GamepadTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onButtonPress(keyCode: Int, action: Int)
        fun onAxisMove(event: MotionEvent, device: InputDevice)
    }

    var listener: Listener? = null

    /** Если >= 0 — слушаем только события этого устройства. */
    var deviceFilter: Int = -1

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setOnKeyListener { _, keyCode, event ->
            val d = event.device
            if (deviceFilter < 0 || d == null || d.id == deviceFilter) {
                listener?.onButtonPress(keyCode, event.action)
                return@setOnKeyListener true
            }
            true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val d = event?.device
        if (deviceFilter < 0 || d == null || d.id == deviceFilter) {
            listener?.onButtonPress(keyCode, KeyEvent.ACTION_DOWN)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val d = event?.device
        if (deviceFilter < 0 || d == null || d.id == deviceFilter) {
            listener?.onButtonPress(keyCode, KeyEvent.ACTION_UP)
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val d = event.device
        if (deviceFilter >= 0 && (d == null || d.id != deviceFilter)) return true

        if (event.actionMasked == MotionEvent.ACTION_MOVE && d != null) {
            listener?.onAxisMove(event, d)
        }
        return true
    }

    fun requestGamepadFocus() {
        post { requestFocus() }
    }
}