package com.example.weatherapp.utils

import com.example.weatherapp.domain.model.Season
import java.util.Calendar
import java.util.Locale

object SeasonDetector {

    fun getCurrentSeason(): Season {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1 // 1-12
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return when (month) {
            12, 1, 2 -> Season.WINTER
            3, 4, 5 -> Season.SPRING
            6, 7, 8 -> Season.SUMMER
            9, 10, 11 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }

    // Для точного определения (с учётом даты начала сезона)
    fun getSeasonByDate(day: Int, month: Int): Season {
        return when {
            month == 12 || month == 1 || month == 2 -> Season.WINTER
            month == 3 && day < 21 -> Season.WINTER
            month == 3 && day >= 21 -> Season.SPRING
            month in 4..5 -> Season.SPRING
            month == 6 && day < 21 -> Season.SPRING
            month == 6 && day >= 21 -> Season.SUMMER
            month in 7..8 -> Season.SUMMER
            month == 9 && day < 23 -> Season.SUMMER
            month == 9 && day >= 23 -> Season.AUTUMN
            month in 10..11 -> Season.AUTUMN
            month == 12 && day < 22 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }
}