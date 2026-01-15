package com.example.weatherapp.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.weatherapp.R
import com.example.weatherapp.domain.model.DailyForecastItem
import com.example.weatherapp.domain.model.HourlyWeather
import com.example.weatherapp.domain.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.weatherapp.data.model.LocationData

class WeatherRepository {

    private val _weatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> = _weatherData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    // Добавь метод для загрузки с автоматическими параметрами
    suspend fun loadWeatherWithAutoLocation(locationData: LocationData) {
        _error.postValue(null)
        try {
            val data = withContext(Dispatchers.IO) {
                fetchWeatherFromOpenMeteo(
                    lat = locationData.location.latitude,
                    lon = locationData.location.longitude,
                    timezone = locationData.timezone
                )
            }
            if (data != null) {
                _weatherData.postValue(data)
            } else {
                _error.postValue("Не удалось загрузить погоду")
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Load error", e)
            _error.postValue("Ошибка: ${e.message}")
        }
    }

    private fun fetchWeatherFromOpenMeteo(lat: Double, lon: Double, timezone: String): WeatherData? {
        return try {
            // ⭐ Используем переданный часовой пояс
            val url = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon&" +
                    "current_weather=true&" +
                    "wind_speed_unit=ms&" +
                    "hourly=relative_humidity_2m&" +
                    "daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,weathercode&" +
                    "forecast_days=8&" +
                    "timezone=$timezone" // ⭐ Автоматический часовой пояс

            val jsonText = URL(url).readText()

            val jsonObject = JSONObject(jsonText)
            val currentWeather = jsonObject.getJSONObject("current_weather")

            // Температура и ветер
            val temperature = currentWeather.getDouble("temperature")
            val windSpeedMs = currentWeather.getDouble("windspeed")
            val windDirection = currentWeather.getDouble("winddirection")

            // ⭐ Влажность из hourly данных
            val humidity = extractCurrentHumidity(jsonObject)

            // "Ощущается как"
            val feelsLike = calculateFeelsLike(temperature, windSpeedMs)

            //восход зкакат
            val (sunrise, sunset) = extractSunriseSunset(jsonObject)

            //прогноз на 7 дней
            val dailyForecast = extractDailyForecast(jsonObject)


            val tempStr = "${temperature.toInt()}°"
            val feelsLikeStr = "${feelsLike.toInt()}°"
            val windSpeedF = windSpeedMs.toFloat()
            val windDirF = windDirection.toFloat()
            val windDirText = getDirectionTextFromDegrees(windDirF)

            WeatherData(
                temperature = tempStr,
                description = "Облачно",
                feelsLike = feelsLikeStr,
                windSpeed = windSpeedF,
                windDirection = windDirF,
                windDirectionText = windDirText,
                humidity = humidity, // Передаём влажность
                sunrise = sunrise,    // восход
                sunset = sunset, //закат
                dailyForecast = dailyForecast
            )
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Open-Meteo parsing error", e)
            null
        }
    }

    // ⭐ Извлекаем 7-дневный прогноз
    private fun extractDailyForecast(jsonObject: JSONObject): List<DailyForecastItem> {
        return try {
            val daily = jsonObject.getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val tempMax = daily.getJSONArray("temperature_2m_max")
            val tempMin = daily.getJSONArray("temperature_2m_min")
            val weatherCodes = daily.getJSONArray("weathercode")

            val forecastList = mutableListOf<DailyForecastItem>()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            for (i in 0 until dates.length()) {
                if (forecastList.size >= 7) break // ограничиваем 7 днями

                val date = dates.getString(i)
                val maxTemp = tempMax.getDouble(i).toInt()
                val minTemp = tempMin.getDouble(i).toInt()
                val weatherCode = weatherCodes.getInt(i)

                // Определяем день недели
                val dayOfWeek = getDayOfWeek(date)

                // Определяем специальные дни
                val isToday = date == today
                val isTomorrow = isToday && forecastList.isNotEmpty() && forecastList.last().isToday
                val isYesterday = false // Open-Meteo не даёт вчерашние данные, поэтому пропускаем

                // ⭐ Для первого элемента (сегодня) не показываем "Вчера"
                if (forecastList.isEmpty() && !isToday) {
                    // Пропускаем, если первый день не сегодня
                    continue
                }

                forecastList.add(DailyForecastItem(
                    date = date,
                    dayOfWeek = if (isToday) "Сегодня" else if (forecastList.size == 1) "Завтра" else dayOfWeek,
                    tempMin = minTemp,
                    tempMax = maxTemp,
                    weatherCode = weatherCode,
                    isToday = isToday,
                    isTomorrow = forecastList.size == 1 && !isToday
                ))
            }

            // ⭐ Заполняем до 7 дней, если данных меньше
            while (forecastList.size < 7) {
                val last = forecastList.last()
                forecastList.add(DailyForecastItem(
                    date = "",
                    dayOfWeek = getNextDayOfWeek(forecastList.last().dayOfWeek),
                    tempMin = last.tempMin,
                    tempMax = last.tempMax,
                    weatherCode = last.weatherCode
                ))
            }

            forecastList.take(7) // гарантируем ровно 7 дней
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Daily forecast extraction error", e)
            // Возвращаем дефолтные данные
            return listOf(
                DailyForecastItem("Сегодня", "Сегодня", -12, -4, 0, true),
                DailyForecastItem("Завтра", "Завтра", -9, -2, 1, false, true),
                DailyForecastItem("ПН", "ПН", -6, 0, 0, false),
                DailyForecastItem("ВТ", "ВТ", -4, 2, 1, false),
                DailyForecastItem("СР", "СР", -2, 4, 0, false),
                DailyForecastItem("ЧТ", "ЧТ", 0, 6, 1, false),
                DailyForecastItem("ПТ", "ПТ", 2, 8, 0, false)
            )
        }
    }

    // ⭐ Преобразуем дату в день недели
    private fun getDayOfWeek(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            val calendar = Calendar.getInstance()
            calendar.time = date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            when (dayOfWeek) {
                Calendar.MONDAY -> "ПН"
                Calendar.TUESDAY -> "ВТ"
                Calendar.WEDNESDAY -> "СР"
                Calendar.THURSDAY -> "ЧТ"
                Calendar.FRIDAY -> "ПТ"
                Calendar.SATURDAY -> "СБ"
                Calendar.SUNDAY -> "ВС"
                else -> "—"
            }
        } catch (e: Exception) {
            "—"
        }
    }

