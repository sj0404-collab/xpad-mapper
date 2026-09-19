package com.xpad.mapper

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/** Настройки оси (стики/триггеры) в профиле. */
data class AxisOptions(
    val deadZone: Float = 0.12f,
    val invert: Boolean = false,
    val expCurve: Boolean = false
) {
    // dead zone остаётся таким же при apply (объект копируется при вводе)

    /** Применить дед-зону, инверсию и кривую к значению из [-1..1]. */
    fun process(v: Float): Float {
        var out = v
        if (invert) out = -out
        val adz = deadZone.coerceIn(0f, 0.95f)
        val norm = abs(out)
        if (norm < adz) return 0f
        val scaled = (norm - adz) / (1f - adz)
        val curved = if (expCurve) scaled.pow(1.6f) else scaled.pow(0.85f)
        return sign(out) * curved
    }

    companion object {
        fun default() = AxisOptions()
    }
}

/** Пара состояний стика/триггера после обработки маппером. */
data class StickValue(val x: Float = 0f, val y: Float = 0f, val magnitude: Float = 0f)

/** Профиль ремапа под конкретный девайс. */
data class ControllerProfile(
    val deviceName: String,
    val deviceId: Int,
    val vendor: Int,
    val product: Int,
    val buttonMap: Map<Int, LogicalButton> = emptyMap(),
    val axisOptions: Map<Int, AxisOptions> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

/** Преобразует сырые события Android в «логический» геймпад Xbox-стиля. */
class Mapper(private val profile: ControllerProfile?) {

    private val axes = profile?.axisOptions ?: emptyMap()

    fun mapKey(keyCode: Int): LogicalButton? =
        profile?.buttonMap?.get(keyCode)

    fun optionsFor(axis: Int): AxisOptions = axes[axis] ?: AxisOptions()

    /** Обрабатывает значение оси из MotionEvent (уже нормализовано в -1..1). */
    fun processAxis(axis: Int, raw: Float): Float {
        val core = raw.coerceIn(-1f, 1f)
        if (core == 0f) return 0f
        val opts = optionsFor(axis)
        if (opts.deadZone <= 0f && !opts.invert && !opts.expCurve) return core
        return opts.process(core)
    }
}

object AxisUtil {
    fun normalize(range: android.view.InputDevice.MotionRange, v: Float): Float {
        val max = abs(range.max)
        val min = abs(range.min)
        val m = maxOf(max, min, 1f)
        return (v / m).coerceIn(-1f, 1f)
    }
}