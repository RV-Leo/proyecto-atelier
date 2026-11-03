package com.tecsup.atelier.data.network

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit del catálogo.
 *
 * En el emulador de Android el host de la computadora es `10.0.2.2`.
 * Para un celular físico cambia [URL_BASE] por la IP de tu equipo en la red,
 * por ejemplo `http://192.168.1.10:8080/api/v1/`.
 *
 * Las credenciales son las del perfil de desarrollo del backend y se pueden
 * cambiar desde `application-local.properties` en el servidor.
 */
object ClienteApiAtelier {

    const val URL_BASE = "http://10.0.2.2:8080/api/v1/"
    const val USUARIO = "atelier"
    const val CLAVE = "atelier-dev"

    private const val TIEMPO_ESPERA_SEGUNDOS = 10L

    fun crearServicio(
        urlBase: String = URL_BASE,
        usuario: String = USUARIO,
        clave: String = CLAVE
    ): ServicioApiAtelier {
        val autenticacion = okhttp3.Interceptor { chain ->
            val solicitud = chain.request().newBuilder()
                .header("Authorization", Credentials.basic(usuario, clave))
                .header("Accept", "application/json")
                .build()
            chain.proceed(solicitud)
        }

        val registro = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val cliente = OkHttpClient.Builder()
            .addInterceptor(autenticacion)
            .addInterceptor(registro)
            .connectTimeout(TIEMPO_ESPERA_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIEMPO_ESPERA_SEGUNDOS, TimeUnit.SECONDS)
            .writeTimeout(TIEMPO_ESPERA_SEGUNDOS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(urlBase)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServicioApiAtelier::class.java)
    }

    val servicio: ServicioApiAtelier by lazy { crearServicio() }
}
