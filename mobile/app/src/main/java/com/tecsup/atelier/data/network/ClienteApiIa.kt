package com.tecsup.atelier.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit singleton configurado para consumir servicios de IA.
 * Ingresa tu URL Base y tu API Key en las constantes indicadas abajo.
 */
object ClienteApiIa {

    // =========================================================================
    // TODO: INGRESA TU CONFIGURACIÓN DE CONEXIÓN A LA API DE IA AQUÍ
    // =========================================================================
    const val URL_BASE_IA = "https://api.tu-servidor-o-nube.com/" // Ej: "https://api.openai.com/" o "https://mi-servidor.com/"
    const val CLAVE_API_IA = "TU_API_KEY_AQUI"                     // Ej: "sk-proj-123456..." o "Bearer token_dedicado"
    // =========================================================================

    private val interceptorAutenticacion = Interceptor { chain ->
        val solicitudOriginal = chain.request()
        val nuevaSolicitud = solicitudOriginal.newBuilder()
            .header("Content-Type", "application/json")
            .apply {
                if (CLAVE_API_IA.isNotBlank() && CLAVE_API_IA != "TU_API_KEY_AQUI") {
                    header("Authorization", if (CLAVE_API_IA.startsWith("Bearer ")) CLAVE_API_IA else "Bearer $CLAVE_API_IA")
                }
            }
            .build()
        chain.proceed(nuevaSolicitud)
    }

    private val interceptorLogging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(interceptorAutenticacion)
            .addInterceptor(interceptorLogging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val servicio: ServicioApiIa by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE_IA)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServicioApiIa::class.java)
    }
}
