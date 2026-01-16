// domain/usecase/WeatherParser.kt
package com.example.weatherapp.domain.usecase

import com.example.weatherapp.domain.model.WeatherCondition

object WeatherParser {
    fun parseWeatherCondition(description: String): WeatherCondition {
        val lowerDesc = description.lowercase()

        return when {
            lowerDesc.contains("ясно") || lowerDesc.contains("солнечно") ||
                    lowerDesc.contains("clear") || lowerDesc.contains("sun") -> WeatherCondition.CLEAR

            lowerDesc.contains("облачно") || lowerDesc.contains("пасмурно") ||
                    lowerDesc.contains("cloud") || lowerDesc.contains("overcast") -> WeatherCondition.CLOUDY

            lowerDesc.contains("дождь") || lowerDesc.contains("ливень") ||
                    lowerDesc.contains("rain") || lowerDesc.contains("drizzle") ||
                    lowerDesc.contains("гроза") || lowerDesc.contains("thunder") ||
                    lowerDesc.contains("storm") -> WeatherCondition.RAIN

            lowerDesc.contains("снег") || lowerDesc.contains("snow") -> WeatherCondition.SNOW

            lowerDesc.contains("туман") || lowerDesc.contains("fog") -> WeatherCondition.FOGGY // ⭐ Туман

            else -> WeatherCondition.CLEAR
        }
    }
}