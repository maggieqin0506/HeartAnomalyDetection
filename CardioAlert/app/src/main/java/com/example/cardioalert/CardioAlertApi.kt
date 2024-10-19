package com.example.cardioalert

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST

interface CardioAlertApi {
    @POST("/sendData")
    suspend fun postHealthData(
        @Body healthData: HealthData
    )
}

data class HealthData(
    val heartRate: Int,
    val bloodPressure: String,
    val timestamp: Long
)

object Network {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val retrofit = Retrofit.Builder().baseUrl("todo")
        .addConverterFactory(MoshiConverterFactory.create(moshi)).build()
    val restaurantFinderApi: CardioAlertApi = retrofit.create()
}