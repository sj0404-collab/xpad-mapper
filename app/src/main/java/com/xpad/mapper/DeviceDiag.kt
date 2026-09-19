package com.xpad.mapper

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/** Результат диагностики: как система видит подключённый девайс. */
data class DiagResult(
    val badgeText: String,
    val badgeColor: Int,
    val verdict: String,
    val tip: String
)

object DeviceDiag {

    fun sourcesLabel(device: InputDevice): String {
        val s = device.sources
        val parts = mutableListOf<String>()
        if (s and InputDevice.SOURCE_DPAD != 0) parts.add("D-pad")
        if (s and InputDevice.SOURCE_GAMEPAD != 0) parts.add("Gamepad")
        if (s and InputDevice.SOURCE_JOYSTICK != 0) parts.add("Joystick")
        if (s and InputDevice.SOURCE_KEYBOARD != 0) parts.add("Keyboard")
        if (s and InputDevice.SOURCE_TOUCHSCREEN != 0) parts.add("Touch")
        if (s and InputDevice.SOURCE_MOUSE != 0) parts.add("Mouse")
        if (parts.isEmpty()) parts.add("Unknown(0x" + Integer.toHexString(s) + ")")
        return parts.joinToString(" + ")
    }

    fun diagnose(device: InputDevice): DiagResult {
        val s = device.sources
        val hasGamepad = s and InputDevice.SOURCE_GAMEPAD != 0
        val hasJoystick = s and InputDevice.SOURCE_JOYSTICK != 0
        val hasKeyboard = s and InputDevice.SOURCE_KEYBOARD != 0
        val hasDpad = s and InputDevice.SOURCE_DPAD != 0

        // Проверяем, какие стандартные (Xbox-стиль) кнопки рекламирует устройство.
        val gamepadKeys = intArrayOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT
        )
        val supported = device.hasKeys(*gamepadKeys).count { it }

        val badge: String
        val color: Int
        val verdict: String
        val tip: String

        if (hasGamepad && hasJoystick) {
            badge = "Gamepad + Joystick"
            color = 0xFF2E7D32.toInt()
            verdict = "OS видит устройство как геймпад и джойстик — это лучший режим."
            tip = "Большинство игр распознает его. Если какая-то игра его не видит — смотри раздел «Помощь»."
        } else if (hasGamepad) {
            badge = "Gamepad"
            color = 0xFF2E7D32.toInt()
            verdict = "OS видит устройство как геймпад."
            tip = if (supported >= 6) {
                "Рекламирует ${supported}/8 стандартных кнопок Xbox-раскладки — совместимость высокая."
            } else {
                "Рекламирует только ${supported}/8 стандартных кнопок. Часть кнопок может прилетать как цифры (Button 1..16) — ремап поможет."
            }
        } else if (hasJoystick) {
            badge = "Joystick"
            color = 0xFFEF6C00.toInt()
            verdict = "OS видит устройство как джойстик (оси), но НЕ как геймпад."
            tip = "Именно поэтому часть игр его не видит. Переключите геймпад в режим Xbox/Gamepad (обычно удерживайте клавишу MODE/Home или комбинацию для смены HID-профиля), затем нажмите «Обновить»."
        } else if (hasKeyboard && hasDpad) {
            badge = "Клавиатура+D-pad"
            color = 0xFFD32F2F.toInt()
            verdict = "OS принимает устройство как клавиатуру с крестовиной."
            tip = "Игры для геймпадов его не увидят. Переключите режим контроллера на Gamepad/Xbox и попробуйте снова."
        } else {
            badge = "Другое"
            color = 0xFFD32F2F.toInt()
            verdict = "Устройство не выглядит как игровой контроллер для Android."
            tip = "Попробуйте другой режим геймпада (Xbox-HID / Dinput). Некоторые дешёвые BT-геймпады в режиме Dinput не рекламируют SOURCE_GAMEPAD."
        }
        return DiagResult(badge, color, verdict, tip)
    }

    /** Оси, которые рекламирует устройство (для деталей). */
    fun axisSummary(device: InputDevice): String {
        val out = StringBuilder()
        for (range in device.motionRanges) {
            if (out.isNotEmpty()) out.append("\n")
            val name = axisName(range.axis)
            out.append("$name: min=${fmt1(range.min)} max=${fmt1(range.max)} flat=${fmt1(range.flat)}")
        }
        return if (out.isEmpty()) "Осей не рекламируется." else out.toString()
    }

    fun stdAxisCheck(device: InputDevice): String {
        val needed = mapOf(
            MotionEvent.AXIS_X to "X", MotionEvent.AXIS_Y to "Y",
            MotionEvent.AXIS_Z to "Z(RX)", MotionEvent.AXIS_RZ to "RZ(RY)",
            MotionEvent.AXIS_LTRIGGER to "L2", MotionEvent.AXIS_RTRIGGER to "R2",
            MotionEvent.AXIS_HAT_X to "HatX", MotionEvent.AXIS_HAT_Y to "HatY"
        )
        val parts = needed.mapNotNull { (axis, n) ->
            device.getMotionRange(axis)?.let { "✓ $n" }
        }
        return if (parts.isEmpty()) "Нет осей из стандартного набора." else parts.joinToString(" ")
    }

    private fun axisName(axis: Int): String = when (axis) {
        MotionEvent.AXIS_X -> "X (левый стик horiz)"
        MotionEvent.AXIS_Y -> "Y (левый стик vert)"
        MotionEvent.AXIS_Z -> "Z (правый стик horiz)"
        MotionEvent.AXIS_RZ -> "RZ (правый стик vert)"
        MotionEvent.AXIS_LTRIGGER -> "L2"
        MotionEvent.AXIS_RTRIGGER -> "R2"
        MotionEvent.AXIS_HAT_X -> "HatX (крестовина ←→)"
        MotionEvent.AXIS_HAT_Y -> "HatY (крестовина ↑↓)"
        MotionEvent.AXIS_GAS -> "Gas"
        MotionEvent.AXIS_BRAKE -> "Brake"
        MotionEvent.AXIS_RX -> "RX"
        MotionEvent.AXIS_RY -> "RY"
        else -> "Axis($axis)"
    }

    private fun fmt1(v: Float): String = String.format("%.2f", v)
}