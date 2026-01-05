// ui/main/HourlyForecastAdapter.kt
package com.example.weatherapp.ui.main

/*
связывает данные о почасовой погоде из главной активити с разметкой item_hourly_forecast.xml
делает эту передачу безопасной и структурированной
 */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.domain.model.HourlyWeather


class HourlyForecastAdapter : RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder>() {

    private var data = listOf<HourlyWeather>()

    fun setData(newData: List<HourlyWeather>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    override fun getItemCount() = data.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHour: TextView = itemView.findViewById(R.id.tvHour)
        private val ivWeather: ImageView = itemView.findViewById(R.id.ivWeather)
        private val tvTemp: TextView = itemView.findViewById(R.id.tvTemp)
        private val ivDrop: ImageView = itemView.findViewById(R.id.ivDrop)
        private val tvHumidity: TextView = itemView.findViewById(R.id.tvHumidity)

        fun bind(item: HourlyWeather) {
            tvHour.text = item.hour
            ivWeather.setImageResource(item.weatherIcon)
            tvTemp.text = "${item.temperature.toInt()}°"
            tvHumidity.text = "${item.humidity}%"
        }
    }
}