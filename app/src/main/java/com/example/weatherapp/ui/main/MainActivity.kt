package com.example.weatherapp.ui.main

/*
точка входа приложения и пультик для  остальных классов
так же частично отвечает за связь с API
 */

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.data.repository.RussianWeatherRepository
import com.example.weatherapp.data.repository.YandexWeatherRepository
import com.example.weatherapp.domain.model.HourlyWeather
import com.example.weatherapp.domain.model.TimeOfDay
import com.example.weatherapp.domain.model.WeatherCondition
import com.example.weatherapp.utils.TimeUtils
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // === Погода ===
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeatherDesc: TextView
    private lateinit var tvFeelsLike: TextView
    private val yandexWeatherRepo = YandexWeatherRepository()

    // === Карта осадков ===
    private lateinit var ivPrecipitationMap: ImageView
    private lateinit var pbPrecipitationLoading: ProgressBar
    private lateinit var tvPrecipitationError: TextView
    private lateinit var flPrecipitationContainer: FrameLayout
    private var currentLocation: Location? = null
    private val weatherRepository = RussianWeatherRepository()

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

        // 1. Обязательные инициализации
        initViews()
        initWeatherViews()

        // 2. Инициализация вьюшек карты ДО загрузки
        initPrecipitationViews()  // ✅ Ключевое изменение

        // 3. Загрузка данных
        setupRecyclerView()
        initializeMockData()
        loadCurrentWeather()
        loadPrecipitationMap()    // ✅ Теперь безопасно

        // Инициализация ветра
        initWindViews()

        // Установка данных ветра (в реальном приложении эти данные будут приходить из API)
        setWindData(direction = 45f, speed = 10f) // 45° - северо-восток, 13 км/ч

        updateBackground()
    }

    private fun initViews() {
        ivBackground = findViewById(R.id.ivBackground)
        findViewById<TextView>(R.id.tvLocationName).text = "Верхняя Пышма"
    }

    private fun initWindViews() {
        ivWindDirection = findViewById(R.id.ivWindDirection)
        tvWindSpeed = findViewById(R.id.tvWindSpeed)
        tvWindUnit = findViewById(R.id.tvWindUnit)
        tvWindDescription = findViewById(R.id.tvWindDescription)
        tvWindDirectionText = findViewById(R.id.tvWindDirectionText)
    }

    private fun setWindData(direction: Float, speed: Float, directionText: String = "—") {
        // Скорость
        tvWindSpeed.text = speed.toInt().toString()
        tvWindUnit.text = "км/ч"

        // Описание силы
        tvWindDescription.text = getWindDescription(speed)

        // Направление текстом
        tvWindDirectionText.text = directionText

        // Поворот стрелки
        ivWindDirection.rotation = direction
    }

    private fun getWindDirectionText(degrees: Float): String {
        return when {
            degrees in 348.75f..360f || degrees in 0f..11.25f -> "С"
            degrees in 11.25f..33.75f -> "ССВ"
            degrees in 33.75f..56.25f -> "СВ"
            degrees in 56.25f..78.75f -> "ВСВ"
            degrees in 78.75f..101.25f -> "В"
            degrees in 101.25f..123.75f -> "ВЮВ"
            degrees in 123.75f..146.25f -> "ЮВ"
            degrees in 146.25f..168.75f -> "ЮЮВ"
            degrees in 168.75f..191.25f -> "Ю"
            degrees in 191.25f..213.75f -> "ЮЮЗ"
            degrees in 213.75f..236.25f -> "ЮЗ"
            degrees in 236.25f..258.75f -> "ЗЮЗ"
            degrees in 258.75f..281.25f -> "З"
            degrees in 281.25f..303.75f -> "ЗСЗ"
            degrees in 303.75f..326.25f -> "СЗ"
            degrees in 326.25f..348.75f -> "ССЗ"
            else -> "—"
        }
    }

    private fun getWindDescription(speed: Float): String {
        return when {
            speed < 1 -> "Штиль"
            speed < 4 -> "Лёгкий ветер"
            speed < 8 -> "Слабый ветер"
            speed < 12 -> "Умеренный ветер"
            speed < 16 -> "Свежий ветер"
            speed < 20 -> "Сильный ветер"
            speed < 24 -> "Крепкий ветер"
            speed < 28 -> "Очень крепкий ветер"
            speed < 32 -> "Шторм"
            else -> "Сильный шторм"
        }
    }

    private fun initWeatherViews() {
        tvTemperature = findViewById(R.id.tvTemperature)
        tvWeatherDesc = findViewById(R.id.tvWeatherDesc)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)
    }

    // === ЗАГРУЗКА ПОГОДЫ ===
    private fun loadCurrentWeather() {
        lifecycleScope.launch {
            yandexWeatherRepo.loadWeather("verkhnyaya-pyshma")
        }

        yandexWeatherRepo.weatherData.observe(this) { data ->
            updateWeatherUI(data)
        }

        yandexWeatherRepo.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Log.e("Weather", error)
                // fallback-значения
                tvTemperature.text = "−12°"
                tvWeatherDesc.text = "Облачно"
                tvFeelsLike.text = "Ощущается как −18°"
            }
        }
    }

    private fun updateWeatherUI(data: com.example.weatherapp.domain.model.WeatherData) {
        // Обновляем основную погоду
        tvTemperature.text = data.temperature
        tvWeatherDesc.text = data.description
        tvFeelsLike.text = "Ощущается как ${data.feelsLike}"

        // ⭐ Обновляем данные о ветре
        // Скорость ветра
        tvWindSpeed.text = data.windSpeed.toInt().toString()
        tvWindUnit.text = "км/ч"

        // Описание силы ветра (например: "Умеренный ветер")
        tvWindDescription.text = getWindDescription(data.windSpeed)

        // Направление ветра текстом (например: "СВ")
        tvWindDirectionText.text = data.windDirectionText

        // Поворот стрелки компаса (в градусах: 0° = север, 90° = восток и т.д.)
        ivWindDirection.rotation = data.windDirection

        // Обновляем фон под новое описание погоды
        updateBackground()
    }

    // === КАРТА ОСАДКОВ (без изменений) ===

    // === Исправленная инициализация карты осадков ===
    private fun initPrecipitationViews() {
        try {
            // ⭐ ВАЖНО: Проверяем, что вьюшки существуют в разметке
            ivPrecipitationMap = findViewById(R.id.ivPrecipitationMap)
            pbPrecipitationLoading = findViewById(R.id.pbPrecipitationLoading)
            tvPrecipitationError = findViewById(R.id.tvPrecipitationError)
            flPrecipitationContainer = findViewById(R.id.flPrecipitationContainer)

            // Обработчик клика по карте осадков
            flPrecipitationContainer.setOnClickListener {
                openFullPrecipitationMap()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing precipitation views: ${e.message}")
            // Если вьюшки не найдены, показываем ошибку
            findViewById<TextView>(R.id.tvPrecipitationError)?.text = "Карта осадков недоступна"
            findViewById<TextView>(R.id.tvPrecipitationError)?.visibility = View.VISIBLE
        }
    }

    private fun loadPrecipitationMap() {
        // Получаем текущую геопозицию
        val location = getCurrentLocation()
        currentLocation = location
        val lat = location?.latitude ?: 56.9474  // Верхняя Пышма
        val lon = location?.longitude ?: 60.5707

        Log.d("MainActivity", "Loading precipitation map for lat=$lat, lon=$lon")

        // Запускаем в IO scope
        lifecycleScope.launch {
            try {
                weatherRepository.loadPrecipitationMap(lat, lon)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in loadPrecipitationMap: ${e.message}", e)
                // ⭐ Не вызывай postValue() напрямую — это защищённый метод!
                // Вместо этого используй уже существующий механизм через _error.postValue(...)
            }
        }

        // Наблюдаем за изменениями
        weatherRepository.precipitationMap.observe(this) { bitmap ->
            Log.d("MainActivity", "Precipitation map received: ${bitmap != null}")
            if (bitmap != null) {
                ivPrecipitationMap.setImageBitmap(bitmap)
                pbPrecipitationLoading.visibility = View.GONE
                tvPrecipitationError.visibility = View.GONE
            } else {
                tvPrecipitationError.text = "Не удалось загрузить карту"
                tvPrecipitationError.visibility = View.VISIBLE
            }
        }

        weatherRepository.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Log.e("MainActivity", "Map error: $error")
                tvPrecipitationError.text = error
                tvPrecipitationError.visibility = View.VISIBLE
                pbPrecipitationLoading.visibility = View.GONE
            }
        }
    }



    private fun getCurrentLocation(): Location? {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Проверяем разрешения
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Сначала пробуем GPS
                val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastGps != null) {
                    Log.d("Location", "GPS location found")
                    return lastGps
                }

                // Затем сетевые провайдеры
                val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastNetwork != null) {
                    Log.d("Location", "Network location found")
                    return lastNetwork
                }
            } else {
                // Запрашиваем разрешение
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            }
        } catch (e: Exception) {
            Log.e("Location", "Error getting location: ${e.message}")
        }

        // Дефолтные координаты (Верхняя Пышма)
        return Location("default").apply {
            latitude = 56.9474
            longitude = 60.5707
        }
    }

    private fun openFullPrecipitationMap() {
        // ⭐ Открываем карту Gismeteo (работает в РФ)
        val fullMapUrl = "https://yandex.ru/pogoda/ru/maps/nowcast/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullMapUrl))
        startActivity(intent)
    }

    // === Остальные методы (без изменений) ===
    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvHourlyForecast)
        adapter = HourlyForecastAdapter()
        rv.adapter = adapter
    }

    private fun initializeMockData() {
        val mockData = listOf(
            HourlyWeather("21:00", R.drawable.ic_cloudy_night, -28.0, 3),
            HourlyWeather("22:00", R.drawable.ic_clear_night, -15.0, 14),
            HourlyWeather("23:00", R.drawable.ic_clear_night, -28.0, 5),
            HourlyWeather("00:00", R.drawable.ic_rainy, -18.0, 24),
            HourlyWeather("01:00", R.drawable.ic_rainy, -10.0, 17),
            HourlyWeather("02:00", R.drawable.ic_sunny_day, -23.0, 31),
            HourlyWeather("03:00", R.drawable.ic_sunny_day, -30.0, 24),
            HourlyWeather("04:00", R.drawable.ic_cloudy_day, -12.0, 28),
            HourlyWeather("05:00", R.drawable.ic_sunny_day, 5.0, 19),
            HourlyWeather("06:00", R.drawable.ic_snowy, -18.0, 24),
            HourlyWeather("07:00", R.drawable.ic_cloudy_day, -10.0, 17),
            HourlyWeather("08:00", R.drawable.ic_sunny_day, -23.0, 31),
            HourlyWeather("09:00", R.drawable.ic_sunny_day, -30.0, 24)
        )

        adapter.setData(mockData)
        findViewById<RecyclerView>(R.id.rvHourlyForecast)
            .addItemDecoration(TemperatureLineDecorator(mockData.map { it.temperature }))
    }

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
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(timeChangedReceiver)
        timeUpdateHandler?.removeCallbacks(timeUpdateRunnable)
        timeUpdateHandler = null
    }

    // Обработка результата запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Повторная загрузка карты после получения разрешения
            loadPrecipitationMap()
        }
    }
}