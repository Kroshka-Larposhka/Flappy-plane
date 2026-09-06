package com.example.flappyplane

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
    private val pipeGap = 380f
    private val pipeSpeed = 6f
    private val planeSize = 90f
    private val pipeSpawnInterval = 90

    private var planeX = 0f
    private var planeY = 0f
    private var planeVelocity = 0f
    private var planeRotation = 0f

    private data class Pipe(var x: Float, val gapTop: Float, var scored: Boolean = false)
    private val pipes = mutableListOf<Pipe>()
    private var frameCount = 0

    private var score = 0
    private var isRunning = false
    private var isGameOver = false
    private var groundY = 0f

    private val skyPaint = Paint().apply { color = Color.parseColor("#87CEEB") }
    private val groundPaint = Paint().apply { color = Color.parseColor("#DEB887") }
    private val pipePaint = Paint().apply { color = Color.parseColor("#4CAF50") }
    private val planePaint = Paint().apply { color = Color.parseColor("#F44336") }
    private val planeWindowPaint = Paint().apply { color = Color.WHITE }
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 90f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val messagePaint = Paint().apply {
        color = Color.WHITE
        textSize = 55f
        textAlign = Paint.Align.CENTER
    }

    init {
        resetGame()
    }

    private fun resetGame() {
        planeVelocity = 0f
        pipes.clear()
        frameCount = 0
        score = 0
        isGameOver = false
        isRunning = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        groundY = h * 0.85f
        planeX = w * 0.25f
        planeY = h * 0.4f
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
            val minGapTop = height * 0.15f
            val maxGapTop = groundY - pipeGap - height * 0.1f
            val gapTop = Random.nextFloat() * (maxGapTop - minGapTop) + minGapTop
            pipes.add(Pipe(width.toFloat(), gapTop))
        }

        val planeRect = RectF(
            planeX - planeSize / 2, planeY - planeSize / 2,
            planeX + planeSize / 2, planeY + planeSize / 2
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

        if (planeY + planeSize / 2 > groundY || planeY - planeSize / 2 < 0) {
            gameOver()
        }
    }

    private fun gameOver() {
        isGameOver = true
        isRunning = false
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint)

        for (pipe in pipes) {
            canvas.drawRect(pipe.x, 0f, pipe.x + pipeWidth, pipe.gapTop, pipePaint)
            canvas.drawRect(pipe.x, pipe.gapTop + pipeGap, pipe.x + pipeWidth, groundY, pipePaint)
        }

        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), groundPaint)

        canvas.save()
        canvas.translate(planeX, planeY)
        canvas.rotate(planeRotation)
        canvas.drawRect(-planeSize / 2, -planeSize / 4, planeSize / 2, planeSize / 4, planePaint)
        canvas.drawCircle(planeSize / 5, 0f, planeSize / 6, planeWindowPaint)
        canvas.restore()

        canvas.drawText(score.toString(), width / 2f, 150f, scorePaint)

        if (!isRunning && !isGameOver) {
            canvas.drawText("Тапни по экрану, чтобы взлететь", width / 2f, height / 2f, messagePaint)
        }
        if (isGameOver) {
            canvas.drawText("Игра
