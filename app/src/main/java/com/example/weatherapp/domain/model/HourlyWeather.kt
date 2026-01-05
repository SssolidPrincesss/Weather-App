package com.example.weatherapp.domain.model
//Модель данных для плашки с погодой по часам
data class HourlyWeather (
    val hour: String,
    val weatherIcon: Int,
    val temperature: Double,
    val humidity: Int
)