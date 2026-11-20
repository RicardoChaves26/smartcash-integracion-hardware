package com.example.smartcash.util

import com.example.smartcash.R

object Categorias {

    val todas = listOf(
        "Comida",
        "Transporte",
        "Servicios",
        "Salario",
        "Entretenimiento",
        "Otros"
    )

    fun icono(nombre: String): Int {
        return when (nombre) {
            "Comida" -> R.drawable.ic_comida
            "Transporte" -> R.drawable.ic_transporte
            "Servicios" -> R.drawable.ic_servicios
            "Salario" -> R.drawable.ic_salario
            "Entretenimiento" -> R.drawable.ic_entretenimiento
            else -> R.drawable.ic_otros
        }
    }
}
