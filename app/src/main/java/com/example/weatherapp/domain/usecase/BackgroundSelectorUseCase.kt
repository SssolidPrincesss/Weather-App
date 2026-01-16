// domain/usecase/BackgroundSelectorUseCase.kt
package com.example.weatherapp.domain.usecase

import com.example.weatherapp.domain.model.Season
import com.example.weatherapp.domain.model.TimeOfDay
import com.example.weatherapp.domain.model.WeatherCondition
import com.example.weatherapp.utils.Output
import com.example.weatherapp.utils.SeasonDetector
import com.example.weatherapp.utils.TimeUtils

class BackgroundSelectorUseCase {
    data class Input(
        val weatherCode: Int // ⭐ Используем код, а не текст
    )

    fun execute(input: Input): Output { // ⭐ Без <T>
        val weatherCondition = when {
            input.weatherCode == 0 -> WeatherCondition.CLEAR
            input.weatherCode in 1..3 -> WeatherCondition.CLOUDY
            input.weatherCode in 45..48 -> WeatherCondition.FOGGY
            input.weatherCode in 51..67 || input.weatherCode in 80..82 -> WeatherCondition.RAIN
            input.weatherCode in 71..77 -> WeatherCondition.SNOW
            else -> WeatherCondition.CLOUDY
        }

        val timeOfDay = TimeUtils.getCurrentTimeOfDay()
        val season = SeasonDetector.getCurrentSeason()

        return Output(weatherCondition, timeOfDay, season) // ⭐ Без <T>
    }
}