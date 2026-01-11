package com.example.weatherapp.domain.model

data class DailyForecastItem(
    val date: String, // "2026-01-11"
    val dayOfWeek: String, // "ПН", "ВТ" и т.д.
    val tempMin: Int,
    val tempMax: Int,
    val weatherCode: Int,
    val isToday: Boolean = false,
    val isTomorrow: Boolean = false,
    val isYesterday: Boolean = false
)