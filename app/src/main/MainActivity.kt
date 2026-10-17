package com.example.chargemonitor

import android.app.Activity
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bm: BatteryManager

    private lateinit var levelMetric: TextView
    private lateinit var currentMetric: TextView
    private lateinit var powerMetric: TextView

    private lateinit var chargeState: TextView
    private lateinit var chargeMode: TextView
    private lateinit var chargeRate: TextView
    private lateinit var wireless: TextView
    private lateinit var estimate: TextView
    private lateinit var remainingCapacity: TextView
    private lateinit var protocol: TextView
    private lateinit var pps: TextView

    private lateinit var batteryVoltage: TextView
    private lateinit var usbVoltage: TextView
    private lateinit var avgCurrent: TextView
    private lateinit var batteryPower: TextView
    private lateinit var batteryTemp: TextView
    private lateinit var batteryHealth: TextView
    private lateinit var batteryTechnology: TextView
    private lateinit var chargeCounter: TextView
    private lateinit var healthEstimate: TextView
    private lateinit var refreshText: TextView

    private val refresher = object : Runnable {
        override fun run() {
            refreshData()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        window.statusBarColor = Color.rgb(116, 116, 116)
        window.navigationBarColor = Color.rgb(241, 243, 250)
        buildUi()
        refreshData()
        handler.postDelayed(refresher, 1000L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresher)
        super.onDestroy()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(239, 241, 250))
            clipToPadding = false
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(root)
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(16))
        }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(text("电池检测", 25, "#20242D", true))
        titleBox.addView(text("Redmi Turbo 3 · 实时充电监控", 13, "#8B929C", false))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text("⚡ 监控中", 14, "#238B68", true))
        root.addView(header)

        val summary = card()
        val metrics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        levelMetric = metric("电量", "--")
        currentMetric = metric("总电流", "--")
        powerMetric = metric("功率", "--")
        metrics.addView(levelMetric, weight())
        metrics.addView(currentMetric, weight())
        metrics.addView(powerMetric, weight())
        summary.addView(metrics)
        root.addView(summary, margins(12))

        val chargeCard = card()
        chargeCard.addView(section("⚡ 充电状态"))
        chargeState = line(chargeCard, "充电状态")
        chargeMode = line(chargeCard, "充电模式")
        chargeRate = line(chargeCard, "电量变化速率")
        wireless = line(chargeCard, "无线充电")
        estimate = line(chargeCard, "预计充满")
        remainingCapacity = line(chargeCard, "剩余电量估算")
        protocol = line(chargeCard, "快充协议")
        pps = line(chargeCard, "充电器最大功率")
        root.addView(chargeCard, margins(12))

        val electrical = card()
        electrical.addView(section("🔌 电压与功率"))
        batteryVoltage = line(electrical, "电池电压")
        usbVoltage = line(electrical, "USB 输入电压")
        batteryPower = line(electrical, "电池侧功率")
        avgCurrent = line(electrical, "平均电流")
        chargeCounter = line(electrical, "电量计数器")
        root.addView(electrical, margins(12))

        val health = card()
        health.addView(section("🔋 电池状态"))
        batteryTemp = line(health, "电池温度")
        batteryHealth = line(health, "系统电池状态")
        batteryTechnology = line(health, "电池技术")
        healthEstimate = line(health, "电池健康度")
        refreshText = line(health, "刷新状态")
        root.addView(health)

        root.addView(text(
            "说明：电流、功率为电池侧读数，不等于充电器 USB 输出功率。MI Turbo Charge 协议、USB 输入电压和充电器最大功率通常不由标准 Android API 提供。",
            12, "#777F8D", false
        ).apply { setPadding(dp(8), dp(14), dp(8), dp(4)) })
    }

    private fun refreshData() {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent == null) {
            chargeState.text = "无法读取"
            return
        }
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) (level * 100 / scale) else -1
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val mv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val temp10 = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "不可用"
        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

        val nowUa = safeInt(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val avgUa = safeInt(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val chargeUah = safeInt(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val full = status == BatteryManager.BATTERY_STATUS_FULL
        val stateText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "✅ 正在充电"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            else -> "状态未知"
        }

        val currentA = if (nowUa != null) nowUa / 1_000_000.0 else null
        val currentMa = currentA?.times(1000.0)
        val voltageV = if (mv > 0) mv / 1000.0 else null
        // Battery-side electrical power estimate: voltage × net battery current.
        val watts = if (voltageV != null && currentA != null) voltageV * currentA else null

        levelMetric.text = "电量\n${if (pct >= 0) "$pct%" else "不可用"}"
        currentMetric.text = "总电流\n${currentA?.let { fmt("%.2f A", abs(it)) } ?: "不可用"}"
        powerMetric.text = "功率\n${watts?.let { fmt("%.2f W", abs(it)) } ?: "不可用"}"

        chargeState.text = stateText
        chargeMode.text = when {
            plugged == 0 -> "未连接电源"
            full -> "已充满 / 仍连接电源"
            charging -> "系统报告正在充电"
            else -> "已连接电源"
        }
        chargeRate.text = if (currentMa != null && charging)
            fmt("%.0f mA（电池侧净电流）", abs(currentMa)) else "—"
        wireless.text = if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) "系统报告无线充电" else "未检测到无线充电"

        val remainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { bm.computeChargeTimeRemaining() } catch (_: Exception) { -1L }
        } else -1L
        estimate.text = when {
            full -> "已充满"
            !charging -> "暂不可估算"
            remainingMs > 0 -> formatDuration(remainingMs)
            else -> "系统未提供估算"
        }

        // Charge counter is remaining charge in microampere-hours when the device exposes it.
        remainingCapacity.text = if (chargeUah != null && chargeUah > 0)
            fmt("%.0f mAh（系统电量计数）", chargeUah / 1000.0) else "不可用"
        protocol.text = "未开放读取（MI Turbo Charge）"
        pps.text = "标准 API 不提供"

        batteryVoltage.text = voltageV?.let { fmt("%.3f V", it) } ?: "不可用"
        usbVoltage.text = "标准 API 不提供"
        batteryPower.text = watts?.let { fmt("%.2f W（电池侧估算）", it) } ?: "不可用"
        avgCurrent.text = avgUa?.let { fmt("%.0f mA", it / 1000.0) } ?: "不可用"
        chargeCounter.text = chargeUah?.let { fmt("%.0f mAh", it / 1000.0) } ?: "不可用"
        batteryTemp.text = if (temp10 >= 0) fmt("%.1f ℃", temp10 / 10.0) else "不可用"
        batteryHealth.text = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "正常"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "温度过高"
            BatteryManager.BATTERY_HEALTH_DEAD -> "电池故障"
            BatteryManager.BATTERY_HEALTH_COLD -> "温度过低"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "电压过高"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "系统报告故障"
            else -> "未知"
        }
        batteryTechnology.text = tech
        healthEstimate.text = "系统未提供真实健康百分比"
        refreshText.text = "每秒刷新 · ${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())}"
    }

    private fun safeInt(id: Int): Int? {
        return try {
            val value = bm.getIntProperty(id)
            if (value == Int.MIN_VALUE || value == 0) null else value
        } catch (_: Exception) { null }
    }

    private fun formatDuration(ms: Long): String {
        val mins = ms / 60000L
        val hours = mins / 60L
        val remain = mins % 60L
        return if (hours > 0) "约 ${hours} 小时 ${remain} 分钟" else "约 ${remain} 分钟"
    }

    private fun fmt(pattern: String, value: Double): String =
        String.format(Locale.getDefault(), pattern, value)

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(12))
        background = rounded(Color.WHITE, dp(22).toFloat())
        elevation = dp(3).toFloat()
    }

    private fun section(value: String) = text(value, 18, "#397FBD", true).apply {
        setPadding(0, 0, 0, dp(8))
    }

    private fun metric(label: String) = text("$label\n--", 17, "#209C72", true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(2), dp(10), dp(2), dp(10))
    }

    private fun line(parent: LinearLayout, label: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }
        row.addView(text(label, 14, "#858D98", false), LinearLayout.LayoutParams(0, -2, 1f))
        val value = text("读取中…", 14, "#343943", true).apply {
            gravity = Gravity.END
        }
        row.addView(value, LinearLayout.LayoutParams(0, -2, 1.5f))
        parent.addView(row)
        val divider = View(this).apply { setBackgroundColor(Color.rgb(235, 237, 241)) }
        parent.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
        return value
    }

    private fun text(value: String, size: Int, color: String, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun weight() = LinearLayout.LayoutParams(0, -2, 1f)
    private fun margins(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }
}
