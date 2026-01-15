package com.example.weatherapp.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.weatherapp.utils.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.weatherapp.data.model.LocationData
import android.location.Geocoder
import android.util.Log
import com.example.weatherapp.utils.LocationUtils
import java.util.Locale


class LocationManager(private val context: Context) {

    suspend fun getCurrentLocationAndTimeZone(): LocationData {
        return withContext(Dispatchers.IO) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@withContext LocationData(
                        location = GeoUtils.createDefaultLocation(),
                        timezone = "Asia/Yekaterinburg",
                        locationName = "Верхняя Пышма"
                    )
                }

                val location = GeoUtils.getCurrentLocation(context) ?: GeoUtils.createDefaultLocation()
                val timezone = GeoUtils.getTimeZoneByCoordinates(location.latitude, location.longitude)

                // ⭐ ИСПОЛЬЗУЕМ УТИЛИТУ:
                val locationName = LocationUtils.getCityName(context, location.latitude, location.longitude)

                LocationData(location, timezone, locationName)

            } catch (e: Exception) {
                LocationData(
                    location = GeoUtils.createDefaultLocation(),
                    timezone = "Asia/Yekaterinburg",
                    locationName = "Верхняя Пышма"
                )
            }
        }
    }
}

