package com.example.weatherapp.ui.main

//график с градиентом для виджета с возходом/закатом

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class SunriseGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    private val paint = Paint().apply {
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val path = Path()
    private val gradientPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 15f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Очищаем фон — делаем прозрачным
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Создаём путь — дуга
        path.reset()
        path.moveTo(0f, height * 0.8f)
        path.cubicTo(
            width * 0.25f, height * 0.3f,
            width * 0.75f, height * 0.3f,
            width, height * 0.8f
        )

        // Градиент: 99.9% — жёлтый, 0.1% — чёрный (самый резкий переход)
        val colors = intArrayOf(
            Color.parseColor("#FFE900"),  // ярко-жёлтый
            Color.parseColor("#808080")   // серый
        )
        val positions = floatArrayOf(0.5f, 0.8f)  // ⭐ самый резкий переход

        val shader = LinearGradient(
            0f, 0f, 0f, height,
            colors, positions, Shader.TileMode.CLAMP
        )
        gradientPaint.shader = shader

        // Рисуем градиентную линию
        canvas.drawPath(path, gradientPaint)
    }
}