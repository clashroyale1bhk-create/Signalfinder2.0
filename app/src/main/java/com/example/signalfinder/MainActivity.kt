package com.example.signalfinder

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.*
import android.location.LocationManager
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var locationManager: LocationManager

    private lateinit var signalText: TextView
    private lateinit var networkText: TextView
    private lateinit var bestText: TextView

    private var bestDbm = -999

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telephonyManager =
            getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        createUI()

        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
                ),
                100
            )
        }
    }

    private fun createUI() {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(30, 30, 30, 30)
        layout.setBackgroundColor(Color.rgb(15, 16, 20))

        fun makeText(
            text: String,
            size: Float
        ): TextView {

            return TextView(this).apply {

                this.text = text
                textSize = size
                setTextColor(Color.WHITE)

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    15,
                    0,
                    15
                )
            }
        }

        val title =
            makeText("📡 SIGNAL FINDER", 28f)

        signalText =
            makeText("Signal: -- dBm", 36f)

        networkText =
            makeText("Network: --", 20f)

        bestText =
            makeText(
                "🏆 Best spot: Not found",
                20f
            )

        val button =
            Button(this)

        button.text = "START FINDING"

        button.setOnClickListener {

            bestDbm = -999

            bestText.text =
                "🏆 Searching for best signal..."

            startScanning()
        }

        layout.addView(title)
        layout.addView(signalText)
        layout.addView(networkText)
        layout.addView(bestText)
        layout.addView(button)

        val info =
            makeText(
                "Walk slowly around your house.\n" +
                "The app keeps the strongest signal reading.",
                14f
            )

        layout.addView(info)

        setContentView(layout)
    }

    private fun startScanning() {

        handler.post(scanRunnable)
    }

    private val scanRunnable =
        object : Runnable {

            override fun run() {

                try {

                    val cells =
                        telephonyManager.allCellInfo

                    var currentDbm = -999

                    for (cell in cells) {

                        if (!cell.isRegistered)
                            continue

                        val dbm =
                            when (cell) {

                                is CellInfoLte ->
                                    cell.cellSignalStrength.dbm

                                is CellInfoWcdma ->
                                    cell.cellSignalStrength.dbm

                                is CellInfoGsm ->
                                    cell.cellSignalStrength.dbm

                                is CellInfoNr ->
                                    cell.cellSignalStrength.dbm

                                else -> -999
                            }

                        if (dbm > currentDbm) {
                            currentDbm = dbm
                        }
                    }

                    if (currentDbm != -999) {

                        signalText.text =
                            "Signal: $currentDbm dBm"

                        if (currentDbm > bestDbm) {

                            bestDbm = currentDbm

                            bestText.text =
                                "🏆 BEST SIGNAL\n" +
                                "$bestDbm dBm"
                        }
                    }

                    updateNetwork()

                } catch (e: Exception) {

                    signalText.text =
                        "Signal: waiting..."
                }

                handler.postDelayed(
                    this,
                    1500
                )
            }
        }

    private fun updateNetwork() {

        val type =
            when (
                telephonyManager.dataNetworkType
            ) {

                TelephonyManager.NETWORK_TYPE_NR ->
                    "5G"

                TelephonyManager.NETWORK_TYPE_LTE ->
                    "4G LTE"

                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA ->
                    "3G"

                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS ->
                    "2G"

                else ->
                    "Unknown"
            }

        networkText.text =
            "Network: $type"
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }
}
