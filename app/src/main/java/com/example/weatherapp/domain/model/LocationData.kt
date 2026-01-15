package com.example.weatherapp.data.model

import android.location.Location

data class LocationData(
    val location: Location,
    val timezone: String,
    val locationName: String
)