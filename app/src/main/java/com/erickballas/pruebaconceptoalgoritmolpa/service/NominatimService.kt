package com.erickballas.pruebaconceptoalgoritmolpa.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Modelo de datos para el resultado de la búsqueda
data class NominatimResult(
    val display_name: String,
    val lat: String,
    val lon: String
)

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressDetails: Int = 1
    ): List<NominatimResult>
}

object NominatimClient {
    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    // Cliente HTTP con User-Agent (Obligatorio para OSM)
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AlertifyApp/1.0") // ¡Importante!
                .build()
            chain.proceed(request)
        }
        .build()

    val service: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}