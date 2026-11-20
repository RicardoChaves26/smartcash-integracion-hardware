package com.example.smartcash.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.smartcash.database.SmartCashDbHelper
import com.example.smartcash.models.Movimiento

class MovimientosRepository(context: Context) {

    private val dbHelper = SmartCashDbHelper(context)

    private fun leerMovimiento(cursor: Cursor): Movimiento {
        return Movimiento(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_ID)),
            nombre = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_NOMBRE)),
            tipo = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_TIPO)),
            monto = cursor.getDouble(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_MONTO)),
            categoria = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_CATEGORIA)),
            fecha = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_FECHA)),
            nota = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_NOTA)) ?: ""
        )
    }

    private fun valoresDe(movimiento: Movimiento): ContentValues {
        return ContentValues().apply {
            put(SmartCashDbHelper.COL_NOMBRE, movimiento.nombre)
            put(SmartCashDbHelper.COL_TIPO, movimiento.tipo)
            put(SmartCashDbHelper.COL_MONTO, movimiento.monto)
            put(SmartCashDbHelper.COL_CATEGORIA, movimiento.categoria)
            put(SmartCashDbHelper.COL_FECHA, movimiento.fecha)
            put(SmartCashDbHelper.COL_NOTA, movimiento.nota)
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
            return if (it.moveToFirst()) leerMovimiento(it) else null
        }
    }

    fun agregar(movimiento: Movimiento): Long {
        val db = dbHelper.writableDatabase

        return db.insert(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            null,
            valoresDe(movimiento)
        )
    }

    fun actualizar(movimiento: Movimiento): Int {
        if (movimiento.id <= 0) {
            return 0
        }

        val db = dbHelper.writableDatabase

        return db.update(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            valoresDe(movimiento),
            "${SmartCashDbHelper.COL_ID} = ?",
            arrayOf(movimiento.id.toString())
        )
    }

    fun eliminar(id: Int): Int {
        if (id <= 0) {
            return 0
        }

        val db = dbHelper.writableDatabase

        return db.delete(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            "${SmartCashDbHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun obtenerRecientes(limite: Int): List<Movimiento> {
        return emptyList() // TODO persona 2
    }

    fun total(tipo: String): Double {
        return 0.0 // TODO persona 2
    }


    // Obtiene movimientos filtrados por mes y categoria.
    fun obtenerFiltrados(mes: String?, categoria: String?): List<Movimiento> {

        val movimientos = mutableListOf<Movimiento>()
        val condiciones = mutableListOf<String>()
        val argumentos = mutableListOf<String>()

        // Filtrar por mes si se selecciono uno.
        if (mes != null) {
            condiciones.add(
                "SUBSTR(${SmartCashDbHelper.COL_FECHA}, 1, 7) = ?"
            )
            argumentos.add(mes)
        }

        // Filtrar por categoria si se selecciono una.
        if (categoria != null) {
            condiciones.add(
                "${SmartCashDbHelper.COL_CATEGORIA} = ?"
            )
            argumentos.add(categoria)
        }

        // Preparar las condiciones de la consulta.
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

        // Consultar la base de datos.
        val cursor = db.query(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            null,
            seleccion,
            seleccionArgs,
            null,
            null,
            "${SmartCashDbHelper.COL_FECHA} DESC, ${SmartCashDbHelper.COL_ID} DESC"
        )

        // Recorrer los resultados.
        cursor.use {
            while (it.moveToNext()) {
                movimientos.add(leerMovimiento(it))
            }
        }

        return movimientos
    }




    fun obtenerMeses(): List<String> {
        val meses = mutableListOf<String>()
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            arrayOf("SUBSTR(${SmartCashDbHelper.COL_FECHA}, 1, 7) AS mes"),
            null,
            null,
            "SUBSTR(${SmartCashDbHelper.COL_FECHA}, 1, 7)",
            null,
            "mes DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                meses.add(it.getString(it.getColumnIndexOrThrow("mes")))
            }
        }

        return meses
    }


    fun cerrar() {
        dbHelper.close()
    }
}