    // ⭐ Получаем следующий день недели
    private fun getNextDayOfWeek(current: String): String {
        val days = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
        val currentIndex = days.indexOf(current)
        return if (currentIndex != -1 && currentIndex < days.size - 1) {
            days[currentIndex + 1]
        } else {
            "ПН"
        }
    }



    // ⭐ Извлекаем восход и закат
    private fun extractSunriseSunset(jsonObject: JSONObject): Pair<String, String> {
        return try {
            val daily = jsonObject.getJSONObject("daily")
            val sunriseArray = daily.getJSONArray("sunrise")
            val sunsetArray = daily.getJSONArray("sunset")

            // Берем данные на сегодня (первый элемент)
            val sunriseTime = sunriseArray.getString(0) // "2026-01-14T09:34"
            val sunsetTime = sunsetArray.getString(0)   // "2026-01-14T16:17"

            // ⭐ ПРОСТО ИЗВЛЕКАЕМ ВРЕМЯ БЕЗ КОНВЕРТАЦИИ
            // Формат: "2026-01-14T09:34" → "09:34"
            val sunriseFormatted = sunriseTime.substringAfter("T").substring(0, 5)
            val sunsetFormatted = sunsetTime.substringAfter("T").substring(0, 5)

            Pair(sunriseFormatted, sunsetFormatted)
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Sunrise/sunset extraction error", e)
            Pair("09:34", "16:17") // дефолтные значения
        }
    }

