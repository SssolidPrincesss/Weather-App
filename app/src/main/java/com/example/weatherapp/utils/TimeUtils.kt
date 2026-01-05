package com.example.weatherapp.utils

/*
класс нужен для правильной смены фона в зависимости от времени суток
 */

import java.util.Calendar
import com.example.weatherapp.domain.model.TimeOfDay

object TimeUtils {

    fun getCurrentTimeOfDay(): TimeOfDay {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        return if (currentHour in 6..18) TimeOfDay.DAY else TimeOfDay.NIGHT
    }
}