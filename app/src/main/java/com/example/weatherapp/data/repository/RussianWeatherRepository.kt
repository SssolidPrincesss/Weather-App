package com.example.weatherapp.data.repository

/*
этот класс подтягивает данные из погодного апи(яндекс погода)
пока используется только для карты осадков
*/


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class RussianWeatherRepository {

    private val _precipitationMap = MutableLiveData<Bitmap?>()
    val precipitationMap: LiveData<Bitmap?> = _precipitationMap

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    suspend fun loadPrecipitationMap(latitude: Double, longitude: Double) {
        _error.postValue(null)

        try {
            // Выполняем сетевые операции в IO потоке
            val bitmap = withContext(Dispatchers.IO) {
                loadMapFromSources(latitude, longitude)
            }

            if (bitmap != null) {
                _precipitationMap.value = bitmap
            } else {
                _error.postValue("Не удалось загрузить карту осадков")
            }

        } catch (e: Exception) {
            Log.e("RussianWeather", "Error loading map: ${e.message}", e)
            _error.postValue("Ошибка загрузки карты осадков: ${e.message}")
        }
    }

    private fun loadMapFromSources(latitude: Double, longitude: Double): Bitmap? {
        val sources = listOf(
            // Работающие источники карт осадков для России/Европы
            "https://static-maps.yandex.ru/1.x/?ll=${longitude},${latitude}&z=4&l=map&size=400,300&pt=${longitude},${latitude},pm2rdm"



        )

        for (urlString in sources) {
            try {
                Log.d("WeatherRepo", "Trying source: $urlString")
                val bitmap = downloadBitmap(urlString.trim())
                if (bitmap != null) {
                    Log.d("WeatherRepo", "Successfully loaded from: $urlString")
                    return bitmap
                }
            } catch (e: Exception) {
                Log.w("WeatherRepo", "Failed to load from $urlString: ${e.message}")
            }
        }

        return null
    }

    private fun downloadBitmap(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) WeatherApp/1.0")

            if (connection.responseCode == 200) {
                val inputStream: InputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream)
            } else {
                Log.w("WeatherRepo", "HTTP ${connection.responseCode} for $urlString")
                null
            }
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Error downloading $urlString: ${e.message}")
            null
        }
    }
}