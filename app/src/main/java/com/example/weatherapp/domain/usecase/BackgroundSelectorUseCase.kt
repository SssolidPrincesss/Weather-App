package com.example.weatherapp.domain.usecase

/*
Этот класс вместе с weatherParser помогают определить состояние погодю(которое, на данный момент,
берется из ЮИ) и присвоить его перечислению WeatherCondition, которое полсле передастся в BackgroundManager
 */
import com.example.weatherapp.domain.model.TimeOfDay
import com.example.weatherapp.domain.model.WeatherCondition
import com.example.weatherapp.utils.TimeUtils

class BackgroundSelectorUseCase(
    private val weatherParser: WeatherParser = WeatherParser
) {

    data class Input(
        val weatherDescription: String
    )

    data class Output(
        val weatherCondition: WeatherCondition,
        val timeOfDay: TimeOfDay
    )

    fun execute(input: Input): Output {
        val weatherCondition = weatherParser.parseWeatherCondition(input.weatherDescription)
        val timeOfDay = TimeUtils.getCurrentTimeOfDay()

        return Output(weatherCondition, timeOfDay)
    }
}