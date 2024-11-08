package com.example.cardioalert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.example.cardioalert.databinding.ActivityMainBinding
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import info.mqtt.android.service.Ack
import info.mqtt.android.service.MqttAndroidClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        requestNotificationPermission()

        mqttAndroidClient =
            MqttAndroidClient(applicationContext, mqttBrokerUrl, clientId, Ack.AUTO_ACK)
        val mqttConnectOptions = MqttConnectOptions().apply {
            isCleanSession = true  // Ensures clean session (no cached data)
            connectionTimeout = 30  // Connection timeout in seconds
            keepAliveInterval = 20  // Send ping every 20 seconds to keep the connection alive
            isAutomaticReconnect = false  // Automatically reconnect if the connection is lost
        }
        mqttConnectOptions.userName = "name"
        mqttConnectOptions.password = "password".toCharArray()
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
                binding.bloodPressureTextView.text = "Blood Pressure: ${fakeData.systolic}/${fakeData.diastolic} mmHg"
                binding.ageTextView.text = "Age: ${fakeData.age}"
                binding.sexTextView.text = "Sex: ${fakeData.sex}"
                binding.chestPainTypeTextView.text = "Chest Pain Type: ${fakeData.chestPainType}"
                binding.restingBPTextView.text = "Resting BP: ${fakeData.restingBP} mmHg"
                binding.cholesterolTextView.text = "Cholesterol: ${fakeData.cholesterol} mg/dL"
                binding.fastingBSTextView.text = "Fasting Blood Sugar: ${fakeData.fastingBS}"
                binding.restingECGTextView.text = "Resting ECG: ${fakeData.restingECG}"
                binding.maxHRTextView.text = "Max Heart Rate: ${fakeData.maxHR} bpm"
                binding.exerciseAnginaTextView.text = "Exercise Angina: ${fakeData.exerciseAngina}"
                binding.oldpeakTextView.text = "Oldpeak: ${fakeData.oldpeak}"
                binding.stSlopeTextView.text = "ST Slope: ${fakeData.st_slope}"
                binding.timestampTextView.text = "Timestamp: ${fakeData.timestamp}"
                binding.fcmTokenTextView.text = "FCM Token: ${fakeData.fcmToken ?: "N/A"}"

                // Convert fake data to JSON and publish
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
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    data class FakeData(
        val age: Int = 45,
        val sex: String = "M",
        val chestPainType: String = "ATA", // ChestPainType ["TA", "ATA", "NAP", ASY"]
        val restingBP: Int,
        val cholesterol: Int,
        val fastingBS: Int,
        val restingECG: String = "Normal",
        val maxHR: Int,
        val exerciseAngina: String = "N",
        val oldpeak: Double,
        val st_slope: String,
        val heartRate: Int,
        val systolic: Int,
        val diastolic: Int,
        val timestamp: Long,
        val fcmToken: String? = null // Add FCM token field
    )

    private fun generateFakeData(): FakeData {
        val sharedPreferences =
            getSharedPreferences("com.example.cardioalert", Context.MODE_PRIVATE)
        val fcmToken = sharedPreferences.getString("fcm_token", null)

        return FakeData(
            restingBP = (90..180).random(),
            cholesterol = (125..350).random(),
            fastingBS = if ((70..120).random() >= 120) 1 else 0,
            maxHR = (60..202).random(),
            oldpeak = (0..60).random() / 10.0,
            st_slope = listOf("Up", "Flat", "Down").random(),
            heartRate = (60..100).random(),
            systolic = (90..140).random(),
            diastolic = (60..90).random(),
            timestamp = System.currentTimeMillis(),
            fcmToken = fcmToken
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttAndroidClient.disconnect()
    }
}

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "HeartAttackAlertChannel"
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val sharedPreferences =
            getSharedPreferences("com.example.cardioalert", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if the message contains data or notification payload
        remoteMessage.notification?.let {
            val title = it.title ?: "Heart Attack Alert"
            val body = it.body ?: "Immediate medical attention required!"
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, message: String) {
        // Create a notification channel
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Heart Attack Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Set up an intent to open the app when the notification is tapped
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.heart)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }
}
