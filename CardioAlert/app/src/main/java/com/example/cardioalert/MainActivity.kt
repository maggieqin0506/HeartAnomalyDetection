package com.example.cardioalert

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cardioalert.databinding.ActivityMainBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

class MainActivity : AppCompatActivity() {

    private lateinit var mqttAndroidClient: MqttAndroidClient
    private val mqttBrokerUrl = "tcp://10.20.9.250:1883"
    val clientId = MqttClient.generateClientId() // Unique client ID

    private lateinit var binding: ActivityMainBinding

    private var keepRunning = true

    val TOPIC_NAME = "watch/heartbeat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mqttAndroidClient = MqttAndroidClient(applicationContext, mqttBrokerUrl, clientId)
        val mqttConnectOptions = MqttConnectOptions().apply {
            isCleanSession = true  // Ensures clean session (no cached data)
            connectionTimeout = 30  // Connection timeout in seconds
            keepAliveInterval = 20  // Send ping every 20 seconds to keep the connection alive
            isAutomaticReconnect = true  // Automatically reconnect if the connection is lost
        }
        mqttConnectOptions.mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1

        connectToBroker(mqttConnectOptions)
        startUpdatingFakeData()
    }

    private fun connectToBroker(options: MqttConnectOptions) {
        try {
            mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Connected to broker successfully!")
                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to connect to broker: ${exception?.message}")
                    // Retry connection after a delay
                    lifecycleScope.launch(Dispatchers.Main) {
                        delay(5000)  // Wait for 5 seconds before retrying
                        connectToBroker(options)  // Retry connecting to the broker
                    }
                }

            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(TOPIC_NAME, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Subscribed to topic: $TOPIC_NAME")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to subscribe: ${exception?.message}")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun startUpdatingFakeData() {
        lifecycleScope.launch(Dispatchers.Main) {
            while (keepRunning) {
                // Generate fake data
                val fakeData = generateFakeData()

                // Update the UI with the fake data
                binding.heartRateTextView.text = "Heart Rate: ${fakeData.heartRate} bpm"
                binding.bloodPressureTextView.text =
                    "Blood Pressure: ${fakeData.systolic}/${fakeData.diastolic} mmHg"

                val fakeDataJson = Gson().toJson(fakeData)
                publishFakeDataToMQTT(fakeDataJson)
                delay(1000L)
            }
        }
    }

    private fun publishFakeDataToMQTT(message: String) {
        try {
            // Check if the client is connected before publishing
            if (!mqttAndroidClient.isConnected) {
                Log.e("MQTT", "Client not connected. Attempting to reconnect...")
                mqttAndroidClient.connect(null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT", "Reconnected successfully. Now publishing the message.")
                        publishMessage(message) // Publish after reconnecting
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT", "Failed to reconnect: ${exception?.message}")
                    }
                })
            } else {
                publishMessage(message)
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun publishMessage(message: String) {
        val mqttMessage = MqttMessage(message.toByteArray())
        mqttMessage.qos = 0 // QoS level 0 (At most once)
        mqttAndroidClient.publish(TOPIC_NAME, mqttMessage)
        Log.d("MQTT", "Published message: $message to topic: $TOPIC_NAME")
    }


    data class FakeData(
        val heartRate: Int,
        val systolic: Int,
        val diastolic: Int,
        val timestamp: Long
    )

    private fun generateFakeData(): FakeData {
        return FakeData(
            heartRate = (60..100).random(),
            systolic = (90..140).random(),
            diastolic = (60..90).random(),
            timestamp = System.currentTimeMillis()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttAndroidClient.disconnect()
    }
}