package com.tecsup.atelier.data.modelo

import com.google.gson.annotations.SerializedName

/**
 * Estructura DTO de solicitud para el modelo de IA.
 * Modifica o adapta las propiedades según el formato que requiera tu proveedor o servidor dedicado.
 */
data class SolicitudIaDto(
    @SerializedName("modelo")
    val modelo: String = "modelo-predeterminado",
    
    @SerializedName("prompt")
    val prompt: String,
    
    @SerializedName("mensajes")
    val mensajes: List<MensajeIaDto>? = null,
    
    @SerializedName("temperatura")
    val temperatura: Double = 0.7,
    
    @SerializedName("max_tokens")
    val maxTokens: Int = 500
)

/**
 * Mensaje individual dentro del historial de chat para servidores de IA.
 */
data class MensajeIaDto(
    @SerializedName("rol")
    val rol: String, // "user", "assistant" o "system"
    
    @SerializedName("contenido")
    val contenido: String
)

/**
 * Estructura DTO de respuesta proveniente de la API del modelo de IA.
 */
data class RespuestaIaDto(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("respuesta")
    val respuestaTexto: String? = null,
    
    @SerializedName("opciones")
    val opciones: List<OpcionIaDto>? = null,
    
    @SerializedName("error")
    val error: String? = null
)

data class OpcionIaDto(
    @SerializedName("mensaje")
    val mensaje: MensajeIaDto? = null,
    
    @SerializedName("texto")
    val texto: String? = null
)
