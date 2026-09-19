package com.xpad.mapper

import android.view.KeyEvent

/**
 * Логические (Xbox-стиль) кнопки, к которым ремапится профиль.
 * Каждая кнопка имеет ключ [constName] для хранения в JSON профиля.
 */
enum class LogicalButton(val labelRu: String, val fullLabel: String, val colorHue: Int) {
    DPAD_UP("D-Pad ↑", "Крестовина вверх", 210),
    DPAD_DOWN("D-Pad ↓", "Крестовина вниз", 210),
    DPAD_LEFT("D-Pad ←", "Крестовина влево", 210),
    DPAD_RIGHT("D-Pad →", "Крестовина вправо", 210),
    A("A", "Кнопка A (нижняя)", 130),
    B("B", "Кнопка B (правая)", 0),
    X("X", "Кнопка X (левая)", 240),
    Y("Y", "Кнопка Y (верхняя)", 55),
    LB("LB", "Левый бампер", 260),
    RB("RB", "Правый бампер", 260),
    LT("LT", "Левый триггер", 260),
    RT("RT", "Правый триггер", 260),
    SELECT("SELECT", "Кнопка Select (/)", 280),
    START("START", "Кнопка Start (☰)", 280),
    HOME("HOME", "Кнопка Home/Guide", 150),
    L3("L3", "Нажатие левого стика", 190),
    R3("R3", "Нажатие правого стика", 190);

    val defaultKeycode: Int? by lazy {
        when (this) {
            DPAD_UP -> KeyEvent.KEYCODE_DPAD_UP
            DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
            DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
            DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
            A -> KeyEvent.KEYCODE_BUTTON_A
            B -> KeyEvent.KEYCODE_BUTTON_B
            X -> KeyEvent.KEYCODE_BUTTON_X
            Y -> KeyEvent.KEYCODE_BUTTON_Y
            LB -> KeyEvent.KEYCODE_BUTTON_L1
            RB -> KeyEvent.KEYCODE_BUTTON_R1
            LT -> KeyEvent.KEYCODE_BUTTON_L2
            RT -> KeyEvent.KEYCODE_BUTTON_R2
            SELECT -> KeyEvent.KEYCODE_BUTTON_SELECT
            START -> KeyEvent.KEYCODE_BUTTON_START
            HOME -> KeyEvent.KEYCODE_BUTTON_MODE
            L3 -> KeyEvent.KEYCODE_BUTTON_THUMBL
            R3 -> KeyEvent.KEYCODE_BUTTON_THUMBR
        }
    }

    companion object {
        fun fromConstName(name: String): LogicalButton? =
            entries.firstOrNull { it.name == name }

        /** Логическая кнопка по Android-коду (стандартная раскладка). */
        fun fromKeyCode(keyCode: Int): LogicalButton? =
            entries.firstOrNull { it.defaultKeycode == keyCode }
    }
}

/** Человекочитаемое имя Android-кода кнопки. */
object KeyNames {
    private val map: Map<Int, String> = mapOf(
        KeyEvent.KEYCODE_BUTTON_A to "A",
        KeyEvent.KEYCODE_BUTTON_B to "B",
        KeyEvent.KEYCODE_BUTTON_C to "C",
        KeyEvent.KEYCODE_BUTTON_X to "X",
        KeyEvent.KEYCODE_BUTTON_Y to "Y",
        KeyEvent.KEYCODE_BUTTON_Z to "Z",
        KeyEvent.KEYCODE_BUTTON_L1 to "L1",
        KeyEvent.KEYCODE_BUTTON_R1 to "R1",
        KeyEvent.KEYCODE_BUTTON_L2 to "L2",
        KeyEvent.KEYCODE_BUTTON_R2 to "R2",
        KeyEvent.KEYCODE_BUTTON_SELECT to "Select",
        KeyEvent.KEYCODE_BUTTON_START to "Start",
        KeyEvent.KEYCODE_BUTTON_THUMBL to "ThumbL",
        KeyEvent.KEYCODE_BUTTON_THUMBR to "ThumbR",
        KeyEvent.KEYCODE_BUTTON_MODE to "Home",
        KeyEvent.KEYCODE_DPAD_UP to "D-pad ↑",
        KeyEvent.KEYCODE_DPAD_DOWN to "D-pad ↓",
        KeyEvent.KEYCODE_DPAD_LEFT to "D-pad ←",
        KeyEvent.KEYCODE_DPAD_RIGHT to "D-pad →",
        KeyEvent.KEYCODE_BUTTON_1 to "Button 1",
        KeyEvent.KEYCODE_BUTTON_2 to "Button 2",
        KeyEvent.KEYCODE_BUTTON_3 to "Button 3",
        KeyEvent.KEYCODE_BUTTON_4 to "Button 4",
        KeyEvent.KEYCODE_BUTTON_5 to "Button 5",
        KeyEvent.KEYCODE_BUTTON_6 to "Button 6",
        KeyEvent.KEYCODE_BUTTON_7 to "Button 7",
        KeyEvent.KEYCODE_BUTTON_8 to "Button 8",
        KeyEvent.KEYCODE_BUTTON_9 to "Button 9",
        KeyEvent.KEYCODE_BUTTON_10 to "Button 10",
        KeyEvent.KEYCODE_BUTTON_11 to "Button 11",
        KeyEvent.KEYCODE_BUTTON_12 to "Button 12",
        KeyEvent.KEYCODE_BUTTON_13 to "Button 13",
        KeyEvent.KEYCODE_BUTTON_14 to "Button 14",
        KeyEvent.KEYCODE_BUTTON_15 to "Button 15",
        KeyEvent.KEYCODE_BUTTON_16 to "Button 16"
    )

    fun of(keyCode: Int): String = map[keyCode] ?: "Key($keyCode)"
}