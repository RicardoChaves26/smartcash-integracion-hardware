package com.example.smartcash.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Fechas {

    private const val FORMATO_PANTALLA = "dd/MM/yyyy"

    private val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    fun esValida(texto: String): Boolean {
        val limpio = texto.trim()
        if (!Regex("""\d{2}/\d{2}/\d{4}""").matches(limpio)) return false

        return try {
            val formato = SimpleDateFormat(FORMATO_PANTALLA, Locale.US)
            formato.isLenient = false
            formato.parse(limpio) != null
        } catch (e: Exception) {
            false
        }
    }

    fun aIso(texto: String): String {
        val partes = texto.trim().split("/")
        return "${partes[2]}-${partes[1]}-${partes[0]}"
    }

    fun aMostrar(iso: String): String {
        val partes = iso.split("-")
        if (partes.size != 3) return iso
        return "${partes[2]}/${partes[1]}/${partes[0]}"
    }

    fun nombreMes(anioMes: String): String {
        val partes = anioMes.split("-")
        if (partes.size != 2) return anioMes
        val numero = partes[1].toIntOrNull() ?: return anioMes
        if (numero !in 1..12) return anioMes
        return "${meses[numero - 1]} ${partes[0]}"
    }

    fun hoy(): String {
        return SimpleDateFormat(FORMATO_PANTALLA, Locale.US).format(Date())
    }
}