    // ⭐ Извлекаем текущую влажность
    private fun extractCurrentHumidity(jsonObject: JSONObject): Int {
        return try {
            val hourly = jsonObject.getJSONObject("hourly")
            val humidityArray = hourly.getJSONArray("relative_humidity_2m")
            val timeArray = hourly.getJSONArray("time")

            val currentTime = System.currentTimeMillis() / 1000
            var minTimeDiff = Long.MAX_VALUE
            var closestHumidity = 50 // дефолтное значение

            for (i in 0 until timeArray.length()) {
                val timeStr = timeArray.getString(i)

                // ✅ Поддержка обоих форматов: с T и без T
                val formattedTime = if (timeStr.contains("T")) {
                    timeStr.replace("T", " ").replace("Z", "")
                } else {
                    timeStr // уже в формате "2026-01-11 00:00"
                }

                // ✅ Парсим без временной зоны (Open-Meteo возвращает локальное время)
                val timeInMillis = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    .parse(formattedTime)?.time ?: continue

                val timeDiff = kotlin.math.abs(timeInMillis / 1000 - currentTime)
                if (timeDiff < minTimeDiff) {
                    minTimeDiff = timeDiff
                    closestHumidity = humidityArray.getDouble(i).toInt()
                }
            }

            closestHumidity
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Humidity extraction error", e)
            return 50
        }
    }

    // ✅ Формула расчёта "ощущается как"
    private fun calculateFeelsLike(temperature: Double, windSpeedMs: Double): Int {
        // Если температура выше 10°C или ветер слабый — ощущается как реальная температура
        if (temperature > 10 || windSpeedMs < 1.3) {
            return temperature.toInt()
        }

        // Конвертируем скорость ветра в км/ч
        val windSpeedKmh = windSpeedMs * 3.6

        // Формула Wind Chill
        val feelsLike = 13.12 +
                (0.6215 * temperature) -
                (11.37 * Math.pow(windSpeedKmh, 0.16)) +
                (0.3965 * temperature * Math.pow(windSpeedKmh, 0.16))

        return feelsLike.toInt()
    }

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

    //виджет-почасовка

    suspend fun loadHourlyForecastWithAutoLocation(locationData: LocationData): List<HourlyWeather> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=${locationData.location.latitude}&" +
                        "longitude=${locationData.location.longitude}&" +
                        "hourly=temperature_2m,relative_humidity_2m,weathercode,windspeed_10m&" +
                        "forecast_days=3&" +
                        "timezone=${locationData.timezone}"

                val jsonText = URL(url).readText()
                val jsonObject = JSONObject(jsonText)
                val hourly = jsonObject.getJSONObject("hourly")

                val timeArray = hourly.getJSONArray("time")
                val tempArray = hourly.getJSONArray("temperature_2m")
                val humidityArray = hourly.getJSONArray("relative_humidity_2m")
                val weatherCodeArray = hourly.getJSONArray("weathercode")

                // Получаем текущее время в формате, который использует Open-Meteo
                val now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Yekaterinburg"))
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
                val currentTime = formatter.format(now.time) // Формат: "2026-01-13T18:00"

// Ищем индекс текущего часа в массиве
                var startIndex = -1
                for (i in 0 until timeArray.length()) {
                    val timeStr = timeArray.getString(i) // Формат: "2026-01-13T18:00"

                    // ✅ ПРАВИЛЬНОЕ СРАВНЕНИЕ
                    if (timeStr.startsWith(currentTime.substring(0, 13))) { // Берём первые 13 символов: "2026-01-13T18"
                        startIndex = i
                        break
                    }
                }

                // Если не нашли текущий час — берём с начала
                if (startIndex == -1) startIndex = 0

                // Формируем 24 часа начиная с текущего
                val hourlyData = mutableListOf<HourlyWeather>()
                for (i in startIndex until minOf(startIndex + 24, timeArray.length())) {
                    val timeStr = timeArray.getString(i)
                    val hour = parseHourFromTimestamp(timeStr)
                    val temperature = tempArray.getDouble(i)
                    val humidity = humidityArray.getDouble(i).toInt()
                    val weatherCode = weatherCodeArray.getInt(i)
                    val weatherIcon = getWeatherIcon(weatherCode, hour)

                    hourlyData.add(
                        HourlyWeather(
                            hour = hour,
                            weatherIcon = weatherIcon,
                            temperature = temperature,
                            humidity = humidity
                        )
                    )
                }

                // Если данных меньше 24 — дополняем моками
                while (hourlyData.size < 24) {
                    val last = hourlyData.lastOrNull()
                    hourlyData.add(
                        HourlyWeather(
                            hour = getNextHour(hourlyData.last().hour),
                            weatherIcon = last?.weatherIcon ?: R.drawable.ic_cloudy_day,
                            temperature = last?.temperature ?: -10.0,
                            humidity = last?.humidity ?: 80
                        )
                    )
                }

