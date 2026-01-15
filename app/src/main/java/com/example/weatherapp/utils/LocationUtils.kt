// utils/LocationUtils.kt
package com.example.weatherapp.utils

import android.content.Context
import android.location.Geocoder
import android.util.Log
import java.util.Locale

object LocationUtils {

    fun getCityName(context: Context, latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("ru", "RU"))
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality
                val adminArea = address.adminArea

                if (!city.isNullOrEmpty()) {
                    city
                } else if (!adminArea.isNullOrEmpty()) {
                    adminArea
                } else {
                    "Россия"
                }
            } else {
                getCityByCoordinates(latitude, longitude)
            }
        } catch (e: Exception) {
            Log.e("LocationUtils", "Geocoder error", e)
            getCityByCoordinates(latitude, longitude)
        }
    }

    private fun getCityByCoordinates(latitude: Double, longitude: Double): String {
        return when {
            // Москва и область
            latitude in 55.5..56.0 && longitude in 37.0..38.0 -> "Москва"

            // Санкт-Петербург и область
            latitude in 59.7..60.1 && longitude in 30.0..31.0 -> "Санкт-Петербург"

            // Екатеринбург и Свердловская область
            latitude in 56.5..57.5 && longitude in 60.0..61.0 -> "Екатеринбург"
            latitude in 56.8..57.0 && longitude in 60.4..60.7 -> "Верхняя Пышма"

            // Новосибирск
            latitude in 54.8..55.2 && longitude in 82.5..83.5 -> "Новосибирск"

            // Казань
            latitude in 55.6..56.0 && longitude in 48.9..49.4 -> "Казань"

            // Челябинск
            latitude in 54.9..55.3 && longitude in 61.0..61.8 -> "Челябинск"

            // Омск
            latitude in 54.7..55.2 && longitude in 73.0..73.8 -> "Омск"

            // Самара
            latitude in 53.0..53.5 && longitude in 50.0..50.5 -> "Самара"

            // Ростов-на-Дону
            latitude in 47.0..47.5 && longitude in 39.5..40.5 -> "Ростов-на-Дону"

            // Уфа
            latitude in 54.5..54.9 && longitude in 55.7..56.2 -> "Уфа"

            // Красноярск
            latitude in 55.8..56.2 && longitude in 92.5..93.5 -> "Красноярск"

            // Воронеж
            latitude in 51.5..51.9 && longitude in 39.0..39.5 -> "Воронеж"

            // Пермь
            latitude in 57.8..58.2 && longitude in 55.8..56.5 -> "Пермь"

            else -> "Россия"
        }
    }
}