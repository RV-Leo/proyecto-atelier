package com.tecsup.atelier.data.modelo

import com.google.gson.annotations.SerializedName

/** Solicitud común hacia el endpoint de chat del backend. */
data class SolicitudIaDto(
    @SerializedName("messages")
    val mensajes: List<MensajeIaDto>
)

/** Mensaje individual en el historial compartido entre web y móvil. */
data class MensajeIaDto(
    @SerializedName("role")
    val rol: String,
    @SerializedName("content")
    val contenido: String
)

/** Respuesta común del backend, independiente del proveedor de IA. */
data class RespuestaIaDto(
    @SerializedName("respuesta")
    val respuestaTexto: String
)