                hourlyData.take(24)
            } catch (e: Exception) {
                Log.e("WeatherRepo", "Hourly forecast error", e)
                // ⭐ ИСПРАВЛЕНИЕ 5: Правильное имя метода (было getMockHourly0Data)
                getMockHourlyData().take(24)
            }
        }
    }

    // Улучшенный метод определения иконки с учётом ночи/дня
    private fun getWeatherIcon(weatherCode: Int, hourStr: String): Int {
        // Определяем, день или ночь (6:00–20:00 = день)
        val hour = hourStr.split(":")[0].toIntOrNull() ?: 12
        val isDay = hour in 6..19

        return when (weatherCode) {
            0 -> if (isDay) R.drawable.ic_sunny_day else R.drawable.ic_clear_night
            1, 2, 3 -> if (isDay) R.drawable.ic_cloudy_day else R.drawable.ic_cloudy_night
            45, 48 -> R.drawable.ic_foggy
            51, 53, 55, 56, 57 -> R.drawable.ic_rainy
            61, 63, 65, 66, 67 -> R.drawable.ic_rainy
            71, 73, 75, 77 -> R.drawable.ic_snowy
            80, 81, 82 -> R.drawable.ic_rainy
            95, 96, 99 -> R.drawable.ic_rainy
            else -> if (isDay) R.drawable.ic_cloudy_day else R.drawable.ic_cloudy_night
        }
    }
    // Вспомогательные методы
    private fun parseHourFromTimestamp(timestamp: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            val date = formatter.parse(timestamp)
            val hourFormatter = SimpleDateFormat("HH:mm", Locale.US)
            hourFormatter.format(date)
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Error parsing timestamp: $timestamp", e)
            "00:00"
        }
    }

    // Вспомогательный метод для получения следующего часа
    private fun getNextHour(current: String): String {
        val parts = current.split(":")
        var hour = parts[0].toInt()
        hour = (hour + 1) % 24
        return String.format("%02d:00", hour)
    }

    fun getMockHourlyData(): List<HourlyWeather> {
        return listOf(
            HourlyWeather("21:00", R.drawable.ic_cloudy_night, -28.0, 3),
            HourlyWeather("22:00", R.drawable.ic_clear_night, -15.0, 14),
            HourlyWeather("23:00", R.drawable.ic_clear_night, -28.0, 5),
            HourlyWeather("00:00", R.drawable.ic_rainy, -18.0, 24),
            HourlyWeather("01:00", R.drawable.ic_rainy, -10.0, 17),
            HourlyWeather("02:00", R.drawable.ic_sunny_day, -23.0, 31),
            HourlyWeather("03:00", R.drawable.ic_sunny_day, -30.0, 24),
            HourlyWeather("04:00", R.drawable.ic_cloudy_day, -12.0, 28),
            HourlyWeather("05:00", R.drawable.ic_sunny_day, 5.0, 19),
            HourlyWeather("06:00", R.drawable.ic_snowy, -18.0, 24),
            HourlyWeather("07:00", R.drawable.ic_cloudy_day, -10.0, 17),
            HourlyWeather("08:00", R.drawable.ic_sunny_day, -23.0, 31),
            HourlyWeather("09:00", R.drawable.ic_sunny_day, -30.0, 24),
            HourlyWeather("10:00", R.drawable.ic_sunny_day, -25.0, 20),
            HourlyWeather("11:00", R.drawable.ic_sunny_day, -20.0, 18),
            HourlyWeather("12:00", R.drawable.ic_sunny_day, -18.0, 15),
            HourlyWeather("13:00", R.drawable.ic_sunny_day, -15.0, 14),
            HourlyWeather("14:00", R.drawable.ic_sunny_day, -12.0, 12),
            HourlyWeather("15:00", R.drawable.ic_sunny_day, -10.0, 10),
            HourlyWeather("16:00", R.drawable.ic_cloudy_day, -8.0, 15),
            HourlyWeather("17:00", R.drawable.ic_cloudy_day, -10.0, 18),
            HourlyWeather("18:00", R.drawable.ic_cloudy_night, -12.0, 20),
            HourlyWeather("19:00", R.drawable.ic_cloudy_night, -15.0, 22),
            HourlyWeather("20:00", R.drawable.ic_cloudy_night, -18.0, 25)
        )
    }

}