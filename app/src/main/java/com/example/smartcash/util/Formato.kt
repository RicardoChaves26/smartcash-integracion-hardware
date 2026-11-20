package com.example.smartcash.util

import java.util.Locale
import kotlin.math.abs

object Formato {

    private const val COLON = "₡"

    fun monto(valor: Double): String {
        val signo = if (valor < 0) "-" else ""
        val absoluto = abs(valor)

        val texto = if (absoluto % 1.0 == 0.0) {
            String.format(Locale.US, "%,.0f", absoluto)
        } else {
            String.format(Locale.US, "%,.2f", absoluto)
        }

        val conFormato = texto.replace(',', '\u00A0').replace('.', ',')
        return "$signo$COLON$conFormato"
    }
}
