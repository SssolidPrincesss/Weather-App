package com.example.weatherapp.ui.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.data.model.LocationData
import com.example.weatherapp.data.repository.RussianWeatherRepository
import com.example.weatherapp.data.repository.WeatherRepository
import com.example.weatherapp.domain.model.DailyForecastItem
import com.example.weatherapp.domain.model.HourlyWeather
import com.example.weatherapp.domain.model.TimeOfDay
import com.example.weatherapp.domain.model.WeatherCondition
import com.example.weatherapp.utils.GeoUtils
import com.example.weatherapp.utils.TimeUtils
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.utils.LocationUtils
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var weatherRepo: WeatherRepository
    private lateinit var russianWeatherRepo: RussianWeatherRepository

    // === Погода ===
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeatherDesc: TextView
    private lateinit var tvFeelsLike: TextView

    // === Карта осадков ===
    private lateinit var ivPrecipitationMap: ImageView
    private lateinit var pbPrecipitationLoading: ProgressBar
    private lateinit var tvPrecipitationError: TextView
    private lateinit var flPrecipitationContainer: FrameLayout

    // === Основные компоненты ===
    private lateinit var adapter: HourlyForecastAdapter
    private lateinit var ivBackground: ImageView

    private var timeUpdateHandler: Handler? = null
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateBackground()
            timeUpdateHandler?.postDelayed(this, 60_000)
        }
    }

    //ветер
    private lateinit var ivWindDirection: ImageView
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvWindUnit: TextView
    private lateinit var tvWindDescription: TextView
    private lateinit var tvWindDirectionText: TextView

    //Влажность
    private lateinit var pbHumidity: ProgressBar
    private lateinit var tvHumidityValue: TextView

    //Восход / закат
    private lateinit var tvSunrise: TextView
    private lateinit var tvSunset: TextView

    //прогноз на 7 дней
    private lateinit var forecastViews: List<Triple<TextView, ImageView, TextView>>

    private val timeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_DATE_CHANGED -> {
                    updateBackground()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация репозиториев
        weatherRepo = WeatherRepository()
        russianWeatherRepo = RussianWeatherRepository()

        // Инициализация UI
        initViews()
        initWeatherViews()
        initWindViews()
        initPrecipitationViews()
        initForecastViews()
        setupRecyclerView()

        // Загрузка данных с автоматическим определением местоположения
        loadWeatherWithAutoLocation()
    }

    // Используй в нужном месте:
    private fun loadWeatherWithAutoLocation() {
        lifecycleScope.launch {
            try {
                val location = getCurrentLocation()
                val lat = location?.latitude ?: 56.9474
                val lon = location?.longitude ?: 60.5707

                val timezone = GeoUtils.getTimeZoneByCoordinates(lat, lon)
                // ⭐ ИСПОЛЬЗУЕМ УТИЛИТУ:
                val locationName = LocationUtils.getCityName(this as Context, lat, lon)

                val locationData = LocationData(location ?: createDefaultLocation(), timezone, locationName)
                findViewById<TextView>(R.id.tvLocationName).text = locationName

                // Обновляем название локации
                findViewById<TextView>(R.id.tvLocationName).text = locationName

                // Загружаем все данные
                weatherRepo.loadWeatherWithAutoLocation(locationData)
                loadHourlyForecast(locationData)
                loadPrecipitationMap(lat, lon)

            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading weather with auto location", e)
                // Fallback на Верхнюю Пышму
                loadFallbackWeather()
            }
        }
    }

    private suspend fun loadFallbackWeather() {
        val defaultLocation = createDefaultLocation()
        val locationData = LocationData(defaultLocation, "Asia/Yekaterinburg", "Верхняя Пышма")

        weatherRepo.loadWeatherWithAutoLocation(locationData)
        loadHourlyForecast(locationData)
        loadPrecipitationMap(56.9474, 60.5707)
        findViewById<TextView>(R.id.tvLocationName).text = "Верхняя Пышма"
    }

    private fun createDefaultLocation(): Location {
        return Location("default").apply {
            latitude = 56.9474
            longitude = 60.5707
        }
    }



    private fun initViews() {
        ivBackground = findViewById(R.id.ivBackground)
        pbHumidity = findViewById(R.id.pbHumidity)
        tvHumidityValue = findViewById(R.id.tvHumidityValue)
        tvSunrise = findViewById(R.id.tvSunrise)
        tvSunset = findViewById(R.id.tvSunset)
    }

    private fun initForecastViews() {
        forecastViews = listOf(
            Triple(findViewById(R.id.tvDay1), findViewById(R.id.ivWeather1), findViewById(R.id.tvTemp1)),
            Triple(findViewById(R.id.tvDay2), findViewById(R.id.ivWeather2), findViewById(R.id.tvTemp2)),
            Triple(findViewById(R.id.tvDay3), findViewById(R.id.ivWeather3), findViewById(R.id.tvTemp3)),
            Triple(findViewById(R.id.tvDay4), findViewById(R.id.ivWeather4), findViewById(R.id.tvTemp4)),
            Triple(findViewById(R.id.tvDay5), findViewById(R.id.ivWeather5), findViewById(R.id.tvTemp5)),
            Triple(findViewById(R.id.tvDay6), findViewById(R.id.ivWeather6), findViewById(R.id.tvTemp6)),
            Triple(findViewById(R.id.tvDay7), findViewById(R.id.ivWeather7), findViewById(R.id.tvTemp7))
        )
    }

    private fun initWindViews() {
        ivWindDirection = findViewById(R.id.ivWindDirection)
        tvWindSpeed = findViewById(R.id.tvWindSpeed)
        tvWindUnit = findViewById(R.id.tvWindUnit)
        tvWindDescription = findViewById(R.id.tvWindDescription)
        tvWindDirectionText = findViewById(R.id.tvWindDirectionText)
    }

    private fun getWindDescription(speed: Float): String {
        return when {
            speed < 0.3f -> "Штиль"
            speed < 1.6f -> "Тихий"
            speed < 3.4f -> "Лёгкий"
            speed < 5.5f -> "Слабый"
            speed < 8.0f -> "Умеренный"
            speed < 10.8f -> "Свежий"
            speed < 13.9f -> "Сильный"
            speed < 17.2f -> "Крепкий"
            speed < 20.8f -> "Очень крепкий"
            speed < 24.5f -> "Шторм"
            speed < 28.5f -> "Сильный шторм"
            else -> "Ураган"
        }
    }

    private fun initWeatherViews() {
        tvTemperature = findViewById(R.id.tvTemperature)
        tvWeatherDesc = findViewById(R.id.tvWeatherDesc)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)
    }

    private fun setDefaultWeatherValues() {
        tvTemperature.text = "−12°"
        tvWeatherDesc.text = "Облачно"
        tvFeelsLike.text = "Ощущается как −18°"
        tvSunrise.text = "09:30"
        tvSunset.text = "16:30"
        setDefaultWindValues()
        setDefaultHumidityValues()
    }

    private fun setDefaultWindValues() {
        tvWindSpeed.text = "2.3"
        tvWindUnit.text = "м/с"
        tvWindDescription.text = "Слабый"
        tvWindDirectionText.text = "ЮЗ"
        ivWindDirection.rotation = 225f
    }

    private fun setDefaultHumidityValues() {
        pbHumidity.progress = 80
        tvHumidityValue.text = "80%"
    }

    // === НАБЛЮДЕНИЕ ЗА ДАННЫМИ ===
    private fun observeWeatherData() {
        weatherRepo.weatherData.observe(this) { data ->
            Log.d("Weather", "Weather data received: $data")
            updateWeatherUI(data)
        }

        weatherRepo.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Log.e("Weather", "Error: $error")
                setDefaultWeatherValues()
            }
        }
    }

    private fun updateWeatherUI(data: com.example.weatherapp.domain.model.WeatherData) {
        tvTemperature.text = data.temperature
        tvWeatherDesc.text = data.description
        tvFeelsLike.text = "Ощущается как ${data.feelsLike}"
        tvSunrise.text = data.sunrise
        tvSunset.text = data.sunset

        updateHumidityUI(data.humidity)
        updateWindUI(data)
        updateForecastUI(data.dailyForecast)
        updateBackground()
    }

    private fun updateWindUI(data: com.example.weatherapp.domain.model.WeatherData) {
        tvWindSpeed.text = String.format("%.1f", data.windSpeed)
        tvWindUnit.text = "м/с"
        tvWindDescription.text = getWindDescription(data.windSpeed)
        tvWindDirectionText.text = data.windDirectionText

        // Правильный поворот стрелки (0° = север → 270° в Android)
        val rotation = (data.windDirection + 270f) % 360f
        ivWindDirection.rotation = rotation
    }

    private fun updateForecastUI(forecast: List<DailyForecastItem>) {
        forecastViews.forEachIndexed { index, (dayView, weatherView, tempView) ->
            if (index < forecast.size) {
                val item = forecast[index]
                dayView.text = item.dayOfWeek
                tempView.text = "${item.tempMax}° / ${item.tempMin}°"
                weatherView.setImageResource(getWeatherIcon(item.weatherCode))
            }
        }
    }

    private fun getWeatherIcon(weatherCode: Int): Int {
        return when {
            weatherCode == 0 -> R.drawable.ic_sunny_day
            weatherCode in 1..3 -> R.drawable.ic_cloudy_day
            weatherCode in 45..48 -> R.drawable.ic_foggy
            weatherCode in 51..57 -> R.drawable.ic_rainy
            weatherCode in 61..67 -> R.drawable.ic_rainy
            weatherCode in 71..77 -> R.drawable.ic_snowy
            weatherCode in 80..82 -> R.drawable.ic_rainy
            else -> R.drawable.ic_cloudy_day
        }
    }

    private fun updateHumidityUI(humidity: Int) {
        pbHumidity.progress = humidity
        tvHumidityValue.text = "${humidity}%"
    }

    // === КАРТА ОСАДКОВ ===
    private fun initPrecipitationViews() {
        try {
            ivPrecipitationMap = findViewById(R.id.ivPrecipitationMap)
            pbPrecipitationLoading = findViewById(R.id.pbPrecipitationLoading)
            tvPrecipitationError = findViewById(R.id.tvPrecipitationError)
            flPrecipitationContainer = findViewById(R.id.flPrecipitationContainer)

            flPrecipitationContainer.setOnClickListener {
                openFullPrecipitationMap()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing precipitation views: ${e.message}")
            findViewById<TextView>(R.id.tvPrecipitationError)?.text = "Карта осадков недоступна"
            findViewById<TextView>(R.id.tvPrecipitationError)?.visibility = View.VISIBLE
        }
    }

    private fun loadPrecipitationMap(lat: Double, lon: Double) {
        Log.d("MainActivity", "Loading precipitation map for lat=$lat, lon=$lon")

        lifecycleScope.launch {
            try {
                russianWeatherRepo.loadPrecipitationMap(lat, lon)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in loadPrecipitationMap: ${e.message}", e)
            }
        }

        russianWeatherRepo.precipitationMap.observe(this) { bitmap ->
            if (bitmap != null) {
                ivPrecipitationMap.setImageBitmap(bitmap)
                pbPrecipitationLoading.visibility = View.GONE
                tvPrecipitationError.visibility = View.GONE
            } else {
                tvPrecipitationError.text = "Не удалось загрузить карту"
                tvPrecipitationError.visibility = View.VISIBLE
            }
        }

        russianWeatherRepo.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Log.e("MainActivity", "Map error: $error")
                tvPrecipitationError.text = error
                tvPrecipitationError.visibility = View.VISIBLE
                pbPrecipitationLoading.visibility = View.GONE
            }
        }
    }

    // === ПОЧАСОВКА ===
    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvHourlyForecast)
        adapter = HourlyForecastAdapter()
        rv.adapter = adapter
    }

    private fun loadHourlyForecast(locationData: LocationData) {
        lifecycleScope.launch {
            try {
                val hourlyData = weatherRepo.loadHourlyForecastWithAutoLocation(locationData)
                adapter.setData(hourlyData)
                findViewById<RecyclerView>(R.id.rvHourlyForecast)
                    .addItemDecoration(TemperatureLineDecorator(hourlyData.map { it.temperature }))
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading hourly forecast", e)
                initializeMockData()
            }
        }
    }

    private fun initializeMockData() {
        val mockData = weatherRepo.getMockHourlyData()
        adapter.setData(mockData)
        findViewById<RecyclerView>(R.id.rvHourlyForecast)
            .addItemDecoration(TemperatureLineDecorator(mockData.map { it.temperature }))
    }

    // === ГЕОПОЗИЦИЯ ===
    private fun getCurrentLocation(): Location? {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastGps != null && isLocationValid(lastGps)) {
                    return lastGps
                }

                val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastNetwork != null && isLocationValid(lastNetwork)) {
                    return lastNetwork
                }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            }
        } catch (e: Exception) {
            Log.e("Location", "Error getting location: ${e.message}")
        }

        return createDefaultLocation()
    }

    private fun isLocationValid(location: Location): Boolean {
        val currentTime = System.currentTimeMillis()
        val locationTime = location.time
        val timeDiff = currentTime - locationTime
        return timeDiff < 3600000 && location.accuracy < 1000f
    }

    private fun openFullPrecipitationMap() {
        // ⭐ ИСПРАВЛЕНО: Удалены лишние пробелы в URL
        val fullMapUrl = "https://yandex.ru/pogoda/ru/maps/nowcast/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullMapUrl))
        startActivity(intent)
    }

    // === ФОН ===
    private fun updateBackground() {
        val weatherDesc = tvWeatherDesc.text.toString()
        val weatherCondition = parseWeatherCondition(weatherDesc)
        val timeOfDay = TimeUtils.getCurrentTimeOfDay()

        val backgroundRes = getBackgroundResource(weatherCondition, timeOfDay)
        performSmoothBackgroundTransition(backgroundRes)
    }

    private fun parseWeatherCondition(description: String): WeatherCondition {
        val lowerDesc = description.lowercase()
        return when {
            lowerDesc.contains("ясно") || lowerDesc.contains("солнечно") ||
                    lowerDesc.contains("clear") || lowerDesc.contains("sun") -> WeatherCondition.CLEAR
            lowerDesc.contains("облачно") || lowerDesc.contains("пасмурно") ||
                    lowerDesc.contains("cloud") || lowerDesc.contains("overcast") -> WeatherCondition.CLOUDY
            lowerDesc.contains("дождь") || lowerDesc.contains("ливень") ||
                    lowerDesc.contains("rain") || lowerDesc.contains("drizzle") ||
                    lowerDesc.contains("гроза") || lowerDesc.contains("thunder") ||
                    lowerDesc.contains("storm") -> WeatherCondition.RAIN
            lowerDesc.contains("снег") || lowerDesc.contains("snow") -> WeatherCondition.SNOW
            else -> WeatherCondition.CLEAR
        }
    }

    private fun getBackgroundResource(
        weatherCondition: WeatherCondition,
        timeOfDay: TimeOfDay
    ): Int = when (weatherCondition) {
        WeatherCondition.CLEAR -> when (timeOfDay) {
            TimeOfDay.DAY -> R.drawable.bg_clear_day
            TimeOfDay.NIGHT -> R.drawable.bg_clear_night
        }
        WeatherCondition.CLOUDY -> when (timeOfDay) {
            TimeOfDay.DAY -> R.drawable.bg_cloudy_day
            TimeOfDay.NIGHT -> R.drawable.bg_cloudy_night
        }
        WeatherCondition.RAIN -> when (timeOfDay) {
            TimeOfDay.DAY -> R.drawable.bg_rain_day
            TimeOfDay.NIGHT -> R.drawable.bg_rain_night
        }
        WeatherCondition.SNOW -> when (timeOfDay) {
            TimeOfDay.DAY -> R.drawable.bg_snow_day
            TimeOfDay.NIGHT -> R.drawable.bg_snow_night
        }
    }

    private fun performSmoothBackgroundTransition(backgroundRes: Int) {
        val newDrawable = ContextCompat.getDrawable(this, backgroundRes)
        if (newDrawable != null) {
            val fadeOut = ObjectAnimator.ofFloat(ivBackground, "alpha", 1f, 0.3f)
            fadeOut.duration = 300
            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ivBackground.setImageDrawable(newDrawable)
                    val fadeIn = ObjectAnimator.ofFloat(ivBackground, "alpha", 0.3f, 1f)
                    fadeIn.duration = 400
                    fadeIn.start()
                }
            })
            fadeOut.start()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(timeChangedReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        })
        timeUpdateHandler = Handler(Looper.getMainLooper())
        timeUpdateHandler?.post(timeUpdateRunnable)
        observeWeatherData() // ⭐ Начинаем наблюдение при возобновлении
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(timeChangedReceiver)
        timeUpdateHandler?.removeCallbacks(timeUpdateRunnable)
        timeUpdateHandler = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadWeatherWithAutoLocation()
        }
    }
}