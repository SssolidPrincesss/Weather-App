package com.example.weatherapp.utils

import com.example.weatherapp.domain.model.Season
import com.example.weatherapp.domain.model.TimeOfDay
import com.example.weatherapp.domain.model.WeatherCondition

data class Output(
    val weatherCondition: WeatherCondition,
    val timeOfDay: TimeOfDay,
    val season: Season
)