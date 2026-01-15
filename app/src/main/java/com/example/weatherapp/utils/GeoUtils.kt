package com.example.weatherapp.utils

/*
нужен для оптимизации нахождения геопозиции пользователя
 */

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.TimeZone

object GeoUtils {

    // ⭐ Новые методы для автоматического определения
    fun getCurrentLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        // Попробуем GPS
        val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (gpsLocation != null && isLocationValid(gpsLocation)) {
            return gpsLocation
        }

        // Попробуем сеть
        val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (networkLocation != null && isLocationValid(networkLocation)) {
            return networkLocation
        }

        // Если ничего не получилось — возвращаем дефолтные координаты Верхней Пышмы
        return createDefaultLocation()
    }

    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    }

    // ⭐ Определяем часовой пояс по координатам
    fun getTimeZoneByCoordinates(latitude: Double, longitude: Double): String {
        return when {
            longitude < 30.0 -> "Europe/Kaliningrad"      // UTC+2
            longitude < 45.0 -> "Europe/Moscow"           // UTC+3
            longitude < 60.0 -> "Asia/Yekaterinburg"      // UTC+5
            longitude < 75.0 -> "Asia/Yekaterinburg"      // ⭐ ИСПРАВЛЕНО: Екатеринбург до 75°
            longitude < 90.0 -> "Asia/Krasnoyarsk"        // UTC+7
            longitude < 105.0 -> "Asia/Irkutsk"           // UTC+8
            longitude < 120.0 -> "Asia/Yakutsk"           // UTC+9
            longitude < 135.0 -> "Asia/Vladivostok"       // UTC+10
            else -> "Asia/Kamchatka"                      // UTC+12
        }
    }

    // ⭐ Вспомогательные методы
    private fun isLocationValid(location: Location): Boolean {
        val currentTime = System.currentTimeMillis()
        val locationTime = location.time
        val timeDiff = currentTime - locationTime

        // Считаем локацию валидной, если она не старше 1 часа
        return timeDiff < 3600000 && location.accuracy < 1000f
    }

    fun createDefaultLocation(): Location {
        return Location("default").apply {
            latitude = 56.9474   // Верхняя Пышма
            longitude = 60.5707
        }
    }

    // ⭐ Получаем текущий часовой пояс устройства
    fun getDeviceTimeZone(): String {
        return TimeZone.getDefault().id
    }
}