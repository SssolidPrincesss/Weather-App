package com.example.weatherapp.utils
/*
Этот класс нужен для подбора фона по типу погоды и времени суток
Он берет состояние погоды из перечисления WeatherCondition
 */
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.weatherapp.R
import com.example.weatherapp.domain.model.TimeOfDay
import com.example.weatherapp.domain.model.WeatherCondition

class BackgroundResourceProvider(private val context: Context) {

    fun getBackgroundDrawable(
        weatherCondition: WeatherCondition,
        timeOfDay: TimeOfDay
    ): Drawable? {
        val resourceId = getBackgroundResourceId(weatherCondition, timeOfDay)
        return ContextCompat.getDrawable(context, resourceId)
    }

    fun getBackgroundResourceId(
        weatherCondition: WeatherCondition,
        timeOfDay: TimeOfDay
    ): Int = when (weatherCondition) {
        WeatherCondition.CLEAR -> when (timeOfDay) {
            TimeOfDay.DAY -> R.drawable.bg_clear_day
            TimeOfDay.NIGHT -> R.drawable.bg_clear_night
        }

        WeatherCondition.CLOUDY -> when (timeOfDay) {
            TimeOfDay.DAY -> R.drawable.bg_cloudy_day
            TimeOfDay.NIGHT -> R.drawable.bg_cloudy_night
        }

        WeatherCondition.RAIN -> when (timeOfDay) {
            TimeOfDay.DAY -> R.drawable.bg_rain_day
            TimeOfDay.NIGHT -> R.drawable.bg_rain_night
        }

        WeatherCondition.SNOW -> when (timeOfDay) {
            TimeOfDay.DAY -> R.drawable.bg_snow_day
            TimeOfDay.NIGHT -> R.drawable.bg_snow_night
        }
    }
}