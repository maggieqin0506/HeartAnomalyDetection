package com.example.cardioalert

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.cardioalert.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val handler = Handler(Looper.getMainLooper()) // to be deleted
    private var keepRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        startSendingFakeData()
        testWithoutNetwork()
    }

    private fun testWithoutNetwork() {
        handler.post(object : Runnable {
            override fun run() {
                if (keepRunning) {
                    val fakeData = generateFakeData()

                    // Update the UI with the fake data
                    binding.heartRateTextView.text = "Heart Rate: ${fakeData.heartRate} bpm"
                    binding.bloodPressureTextView.text =
                        "Blood Pressure: ${fakeData.bloodPressure} mmHg"

                    // Post this Runnable again with a 1-second delay
                    handler.postDelayed(this, 1000L) // 1000 milliseconds = 1 second
                }
            }
        })
    }

    private fun generateFakeData(): HealthData = HealthData(
        heartRate = (60..100).random(),
        bloodPressure = "${(90..140).random()}/${(60..90).random()}",
        timestamp = System.currentTimeMillis()
    )
}