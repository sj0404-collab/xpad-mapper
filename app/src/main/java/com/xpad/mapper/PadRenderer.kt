package com.xpad.mapper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max

/**
 * Рисует Xbox-стиль геймпад и подсвечивает нажатия/стики в реальном времени.
 */
class PadRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val buttons = BooleanArray(LogicalButton.entries.size)
    private var lx = 0f; private var ly = 0f
    private var rx = 0f; private var ry = 0f
    private var lt = 0f; private var rt = 0f

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 30, 32, 40); style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 96, 102, 118); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 50, 53, 64); style = Paint.Style.FILL
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 152, 160, 178); style = Paint.Style.FILL
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 0, 200, 150); style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 30f
    }
    private val pressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 255, 220, 80); style = Paint.Style.FILL
    }

    private val buttonPaint = HashMap<Int, Paint>()

    fun setButton(btn: LogicalButton, pressed: Boolean) {
        buttons[btn.ordinal] = pressed
        invalidate()
    }

    fun setLeftStick(x: Float, y: Float) { lx = x; ly = y; invalidate() }
    fun setRightStick(x: Float, y: Float) { rx = x; ry = y; invalidate() }
    fun setTriggers(l: Float, r: Float) { lt = l; rt = r; invalidate() }

    fun reset() {
        buttons.fill(false); lx = 0f; ly = 0f; rx = 0f; ry = 0f; lt = 0f; rt = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f

        val body = RectF(w * 0.04f, h * 0.07f, w * 0.96f, h * 0.93f)
        canvas.drawRoundRect(body, h * 0.14f, h * 0.14f, bodyPaint)
        canvas.drawRoundRect(body, h * 0.14f, h * 0.14f, strokePaint)

        val s = w * 0.13f          // характерный размер
        val lyc = cy + h * 0.04f   // левая колонка
        val ryc = cy - h * 0.02f   // правая колонка (стик ниже)

        // Левый стик
        val lStickCx = cx - w * 0.30f
        drawStick(canvas, lStickCx, cy - h * 0.22f, s, lx, ly, LogicalButton.L3)
        // Правый стик
        val rStickCx = cx + w * 0.30f
        drawStick(canvas, rStickCx, cy + h * 0.10f, s, rx, ry, LogicalButton.R3)

        // ABXY ромб
        val ax = cx + w * 0.30f; val ay = cy - h * 0.22f
        drawFaceButton(canvas, X = LogicalButton.X, cx0 = ax - s * 0.9f, cy0 = ay - s * 0.9f, ch = "X")
        drawFaceButton(canvas, X = LogicalButton.Y, cx0 = ax + s * 0.9f, cy0 = ay - s * 0.9f, ch = "Y")
        drawFaceButton(canvas, X = LogicalButton.A, cx0 = ax - s * 0.9f, cy0 = ay + s * 0.9f, ch = "A")
        drawFaceButton(canvas, X = LogicalButton.B, cx0 = ax + s * 0.9f, cy0 = ay + s * 0.9f, ch = "B")

        // Крестовина
        val dcx = cx - w * 0.30f; val dcy = cy + h * 0.10f
        drawDpad(canvas, dcx, dcy, s)

        // Бамперы и триггеры
        val bumpW = w * 0.30f; val bumpH = h * 0.06f
        drawBumper(canvas, LogicalButton.LB, cx - w * 0.04f - bumpW, h * 0.025f, bumpW, LogicalButton.LT, "L2 / LB")
        drawBumper(canvas, LogicalButton.RB, cx + w * 0.04f, h * 0.025f, bumpW, LogicalButton.RT, "RT / RB")

        // Левый триггер полоска
        drawTriggerBar(canvas, lt, cx - w * 0.40f, w * 0.35f)
        drawTriggerBar(canvas, rt, cx + w * 0.05f, w * 0.35f)

        // Start / Select / Home
        drawCenter(canvas, LogicalButton.SELECT, cx - w * 0.12f, cy, "⚙")
        drawCenter(canvas, LogicalButton.HOME, cx, cy, "◉")
        drawCenter(canvas, LogicalButton.START, cx + w * 0.12f, cy, "≡")
    }

    private fun drawStick(canvas: Canvas, cx: Float, cy: Float, s: Float, vx: Float, vy: Float, press: LogicalButton) {
        canvas.drawCircle(cx, cy, s, basePaint)
        val mag = max(abs(vx), abs(vy))
        val kx = vx * s * 0.55f
        val ky = vy * s * 0.55f
        val p = if (mag > 0.08f) activePaint else knobPaint
        canvas.drawCircle(cx + kx, cy + ky, s * 0.46f, p)
        canvas.drawCircle(cx, cy, s * 0.46f, strokePaint)
        labelPaint.textSize = s * 0.42f
        if (buttons[press.ordinal]) {
            canvas.drawCircle(cx, cy, s * 0.20f, pressPaint)
            canvas.drawText("L3", cx, cy + s * 0.52f, labelPaint)
        } else {
            canvas.drawText(if (press == LogicalButton.L3) "L3" else "R3", cx, cy + s * 0.52f, labelPaint)
        }
    }

    private fun drawDpad(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        val arm = s * 0.62f; val tw = s * 0.46f
        drawDpadArm(canvas, cx - s * 0.62f, cy, tw, s * 1.9f, LogicalButton.DPAD_LEFT)
        drawDpadArm(canvas, cx - s * 0.62f, cy + 0f, tw, s * 1.9f, LogicalButton.DPAD_LEFT)
        drawDpadArm(canvas, cx + s * 0.62f - tw, cy, tw, s * 1.9f, LogicalButton.DPAD_RIGHT)
        drawDpadArm(canvas, cx, cy - s * 0.62f, s * 1.9f, tw, LogicalButton.DPAD_UP)
        drawDpadArm(canvas, cx, cy + s * 0.62f - tw, s * 1.9f, tw, LogicalButton.DPAD_DOWN)
        canvas.drawCircle(cx, cy, tw * 0.5f, basePaint)
    }

    private fun drawDpadArm(canvas: Canvas, l: Float, t: Float, w: Float, h: Float, btn: LogicalButton) {
        val rect = RectF(l, t, l + w, t + h)
        val p = if (buttons[btn.ordinal]) activePaint else basePaint
        canvas.drawRoundRect(rect, w * 0.22f, h * 0.22f, p)
    }

    private fun drawFaceButton(canvas: Canvas, X: LogicalButton, cx0: Float, cy0: Float, ch: String) {
        val r = sFace
        canvas.drawCircle(cx0, cy0, r, faceColor(X))
        labelPaint.textSize = r * 1.2f
        canvas.drawText(ch, cx0, cy0 + r * 0.38f, labelPaint)
    }

    private val sFace: Float get() = h * 0.075f

    private fun faceColor(btn: LogicalButton): Paint {
        val color = when (btn) {
            LogicalButton.A -> 0xFF2E7D32.toInt()
            LogicalButton.B -> 0xFFC62828.toInt()
            LogicalButton.X -> 0xFF1565C0.toInt()
            LogicalButton.Y -> 0xFFF9A825.toInt()
            else -> 0xFF4A4A5A.toInt()
        }
        return buttonPaint.getOrPut(color) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
        }
    }

    private fun has(btn: LogicalButton): Boolean = buttons[btn.ordinal]

    private fun drawBumper(canvas: Canvas, btn: LogicalButton, cx: Float, cy: Float, w: Float, trig: LogicalButton, ch: String) {
        val bh = h * 0.05f
        val p = if (has(btn)) activePaint else basePaint
        val rect = RectF(cx, cy, cx + w, cy + bh)
        canvas.drawRoundRect(rect, bh * 0.5f, bh * 0.5f, p)
        labelPaint.textSize = h * 0.045f
        canvas.drawText(ch, cx + w / 2f, cy + bh * 0.66f, labelPaint)
    }

    private fun drawTriggerBar(canvas: Canvas, value: Float, cx: Float, w: Float) {
        val bh = h * 0.012f
        val top = h * 0.02f - bh / 2f
        canvas.drawRect(RectF(cx, top, cx + w * value.coerceIn(0f, 1f), top + bh), activePaint)
        canvas.drawRect(RectF(cx, top, cx + w, top + bh), strokePaint)
    }

    private fun drawCenter(canvas: Canvas, btn: LogicalButton, cx: Float, cy: Float, ch: String) {
        val r = h * 0.03f
        val p = if (has(btn)) activePaint else basePaint
        canvas.drawCircle(cx, cy, r * 1.5f, p)
        canvas.drawText(ch, cx, cy + r * 0.55f, labelPaint)
    }
}