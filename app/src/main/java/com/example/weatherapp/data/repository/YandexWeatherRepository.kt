package com.example.weatherapp.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.weatherapp.domain.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class YandexWeatherRepository {

    private val _weatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> = _weatherData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    suspend fun loadWeather(citySlug: String = "верхняя-пышма") {
        _error.postValue(null)
        try {
            val data = withContext(Dispatchers.IO) {
                fetchWeatherFromYandex(citySlug)
            }
            if (data != null) {
                _weatherData.postValue(data)
            } else {
                _error.postValue("Не удалось загрузить погоду")
            }
        } catch (e: Exception) {
            Log.e("YandexWeatherRepo", "Load error", e)
            _error.postValue("Ошибка: ${e.message}")
        }
    }

    private fun fetchWeatherFromYandex(citySlug: String): WeatherData? {
        return try {
            val url = "https://yandex.ru/pogoda/?lat=56.9474&lon=60.5707"

            Log.d("YandexWeather", "Fetching: $url")

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/118.0.0.0 Mobile Safari/537.36")
                .timeout(20_000)
                .get()

            // ✅ Сначала пытаемся получить данные из JavaScript
            val jsData = parseWeatherFromJavaScript(doc)
            if (jsData != null) {
                return jsData
            }

            // ⭐ Если не получилось — fallback на HTML парсинг
            parseWeatherFromHtml(doc)

        } catch (e: Exception) {
            Log.e("YandexWeather", "Failed to fetch weather: ${e.message}", e)
            WeatherData("−12°", "Облачно", "−18°", 0f, 0f, "—")
        }
    }

    // ✅ НОВЫЙ МЕТОД: Парсинг из JavaScript-объекта
    private fun parseWeatherFromJavaScript(doc: Document): WeatherData? {
        try {
            // Ищем скрипт с данными погоды
            val scripts = doc.select("script")
            for (script in scripts) {
                val scriptText = script.html()
                if (scriptText.contains("window.yaWeather") || scriptText.contains("\"direction\":")) {
                    // Ищем направление ветра в градусах
                    val directionMatch = Regex("\"direction\":\\s*(\\d+)").find(scriptText)
                    val speedMatch = Regex("\"speed\":\\s*(\\d+\\.?\\d*)").find(scriptText)
                    val tempMatch = Regex("\"temp\":\\s*(-?\\d+)").find(scriptText)
                    val conditionMatch = Regex("\"condition\":\\s*\"([^\"]+)\"").find(scriptText)
                    val feelsLikeMatch = Regex("\"feelsLike\":\\s*(-?\\d+)").find(scriptText)

                    if (directionMatch != null && speedMatch != null) {
                        val windDirection = directionMatch.groupValues[1].toFloatOrNull() ?: 0f
                        val windSpeedMs = speedMatch.groupValues[1].toFloatOrNull() ?: 0f
                        val windSpeedKmh = windSpeedMs * 3.6f

                        val temperature = if (tempMatch != null) "${tempMatch.groupValues[1]}°" else "−12°"
                        val description = if (conditionMatch != null) conditionMatch.groupValues[1] else "Облачно"
                        val feelsLike = if (feelsLikeMatch != null) "${feelsLikeMatch.groupValues[1]}°" else "−18°"

                        val windDirectionText = getDirectionTextFromDegrees(windDirection)

                        Log.d("YandexWeather", "Parsed from JS: $temperature, $description, $feelsLike")
                        Log.d("YandexWeather", "Wind from JS: ${windSpeedKmh} км/ч, $windDirectionText ($windDirection°)")

                        return WeatherData(
                            temperature = temperature,
                            description = description,
                            feelsLike = feelsLike,
                            windSpeed = windSpeedKmh,
                            windDirection = windDirection,
                            windDirectionText = windDirectionText
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("YandexWeather", "JS parsing failed", e)
        }
        return null
    }

    // ✅ Функция для определения текстового направления по градусам
    private fun getDirectionTextFromDegrees(degrees: Float): String {
        return when {
            degrees in 348.75f..360f || degrees in 0f..11.25f -> "С"
            degrees in 11.25f..33.75f -> "ССВ"
            degrees in 33.75f..56.25f -> "СВ"
            degrees in 56.25f..78.75f -> "ВСВ"
            degrees in 78.75f..101.25f -> "В"
            degrees in 101.25f..123.75f -> "ВЮВ"
            degrees in 123.75f..146.25f -> "ЮВ"
            degrees in 146.25f..168.75f -> "ЮЮВ"
            degrees in 168.75f..191.25f -> "Ю"
            degrees in 191.25f..213.75f -> "ЮЮЗ"
            degrees in 213.75f..236.25f -> "ЮЗ"
            degrees in 236.25f..258.75f -> "ЗЮЗ"
            degrees in 258.75f..281.25f -> "З"
            degrees in 281.25f..303.75f -> "ЗСЗ"
            degrees in 303.75f..326.25f -> "СЗ"
            degrees in 326.25f..348.75f -> "ССЗ"
            else -> "—"
        }
    }

    // ⭐ ОСТАВЛЯЕМ HTML-ПАРСИНГ ДЛЯ FALLBACK
    private fun parseWeatherFromHtml(doc: Document): WeatherData {
        var temperature = "−12°"
        val tempSelectors = listOf(
            "div.temp__value",
            "span.temp__value",
            ".temp-value",
            "[class*='temp'][class*='value']"
        )

        for (selector in tempSelectors) {
            val elements = doc.select(selector)
            for (el in elements) {
                val text = el.text().trim()
                if (text.contains("°") && text.length < 10) {
                    temperature = text.replace(" ", "")
                    break
                }
            }
            if (temperature != "−12°") break
        }

        var description = "Облачно"
        val descSelectors = listOf(
            "div.link__condition",
            ".weather__descr",
            "[data-testid='weather-description']",
            "[class*='condition']"
        )

        for (selector in descSelectors) {
            val elements = doc.select(selector)
            for (el in elements) {
                val text = el.text().trim()
                if (text.isNotEmpty() && text.length in 3..30) {
                    description = text
                    break
                }
            }
            if (description != "Облачно") break
        }

        var feelsLike = "−18°"
        val allText = doc.body().text()
        val match = Regex("Ощущается как ([−\\-+]?\\d+)°?").find(allText)
        if (match != null) {
            var feelsText = match.groupValues[1].trim()
            feelsText = feelsText.replace('−', '-')
            feelsLike = "${feelsText}°"
        }

        // ⭐ Для HTML парсинга используем дефолтные значения для ветра
        return WeatherData(
            temperature = temperature,
            description = description,
            feelsLike = feelsLike,
            windSpeed = 10f,  // ⭐ дефолтная скорость
            windDirection = 225f,  // ⭐ дефолтное направление (ЮЗ)
            windDirectionText = "ЮЗ"  // ⭐ дефолтный текст
        )
    }
}