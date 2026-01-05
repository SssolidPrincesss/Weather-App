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

object GeoUtils {

    fun getCurrentLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Проверяем разрешения
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        // Получаем последнюю известную позицию
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) {
            return lastKnownLocation
        }

        // Если нет последней позиции, возвращаем дефолтные координаты (например, Москва)
        return Location("default").apply {
            latitude = 55.7558
            longitude = 37.6176
        }
    }

    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    }
}