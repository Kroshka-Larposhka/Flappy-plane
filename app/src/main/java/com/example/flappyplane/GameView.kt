package com.example.flappyplane

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Choreographer.FrameCallback {

    private val gravity = 0.6f
    private val flapPower = -11f
    private val pipeWidth = 160f
    private val pipeGap = 420f
    private val pipeSpeed = 6f
    private val planeSize = 90f
    private val pipeSpawnInterval = 90
    private val maxGapShiftFraction = 0.28f

    private var planeX = 0f
    private var planeY = 0f
    private var planeVelocity = 0f
    private var planeRotation = 0f

    private data class Pipe(var x: Float, val gapTop: Float, var scored: Boolean = false)
    private val pipes = mutableListOf<Pipe>()
    private var frameCount = 0
    private var lastGapTop: Float? = null

    private data class Cloud(var x: Float, val y: Float, val scale: Float)
    private val clouds = mutableListOf<Cloud>()

    private var score = 0
    private var isRunning = false
    private var isGameOver = false
    private var groundY = 0f

    // ---- Небо и фон ----
    private val skyPaint = Paint()
    private val sunGlowPaint = Paint().apply {
        color = Color.parseColor("#FFF59D")
        isAntiAlias = true
    }
    private val sunPaint = Paint().apply {
        color = Color.parseColor("#FFEB3B")
        isAntiAlias = true
    }
    private val cloudPaint = Paint().apply {
        color = Color.WHITE
        alpha = 230
        isAntiAlias = true
    }

    // ---- Земля ----
    private val groundPaint = Paint()
    private val grassPaint = Paint().apply {
        color = Color.parseColor("#6FBF73")
        isAntiAlias = true
    }

    // ---- Трубы ----
    private val pipePaint = Paint().apply {
        color = Color.parseColor("#43A047")
        isAntiAlias = true
    }
    private val pipeOutlinePaint = Paint().apply {
        color = Color.parseColor("#2E7D32")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    private val pipeHighlightPaint = Paint().apply {
        color = Color.parseColor("#81C784")
        alpha = 160
        isAntiAlias = true
    }
    private val pipeCapPaint = Paint().apply {
        color = Color.parseColor("#388E3C")
        isAntiAlias = true
    }

    // ---- Самолёт ----
    private val fuselagePaint = Paint().apply {
        color = Color.parseColor("#E53935")
        isAntiAlias = true
    }
    private val fuselageOutlinePaint = Paint().apply {
        color = Color.parseColor("#8B0000")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val tailFinPaint = Paint().apply {
        color = Color.parseColor("#B71C1C")
        isAntiAlias = true
    }
    private val tailWingPaint = Paint().apply {
        color = Color.parseColor("#C62828")
        isAntiAlias = true
    }
    private val mainWingPaint = Paint().apply {
        color = Color.parseColor("#ECEFF1")
        isAntiAlias = true
    }
    private val mainWingOutlinePaint = Paint().apply {
        color = Color.parseColor("#90A4AE")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val spinnerBlurPaint = Paint().apply {
        color = Color.parseColor("#BDBDBD")
        alpha = 130
        isAntiAlias = true
    }
    private val spinnerPaint = Paint().apply {
        color = Color.parseColor("#424242")
        isAntiAlias = true
    }
    private val windowPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    private val windowRimPaint = Paint().apply {
        color = Color.parseColor("#29B6F6")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val motionLinePaint = Paint().apply {
        color = Color.WHITE
        alpha = 150
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    // ---- Текст ----
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 90f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(6f, 2f, 3f, Color.argb(160, 0, 0, 0))
    }
    private val messagePaint = Paint().apply {
        color = Color.WHITE
        textSize = 55f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val messagePanelPaint = Paint().apply {
        color = Color.BLACK
        alpha = 110
        isAntiAlias = true
    }

    // ---- Форма самолёта (считается один раз при создании) ----
    private val fuselagePath = Path()
    private val tailFinPath = Path()
    private val tailWingPath = Path()
    private val mainWingPath = Path()
    private var windowCx = 0f
    private var windowCy = 0f
    private var windowR = 0f
    private var noseX = 0f
    private var noseR = 0f
    private var noseBlurR = 0f

    init {
        buildPlaneShape()
        resetGame()
    }

    private fun buildPlaneShape() {
        val half = planeSize / 2f
        val bodyHalf = planeSize / 4f

        fuselagePath.apply {
            moveTo(-half * 0.6f, -bodyHalf * 0.65f)
            lineTo(half * 0.55f, -bodyHalf * 0.65f)
            lineTo(half * 0.85f, 0f)
            lineTo(half * 0.55f, bodyHalf * 0.65f)
            lineTo(-half * 0.6f, bodyHalf * 0.65f)
            quadTo(-half * 0.78f, bodyHalf * 0.65f, -half * 0.78f, 0f)
            quadTo(-half * 0.78f, -bodyHalf * 0.65f, -half * 0.6f, -bodyHalf * 0.65f)
            close()
        }

        tailFinPath.apply {
            moveTo(-half * 0.75f, -bodyHalf * 0.6f)
            lineTo(-half * 0.55f, -bodyHalf * 0.6f)
            lineTo(-half * 0.68f, -bodyHalf * 1.6f)
            close()
        }

        tailWingPath.apply {
            moveTo(-half * 0.78f, bodyHalf * 0.3f)
            lineTo(-half * 0.55f, bodyHalf * 0.3f)
            lineTo(-half * 0.85f, bodyHalf * 1.3f)
            lineTo(-half * 0.95f, bodyHalf * 1.1f)
            close()
        }

        mainWingPath.apply {
            moveTo(-half * 0.15f, bodyHalf * 0.5f)
            lineTo(half * 0.15f, bodyHalf * 0.5f)
            lineTo(half * 0.05f, bodyHalf * 1.8f)
            lineTo(-half * 0.35f, bodyHalf * 1.8f)
            close()
        }

        windowCx = -half * 0.05f
        windowCy = 0f
        windowR = bodyHalf * 0.35f

        noseX = half * 0.85f
        noseR = bodyHalf * 0.25f
        noseBlurR = bodyHalf * 0.55f
    }

    private fun resetGame() {
        planeVelocity = 0f
        pipes.clear()
        frameCount = 0
        lastGapTop = null
        score = 0
        isGameOver = false
        isRunning = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        groundY = h * 0.85f
        planeX = w * 0.25f
        planeY = h * 0.4f

        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.parseColor("#87CEEB"), Color.parseColor("#D2F3FE"),
            Shader.TileMode.CLAMP
        )
        groundPaint.shader = LinearGradient(
            0f, groundY, 0f, h.toFloat(),
            Color.parseColor("#C68958"), Color.parseColor("#A9713F"),
            Shader.TileMode.CLAMP
        )

        clouds.clear()
        clouds.add(Cloud(w * 0.18f, h * 0.15f, 1f))
        clouds.add(Cloud(w * 0.55f, h * 0.24f, 0.7f))
        clouds.add(Cloud(w * 0.82f, h * 0.11f, 0.55f))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when {
                isGameOver -> {
                    resetGame()
                    planeY = height * 0.4f
                    startLoop()
                }
                !isRunning -> {
                    startLoop()
                    planeVelocity = flapPower
                }
                else -> planeVelocity = flapPower
            }
        }
        return true
    }

    private fun startLoop() {
        isRunning = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (isRunning && !isGameOver) {
            update()
            Choreographer.getInstance().postFrameCallback(this)
        }
        invalidate()
    }

    private fun update() {
        frameCount++

        planeVelocity += gravity
        planeY += planeVelocity
        planeRotation = (planeVelocity * 2.5f).coerceIn(-30f, 70f)

        if (frameCount % pipeSpawnInterval == 0) {
            spawnPipe()
        }

        val cloudSpeed = pipeSpeed * 0.25f
        for (cloud in clouds) {
            cloud.x -= cloudSpeed
            val margin = 140f * cloud.scale
            if (cloud.x < -margin) {
                cloud.x = width + margin
            }
        }

        val hitboxMargin = 8f
        val planeHalfWidth = planeSize / 2 - hitboxMargin
        val planeHalfHeight = planeSize / 4 - hitboxMargin
        val planeRect = RectF(
            planeX - planeHalfWidth, planeY - planeHalfHeight,
            planeX + planeHalfWidth, planeY + planeHalfHeight
        )

        val iterator = pipes.iterator()
        while (iterator.hasNext()) {
            val pipe = iterator.next()
            pipe.x -= pipeSpeed

            if (pipe.x + pipeWidth < 0) {
                iterator.remove()
                continue
            }

            if (!pipe.scored && pipe.x + pipeWidth < planeX) {
                pipe.scored = true
                score++
            }

            val topPipeRect = RectF(pipe.x, 0f, pipe.x + pipeWidth, pipe.gapTop)
            val bottomPipeRect = RectF(pipe.x, pipe.gapTop + pipeGap, pipe.x + pipeWidth, groundY)
            if (RectF.intersects(planeRect, topPipeRect) || RectF.intersects(planeRect, bottomPipeRect)) {
                gameOver()
            }
        }

        if (planeY + planeHalfHeight > groundY || planeY - planeHalfHeight < 0) {
            gameOver()
        }
    }

    private fun spawnPipe() {
        val minGapTop = height * 0.12f
        val maxGapTop = (groundY - pipeGap - height * 0.08f).coerceAtLeast(minGapTop)

        val gapTop = if (lastGapTop == null || maxGapTop <= minGapTop) {
            Random.nextFloat() * (maxGapTop - minGapTop) + minGapTop
        } else {
            val maxShift = height * maxGapShiftFraction
            val low = (lastGapTop!! - maxShift).coerceAtLeast(minGapTop)
            val high = (lastGapTop!! + maxShift).coerceAtMost(maxGapTop)
            if (high <= low) low else Random.nextFloat() * (high - low) + low
        }

        lastGapTop = gapTop
        pipes.add(Pipe(width.toFloat(), gapTop))
    }

    private fun gameOver() {
        isGameOver = true
        isRunning = false
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint)

        val sunX = width * 0.82f
        val sunY = height * 0.15f
        canvas.drawCircle(sunX, sunY, 46f, sunGlowPaint)
        canvas.drawCircle(sunX, sunY, 30f, sunPaint)

        for (cloud in clouds) {
            drawCloud(canvas, cloud.x, cloud.y, cloud.scale)
        }

        for (pipe in pipes) {
            drawPipe(canvas, pipe.x, 0f, pipe.gapTop, isTop = true)
            drawPipe(canvas, pipe.x, pipe.gapTop + pipeGap, groundY, isTop = false)
        }

        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), groundPaint)
        canvas.drawRect(0f, groundY, width.toFloat(), groundY + 10f, grassPaint)

        canvas.save()
        canvas.translate(planeX, planeY)
        canvas.rotate(planeRotation)
        drawPlane(canvas)
        canvas.restore()

        canvas.drawText(score.toString(), width / 2f, 150f, scorePaint)

        if (!isRunning && !isGameOver) {
            drawMessage(canvas, "Тапни по экрану, чтобы взлететь", height / 2f)
        }
        if (isGameOver) {
            drawMessage(canvas, "Игра окончена. Счёт: $score", height / 2f)
            drawMessage(canvas, "Тапни, чтобы начать заново", height / 2f + 90f)
        }
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        canvas.drawCircle(cx, cy, 20f * scale, cloudPaint)
        canvas.drawCircle(cx + 18f * scale, cy - 4f * scale, 24f * scale, cloudPaint)
        canvas.drawCircle(cx - 20f * scale, cy + 2f * scale, 16f * scale, cloudPaint)
    }

    private fun drawPipe(canvas: Canvas, x: Float, top: Float, bottom: Float, isTop: Boolean) {
        val rect = RectF(x, top, x + pipeWidth, bottom)
        canvas.drawRect(rect, pipePaint)
        canvas.drawRect(rect, pipeOutlinePaint)

        canvas.drawRect(x + pipeWidth * 0.12f, top, x + pipeWidth * 0.3f, bottom, pipeHighlightPaint)

        val capHeight = 26f
        val capOverhang = 10f
        val capRect = if (isTop) {
            RectF(x - capOverhang, bottom - capHeight, x + pipeWidth + capOverhang, bottom)
        } else {
            RectF(x - capOverhang, top, x + pipeWidth + capOverhang, top + capHeight)
        }
        canvas.drawRect(capRect, pipeCapPaint)
        canvas.drawRect(capRect, pipeOutlinePaint)
    }

    private fun drawPlane(canvas: Canvas) {
        if (isRunning) {
            val half = planeSize / 2f
            val bodyHalf = planeSize / 4f
            canvas.drawLine(-half * 1.3f, -bodyHalf * 0.5f, -half * 0.95f, -bodyHalf * 0.5f, motionLinePaint)
            canvas.drawLine(-half * 1.45f, 0f, -half * 0.95f, 0f, motionLinePaint)
            canvas.drawLine(-half * 1.25f, bodyHalf * 0.5f, -half * 0.95f, bodyHalf * 0.5f, motionLinePaint)
        }

        canvas.drawPath(mainWingPath, mainWingPaint)
        canvas.drawPath(mainWingPath, mainWingOutlinePaint)
        canvas.drawPath(tailWingPath, tailWingPaint)
        canvas.drawPath(tailFinPath, tailFinPaint)
        canvas.drawPath(fuselagePath, fuselagePaint)
        canvas.drawPath(fuselagePath, fuselageOutlinePaint)

        canvas.drawCircle(noseX, 0f, noseBlurR, spinnerBlurPaint)
        canvas.drawCircle(noseX, 0f, noseR, spinnerPaint)

        canvas.drawCircle(windowCx, windowCy, windowR, windowPaint)
        canvas.drawCircle(windowCx, windowCy, windowR, windowRimPaint)
    }

    private fun drawMessage(canvas: Canvas, text: String, y: Float) {
        val textWidth = messagePaint.measureText(text)
        val paddingH = 28f
        val paddingV = 18f
        val rect = RectF(
            width / 2f - textWidth / 2f - paddingH,
            y - messagePaint.textSize - paddingV / 2f,
            width / 2f + textWidth / 2f + paddingH,
            y + paddingV
        )
        canvas.drawRoundRect(rect, 20f, 20f, messagePanelPaint)
        canvas.drawText(text, width / 2f, y, messagePaint)
    }
}
}