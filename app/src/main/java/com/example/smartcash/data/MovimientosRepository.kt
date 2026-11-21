package com.example.smartcash.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.util.Log
import com.example.smartcash.database.SmartCashDbHelper
import com.example.smartcash.models.Movimiento
import com.example.smartcash.util.ComprobanteStorage
import java.io.File

class MovimientosRepository(context: Context) {

    private val dbHelper = SmartCashDbHelper(context)

    private val storage = ComprobanteStorage(context)

    companion object {
        private const val TAG = "MovimientosRepository"
    }

    private fun leerMovimiento(cursor: Cursor): Movimiento {
        return Movimiento(

            id = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                    SmartCashDbHelper.COL_ID
                )
            ),

            nombre = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    SmartCashDbHelper.COL_NOMBRE
                )
            ),

            tipo = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    SmartCashDbHelper.COL_TIPO
                )
            ),

            monto = cursor.getDouble(
                cursor.getColumnIndexOrThrow(
                    SmartCashDbHelper.COL_MONTO
                )
            ),

            categoria = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    SmartCashDbHelper.COL_CATEGORIA
                )
            ),

            fecha = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    SmartCashDbHelper.COL_FECHA
                )
            ),

            nota = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    SmartCashDbHelper.COL_NOTA
                )
            ) ?: "",

            rutaComprobante = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    SmartCashDbHelper.COL_RUTA_COMPROBANTE
                )
            )
        )
    }

    private fun valoresDe(
        movimiento: Movimiento
    ): ContentValues {
        return ContentValues().apply {

            put(
                SmartCashDbHelper.COL_NOMBRE,
                movimiento.nombre
            )

            put(
                SmartCashDbHelper.COL_TIPO,
                movimiento.tipo
            )

            put(
                SmartCashDbHelper.COL_MONTO,
                movimiento.monto
            )

            put(
                SmartCashDbHelper.COL_CATEGORIA,
                movimiento.categoria
            )

            put(
                SmartCashDbHelper.COL_FECHA,
                movimiento.fecha
            )

            put(
                SmartCashDbHelper.COL_NOTA,
                movimiento.nota
            )

            put(
                SmartCashDbHelper.COL_RUTA_COMPROBANTE,
                movimiento.rutaComprobante
            )
        }
    }

    fun obtenerPorId(id: Int): Movimiento? {
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            null,
            "${SmartCashDbHelper.COL_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                leerMovimiento(it)
            } else {
                null
            }
        }
    }

    fun agregar(movimiento: Movimiento): Long {

        if (
            movimiento.rutaComprobante != null &&
            !storage.existeComprobante(
                movimiento.rutaComprobante
            )
        ) {
            return -1L
        }

        return try {
            val db = dbHelper.writableDatabase
            db.insert(
                SmartCashDbHelper.TABLE_MOVIMIENTOS,
                null,
                valoresDe(movimiento)
            )

        } catch (e: SQLiteException) {
            Log.e(TAG, "Error al agregar movimiento", e)
            -1L
        }
    }

    fun agregarConComprobante(
        movimiento: Movimiento,
        archivoTemporal: File
    ): Long {

        val nuevaRuta =
            storage.guardarComprobante(archivoTemporal)
                ?: return -1L

        val resultado = agregar(
            movimiento.copy(
                rutaComprobante = nuevaRuta
            )
        )
        if (resultado > 0L) {
            storage.eliminarTemporal(archivoTemporal)
        } else {
            eliminarSiNoTieneReferencias(nuevaRuta)
        }
        return resultado
    }

    fun actualizar(movimiento: Movimiento): Int {
        return actualizarDatos(
            movimiento,
            cambiarComprobante = false
        )
    }

    fun actualizarConComprobante(
        movimiento: Movimiento,
        archivoTemporal: File
    ): Int {

        if (movimiento.id <= 0) {
            return 0
        }

        val nuevaRuta =
            storage.guardarComprobante(archivoTemporal)
                ?: return 0

        val resultado = actualizarDatos(

            movimiento.copy(
                rutaComprobante = nuevaRuta
            ),
            cambiarComprobante = true
        )
        if (resultado > 0) {
            storage.eliminarTemporal(archivoTemporal)
        } else {
            eliminarSiNoTieneReferencias(nuevaRuta)
        }
        return resultado
    }

    fun actualizarSinComprobante(
        movimiento: Movimiento
    ): Int {

        return actualizarDatos(
            movimiento.copy(
                rutaComprobante = null
            ),
            cambiarComprobante = true
        )
    }

    private fun actualizarDatos(
        movimiento: Movimiento,
        cambiarComprobante: Boolean
    ): Int {

        if (movimiento.id <= 0) {
            return 0
        }

        if (
            cambiarComprobante &&
            movimiento.rutaComprobante != null &&
            !storage.existeComprobante(
                movimiento.rutaComprobante
            )
        ) {
            return 0
        }

        var rutaAnterior: String? = null
        val resultado = try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()

            try {
                val anterior = obtenerPorId(
                    movimiento.id
                ) ?: return 0
                rutaAnterior = anterior.rutaComprobante

                val movimientoFinal =
                    if (cambiarComprobante) {
                        movimiento
                    } else {
                        movimiento.copy(
                            rutaComprobante =
                                anterior.rutaComprobante
                        )
                    }

                val filas = db.update(

                    SmartCashDbHelper.TABLE_MOVIMIENTOS,

                    valoresDe(movimientoFinal),

                    "${SmartCashDbHelper.COL_ID} = ?",

                    arrayOf(
                        movimiento.id.toString()
                    )
                )
                db.setTransactionSuccessful()
                filas
            } finally {
                db.endTransaction()
            }

        } catch (e: SQLiteException) {

            Log.e(TAG, "Error al actualizar movimiento", e)

            0
        }

        if (
            resultado > 0 &&
            cambiarComprobante &&
            rutaAnterior != movimiento.rutaComprobante
        ) {
            eliminarSiNoTieneReferencias(rutaAnterior)
        }
        return resultado
    }

    fun eliminar(id: Int): Int {
        if (id <= 0) {
            return 0
        }
        var rutaComprobante: String? = null
        val resultado = try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                val movimiento = obtenerPorId(id)
                    ?: return 0

                rutaComprobante =
                    movimiento.rutaComprobante

                val filas = db.delete(
                    SmartCashDbHelper.TABLE_MOVIMIENTOS,
                    "${SmartCashDbHelper.COL_ID} = ?",
                    arrayOf(id.toString())
                )
                db.setTransactionSuccessful()
                filas
            } finally {

                db.endTransaction()
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error al eliminar movimiento", e)
            0
        }

        if (resultado > 0) {
            eliminarSiNoTieneReferencias(
                rutaComprobante
            )
        }
        return resultado
    }

    private fun eliminarSiNoTieneReferencias(
        ruta: String?
    ) {
        if (ruta.isNullOrBlank()) {
            return
        }
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.query(

                SmartCashDbHelper.TABLE_MOVIMIENTOS,

                arrayOf(SmartCashDbHelper.COL_ID),

                "${SmartCashDbHelper.COL_RUTA_COMPROBANTE} = ?",

                arrayOf(ruta),

                null,
                null,
                null,
                "1"
            )
            val tieneReferencias = cursor.use {

                it.moveToFirst()
            }
            if (
                !tieneReferencias &&
                !storage.eliminarComprobante(ruta)
            ) {
                Log.w(
                    TAG,
                    "No se pudo eliminar el archivo: $ruta"
                )
            }
        } catch (e: SQLiteException) {
            Log.e(
                TAG,
                "Error al comprobar referencias",
                e
            )
        }
    }

    fun obtenerRecientes(limite: Int): List<Movimiento> {
        if (limite <= 0) {
            return emptyList()
        }
        val movimientos = mutableListOf<Movimiento>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(

            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            null,
            null,
            null,
            null,
            null,

            "${SmartCashDbHelper.COL_FECHA} DESC, " +
                    "${SmartCashDbHelper.COL_ID} DESC",

            limite.toString()
        )

        cursor.use {
            while (it.moveToNext()) {
                movimientos.add(
                    leerMovimiento(it)
                )
            }
        }
        return movimientos
    }

    fun total(tipo: String): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(

            """
            SELECT COALESCE(
                SUM(${SmartCashDbHelper.COL_MONTO}), 0
            )
            FROM ${SmartCashDbHelper.TABLE_MOVIMIENTOS}
            WHERE ${SmartCashDbHelper.COL_TIPO} = ?
            """.trimIndent(),

            arrayOf(tipo)
        )

        cursor.use {
            return if (it.moveToFirst()) {
                it.getDouble(0)
            } else {
                0.0
            }
        }
    }

    fun obtenerFiltrados(
        mes: String?,
        categoria: String?
    ): List<Movimiento> {

        val movimientos = mutableListOf<Movimiento>()
        val condiciones = mutableListOf<String>()
        val argumentos = mutableListOf<String>()

        if (mes != null) {
            condiciones.add(
                "SUBSTR(${SmartCashDbHelper.COL_FECHA}, 1, 7) = ?"
            )
            argumentos.add(mes)
        }

        if (categoria != null) {
            condiciones.add(
                "${SmartCashDbHelper.COL_CATEGORIA} = ?"
            )
            argumentos.add(categoria)
        }

        val seleccion = if (condiciones.isEmpty()) {
            null
        } else {
            condiciones.joinToString(" AND ")
        }

        val seleccionArgs = if (argumentos.isEmpty()) {
            null
        } else {
            argumentos.toTypedArray()
        }

        val db = dbHelper.readableDatabase

        val cursor = db.query(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            null,
            seleccion,
            seleccionArgs,
            null,
            null,

            "${SmartCashDbHelper.COL_FECHA} DESC, " +
                    "${SmartCashDbHelper.COL_ID} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {

                movimientos.add(
                    leerMovimiento(it)
                )
            }
        }

        return movimientos
    }

    fun obtenerMeses(): List<String> {
        val meses = mutableListOf<String>()
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,

            arrayOf(
                "SUBSTR(${SmartCashDbHelper.COL_FECHA}, 1, 7) AS mes"
            ),

            null,
            null,
            "SUBSTR(${SmartCashDbHelper.COL_FECHA}, 1, 7)",
            null,
            "mes DESC"
        )

        cursor.use {
            while (it.moveToNext()) {

                meses.add(
                    it.getString(
                        it.getColumnIndexOrThrow("mes")
                    )
                )
            }
        }

        return meses
    }

    fun cerrar() {

        dbHelper.close()
    }
}