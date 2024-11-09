package com.example.cardioalert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.example.cardioalert.databinding.ActivityMainBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
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
import java.lang.Math.sin
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var mqttAndroidClient: MqttAndroidClient
    private val mqttBrokerUrl = "tcp://10.20.9.250:1883"
    val clientId = MqttClient.generateClientId() // Unique client ID

    private lateinit var binding: ActivityMainBinding

    private var keepRunning = true

    val TOPIC_NAME = "watch/heartbeat"

    val age = 45
    val sex = "M"
    var baselineRestingHeartRate = 70 // Typical resting heart rate
    var baselineMaxHR = 220 - age     // Maximum heart rate based on age
    var baselineRestingBP = 120
    var baselineCholesterol = 200
    var baselineOldpeak = 1.0


    private val emergencyAlertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showEmergencyAlert()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerReceiver(
            emergencyAlertReceiver,
            IntentFilter("com.example.cardioalert.EMERGENCY_ALERT")
        )

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

    private fun showEmergencyAlert() {
        // Set the bitmap and ensure visibility
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.heart)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true) // Scale to 100x100
        binding.emergencyAlertImage.setImageBitmap(scaledBitmap)
        binding.emergencyAlertImage.visibility = View.VISIBLE
        binding.emergencyAlertText.visibility = View.VISIBLE

        // Ensure animation starts after the view is visible
        binding.emergencyAlertImage.post {
            // Use ScaleAnimation as an alternative to ObjectAnimator
            val scaleAnimation = ScaleAnimation(
                1f, 1.2f,  // Scale from original size to 1.2x
                1f, 1.2f,  // Scale from original size to 1.2x
                Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot at the center of X axis
                Animation.RELATIVE_TO_SELF, 0.5f   // Pivot at the center of Y axis
            ).apply {
                duration = 500
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }

            // Start the animation
            binding.emergencyAlertImage.startAnimation(scaleAnimation)

            // Stop the animation after 5 seconds
            binding.emergencyAlertImage.postDelayed({
                binding.emergencyAlertImage.clearAnimation() // Stop the animation
                binding.emergencyAlertImage.visibility = View.GONE // Hide the alert image if needed
                binding.emergencyAlertText.visibility = View.GONE // Hide the alert text if needed
            }, 5000) // 5 seconds in milliseconds
        }
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
                binding.ageTextView.text = "Age: ${fakeData.Age}"
                binding.sexTextView.text = "Sex: ${fakeData.Sex}"
                binding.chestPainTypeTextView.text = "Chest Pain Type: ${fakeData.ChestPainType}"
                binding.restingBPTextView.text = "Resting BP: ${fakeData.RestingBP} mmHg"
                binding.cholesterolTextView.text = "Cholesterol: ${fakeData.Cholesterol} mg/dL"
                binding.fastingBSTextView.text = "Fasting Blood Sugar: ${fakeData.FastingBS}"
                binding.restingECGTextView.text = "Resting ECG: ${fakeData.RestingECG}"
                binding.maxHRTextView.text = "Max Heart Rate: ${fakeData.MaxHR} bpm"
                binding.exerciseAnginaTextView.text = "Exercise Angina: ${fakeData.ExerciseAngina}"
                binding.oldpeakTextView.text = "Oldpeak: ${fakeData.Oldpeak}"
                binding.stSlopeTextView.text = "ST Slope: ${fakeData.ST_Slope}"

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
        val Age: Int,
        val Sex: String,
        val ChestPainType: String = "ATA", // ChestPainType ["TA", "ATA", "NAP", ASY"]
        val RestingBP: Int,
        val Cholesterol: Int,
        val FastingBS: Int,
        val RestingECG: String = "Normal",
        val MaxHR: Int,
        val ExerciseAngina: String = "N",
        val Oldpeak: Float,
        val ST_Slope: String,
        val DEVICE_ID: String? = null // Add FCM token field
    )

    private fun generateFakeData(): FakeData {
        val sharedPreferences =
            getSharedPreferences("com.example.cardioalert", Context.MODE_PRIVATE)
        val fcmToken = sharedPreferences.getString("fcm_token", null)
        fcmToken?.run {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                val token = task.result
                getString(R.string.msg_token_fmt, token)
            })
        }

        // Add a 20% chance of generating heart attack-like data for testing
        val isHeartAttackSimulated = (1..100).random() <= 5

        // Calculate daily cycle factors for natural fluctuation
        val currentHour = (System.currentTimeMillis() / (1000 * 60 * 60) % 24).toInt()
        val dailyCycleFactor = sin(currentHour * (Math.PI / 12)) // 24-hour cycle

        // Generate simulated data with or without heart attack-like values
        val restingBP = if (isHeartAttackSimulated) (180..200).random() else
            (baselineRestingBP + dailyCycleFactor * 5 + (-3..3).random()).roundToInt()
                .coerceIn(90, 180)

        val cholesterol = if (isHeartAttackSimulated) (300..400).random() else
            (baselineCholesterol + (-10..10).random()).coerceIn(125, 350)

        val fastingBS = if (isHeartAttackSimulated) 1 else if ((70..120).random() >= 120) 1 else 0

        val restingECG =
            if (isHeartAttackSimulated) "ST" else if ((1..100).random() < 5) "ST" else "Normal"

        val oldpeak = if (isHeartAttackSimulated) (2.5 + Math.random() * 2).toFloat() else
            (baselineOldpeak + dailyCycleFactor * 0.5 + (-0.2 + Math.random() * 0.4)).coerceIn(
                0.0,
                5.0
            ).toFloat()

        val st_slope = if (isHeartAttackSimulated) "Down" else listOf("Up", "Flat", "Down").random()

        val maxHR =
            if (isHeartAttackSimulated) (baselineMaxHR + 20..baselineMaxHR + 40).random() else
                (baselineMaxHR + dailyCycleFactor * 5).roundToInt().coerceAtMost(baselineMaxHR)

        // Use exercise/stress event logic
        val exerciseEvent = (1..100).random() < 10
        if (exerciseEvent) {
            // Temporarily raise the heart rate due to simulated exercise or stress
            baselineRestingHeartRate =
                (baselineRestingHeartRate + (maxHR - baselineRestingHeartRate) * 0.7).roundToInt()
        } else {
            baselineRestingHeartRate = (baselineRestingHeartRate - 1).coerceAtLeast(60)
        }

        return FakeData(
            Age = age,
            Sex = sex,
            RestingBP = restingBP,
            Cholesterol = cholesterol,
            FastingBS = fastingBS,
            RestingECG = restingECG,
            MaxHR = maxHR,
            ExerciseAngina = if (isHeartAttackSimulated) "Y" else "N",
            Oldpeak = oldpeak,
            ST_Slope = st_slope,
            DEVICE_ID = fcmToken
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

            sendBroadcastToActivity(title, body)
        }
    }

    private fun sendBroadcastToActivity(title: String, body: String) {
        val intent = Intent("com.example.cardioalert.EMERGENCY_ALERT")
        intent.putExtra("title", title)
        intent.putExtra("body", body)
        sendBroadcast(intent)
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
