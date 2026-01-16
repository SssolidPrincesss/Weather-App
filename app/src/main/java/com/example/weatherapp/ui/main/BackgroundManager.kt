// utils/BackgroundManager.kt
package com.example.weatherapp.utils

import android.content.Context
import com.example.weatherapp.R
import com.example.weatherapp.domain.model.Season
import com.example.weatherapp.domain.model.TimeOfDay
import com.example.weatherapp.domain.model.WeatherCondition

object BackgroundManager {

    fun getBackgroundResource(
        context: Context,
        weatherCondition: WeatherCondition,
        timeOfDay: TimeOfDay,
        season: Season
    ): Int {
        return when {
            // Зима
            season == Season.WINTER -> when {
                weatherCondition == WeatherCondition.CLEAR && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_clear_winter_day
                weatherCondition == WeatherCondition.CLEAR && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_clear_winter_night
                weatherCondition == WeatherCondition.CLOUDY && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_cloudy_winter_day
                weatherCondition == WeatherCondition.CLOUDY && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_cloudy_winter_night
                weatherCondition == WeatherCondition.SNOW && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_snowy_winter_day
                weatherCondition == WeatherCondition.SNOW && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_snowy_winter_night
                weatherCondition == WeatherCondition.FOGGY && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_foggy_winter_day
                weatherCondition == WeatherCondition.FOGGY && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_foggy_winter_night
                else -> R.drawable.bg_clear_winter_day
            }

            // Осень
            season == Season.AUTUMN -> when {
                weatherCondition == WeatherCondition.CLEAR && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_clear_autumn_day
                weatherCondition == WeatherCondition.CLEAR && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_clear_autumn_night
                weatherCondition == WeatherCondition.CLOUDY && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_cloudy_autumn_day
                weatherCondition == WeatherCondition.CLOUDY && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_cloudy_autumn_night
                weatherCondition == WeatherCondition.RAIN && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_rainy_autumn_day
                weatherCondition == WeatherCondition.RAIN && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_rainy_autumn_night
                weatherCondition == WeatherCondition.SNOW && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_snowy_autumn_day
                weatherCondition == WeatherCondition.SNOW && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_snowy_autumn_night
                weatherCondition == WeatherCondition.FOGGY && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_foggy_autumn_day
                weatherCondition == WeatherCondition.FOGGY && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_foggy_autumn_night
                else -> R.drawable.bg_clear_autumn_day
            }

            // Лето/весна
            season == Season.SPRING || season == Season.SUMMER -> when {
                weatherCondition == WeatherCondition.CLEAR && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_clear_summer_spring_day
                weatherCondition == WeatherCondition.CLEAR && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_clear_summer_spring_night
                weatherCondition == WeatherCondition.CLOUDY && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_cloudy_summer_spring_day
                weatherCondition == WeatherCondition.CLOUDY && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_cloudy_summer_spring_night
                weatherCondition == WeatherCondition.RAIN && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_rainy_summer_spring_day
                weatherCondition == WeatherCondition.RAIN && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_rainy_summer_spring_night
                weatherCondition == WeatherCondition.SNOW && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_snowy_spring_day
                weatherCondition == WeatherCondition.SNOW && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_snowy_spring_night
                weatherCondition == WeatherCondition.FOGGY && timeOfDay == TimeOfDay.DAY ->
                    R.drawable.bg_foggy_summer_spring_day
                weatherCondition == WeatherCondition.FOGGY && timeOfDay == TimeOfDay.NIGHT ->
                    R.drawable.bg_foggy_summer_spring_night
                else -> R.drawable.bg_clear_summer_spring_day
            }

            else -> R.drawable.bg_clear_winter_day
        }
    }
}