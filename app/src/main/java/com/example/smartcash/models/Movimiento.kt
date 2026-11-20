package com.example.smartcash.models

data class Movimiento(
    val id: Int = 0,
    val nombre: String,
    val tipo: String,
    val monto: Double,
    val categoria: String,
    val fecha: String,
    val nota: String = ""
)
