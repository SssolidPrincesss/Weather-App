package com.example.weatherapp.domain.model

enum class WeatherCondition {
    CLEAR,
    CLOUDY,
    RAIN,
    SNOW,
    FOGGY // ⭐ Новый тип для тумана
}

enum class TimeOfDay {
    DAY, NIGHT
}

// ⭐ НОВЫЙ ENUM: Время года
enum class Season {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN
}