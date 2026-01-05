package com.example.weatherapp.ui.main

/*
почасовая температурная кривая
часть виджета с почасовой погодой
 */

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.recyclerview.widget.RecyclerView

class TemperatureLineDecorator(
    private val temperatures: List<Double>
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = Color.parseColor("#29B6F6")
        strokeWidth = 10f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        if (temperatures.size < 2) return

        val childCount = parent.childCount
        val points = mutableListOf<PointF>()

        // 🔍 Находим min и max температуру
        val minTemp = temperatures.minOrNull() ?: 0.0
        val maxTemp = temperatures.maxOrNull() ?: 0.0
        val tempRange = maxTemp - minTemp

        // 📏 Определяем область для графика (ниже температур, выше влажности)
        val graphTop = parent.height * 0.5f  // 50% высоты RecyclerView
        val graphBottom = parent.height * 0.7f  // 70% высоты RecyclerView
        val graphHeight = graphBottom - graphTop

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position >= 0 && position < temperatures.size) {
                val centerX = child.x + child.width / 2f
                val temp = temperatures[position]

                // 📈 Вычисляем Y-координату на основе температуры
                val normalizedY = if (tempRange > 0) {
                    1 - ((temp - minTemp) / tempRange)
                } else {
                    0.5
                }
                val y = graphTop + (normalizedY * graphHeight).toFloat()

                points.add(PointF(centerX, y))
            }
        }

        if (points.size < 2) return

        // Рисуем линию
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        c.drawPath(path, paint)

        // Рисуем точки
        for (point in points) {
            c.drawCircle(point.x, point.y, 5f, paint.apply { color = Color.WHITE })
        }
    }
}