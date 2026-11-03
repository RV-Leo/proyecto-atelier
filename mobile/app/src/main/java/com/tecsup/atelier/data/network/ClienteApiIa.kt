package com.tecsup.atelier.data.network

import com.tecsup.atelier.BuildConfig
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit hacia la API de Atelier. La clave del proveedor de IA
 * permanece en el backend y nunca se distribuye con la aplicación.
 */
object ClienteApiIa {

    private val interceptorAutenticacion = Interceptor { chain ->
        val solicitud = chain.request().newBuilder()
            .header("Content-Type", "application/json")
        if (BuildConfig.API_USER.isNotBlank() && BuildConfig.API_PASSWORD.isNotBlank()) {
            solicitud.header(
                "Authorization",
                Credentials.basic(BuildConfig.API_USER, BuildConfig.API_PASSWORD)
            )
        }
        chain.proceed(solicitud.build())
    }

    private val interceptorLogging = HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(interceptorAutenticacion)
            .addInterceptor(interceptorLogging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val servicio: ServicioApiIa by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServicioApiIa::class.java)
    }
}
