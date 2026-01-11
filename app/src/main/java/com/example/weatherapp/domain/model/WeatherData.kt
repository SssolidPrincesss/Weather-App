package com.example.weatherapp.domain.model

data class WeatherData(
    val temperature: String,      // например: "−12°"
    val description: String,      // например: "Дождь"
    val feelsLike: String,         // например: "−18°"
    val windSpeed: Float = 0f,      // новое
    val windDirection: Float = 0f,   // ← новое (в градусах)
    val windDirectionText: String = "—", // ← новое (например: "СВ")
    val humidity: Int, //влажность
    val sunrise: String, // Восход
    val sunset: String,   // Закат
    val dailyForecast: List<DailyForecastItem> //Прогноз на 7 дней
)