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
import com.example.weatherapp.domain.model.HourlyWeather
import com.example.weatherapp.domain.model.TimeOfDay
import com.example.weatherapp.domain.model.WeatherCondition
import com.example.weatherapp.utils.TimeUtils
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.data.repository.WeatherRepository
import com.example.weatherapp.domain.model.DailyForecastItem
import kotlinx.coroutines.launch

// Новые импорты для 7-дневного прогноза
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var weatherRepo: WeatherRepository

    // === Погода ===
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeatherDesc: TextView
    private lateinit var tvFeelsLike: TextView
    //private val yandexWeatherRepo = YandexWeatherRepository()

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

        // ✅ Инициализация репозитория
        weatherRepo = WeatherRepository()

        // 1. Обязательные инициализации
        initViews()
        initWeatherViews()
        initWindViews()
        initPrecipitationViews()

        // 2. Загрузка данных
        setupRecyclerView()
        initializeMockData()

        // ✅ Теперь репозиторий инициализирован
        loadCurrentWeather()
        loadPrecipitationMap()

        //Прогноз на 7 дней
        initForecastViews() // ⭐ КЛЮЧЕВОЕ: инициализируем ДО загрузки данных

        // 2. Загрузка данных
        setupRecyclerView()
        initializeMockData()
        loadCurrentWeather()
        loadPrecipitationMap()



        updateBackground()
    }

    private fun initViews() {
        ivBackground = findViewById(R.id.ivBackground)
        findViewById<TextView>(R.id.tvLocationName).text = "Верхняя Пышма"
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


    private fun getWindDescription(speed: Float): String { // speed в м/с
        return when {
            speed < 0.3f -> "Штиль"
            speed < 1.6f -> "Тихий"           // 1 м/с = 3.6 км/ч
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

    // === ЗАГРУЗКА ПОГОДЫ ===
    private fun loadCurrentWeather() {
        Log.d("Weather", "Starting weather load...")

        lifecycleScope.launch {
            Log.d("Weather", "Launching coroutine...")
            weatherRepo.loadWeather(56.9474, 60.5707)
        }

        weatherRepo.weatherData.observe(this) { data ->
            Log.d("Weather", "Weather data received: $data")
            updateWeatherUI(data)
        }

        weatherRepo.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Log.e("Weather", "Error: $error")

            }
        }
    }

    private fun updateWeatherUI(data: com.example.weatherapp.domain.model.WeatherData) {
        // Обновляем основную погоду
        tvTemperature.text = data.temperature
        tvWeatherDesc.text = data.description
        tvFeelsLike.text = "Ощущается как ${data.feelsLike}"

        // обновляем восход / закат
        tvSunrise.text = data.sunrise
        tvSunset.text = data.sunset

        // ⭐ Обновляем влажность
        updateHumidityUI(data.humidity)

        // ⭐ Обновляем данные о ветре
        // Скорость ветра
        tvWindSpeed.text = String.format("%.1f", data.windSpeed) // например: "2,3"
        tvWindUnit.text = "м/с"

        // Описание силы ветра (например: "Умеренный ветер")
        tvWindDescription.text = getWindDescription(data.windSpeed)

        // Направление ветра текстом (например: "СВ")
        tvWindDirectionText.text = data.windDirectionText

        // Поворот стрелки компаса (в градусах: 0° = север, 90° = восток и т.д.)
        ivWindDirection.rotation = data.windDirection

        // ⭐ Обновляем прогноз на 7 дней
        updateForecastUI(data.dailyForecast)
        // Обновляем фон под новое описание погоды
        updateBackground()
    }

    private fun updateForecastUI(forecast: List<DailyForecastItem>) {
        forecastViews.forEachIndexed { index, (dayView, weatherView, tempView) ->
            if (index < forecast.size) {
                val item = forecast[index]
                dayView.text = item.dayOfWeek
                // ✅ ИСПРАВЛЕНО: сначала дневная (макс), потом ночная (мин)
                tempView.text = "${item.tempMax}° / ${item.tempMin}°"
                weatherView.setImageResource(getWeatherIcon(item.weatherCode))
            }
        }
    }

    // ⭐ Преобразуем weather code в иконку
    private fun getWeatherIcon(weatherCode: Int): Int {
        return when {
            weatherCode == 0 -> R.drawable.ic_sunny_day // Ясно
            weatherCode in 1..3 -> R.drawable.ic_cloudy_day // Облачно
            weatherCode in 45..48 -> R.drawable.ic_foggy // Туман
            weatherCode in 51..57 -> R.drawable.ic_rainy// Морось
            weatherCode in 61..67 -> R.drawable.ic_rainy // Дождь
            weatherCode in 71..77 -> R.drawable.ic_snowy // Снег
            weatherCode in 80..82 -> R.drawable.ic_rainy // Ливень
            else -> R.drawable.ic_cloudy_day
        }
    }

    private fun updateHumidityUI(humidity: Int) {
        pbHumidity.progress = humidity
        tvHumidityValue.text = "${humidity}%"
    }

    // === КАРТА ОСАДКОВ (без изменений) ===
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