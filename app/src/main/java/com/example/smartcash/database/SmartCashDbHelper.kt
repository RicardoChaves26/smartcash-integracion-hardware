package com.example.smartcash.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SmartCashDbHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {

    companion object {

        const val DATABASE_NAME = "SmartCash.db"

        const val DATABASE_VERSION = 2

        const val TABLE_MOVIMIENTOS = "movimientos"

        const val COL_ID = "id"
        const val COL_NOMBRE = "nombre"
        const val COL_TIPO = "tipo"
        const val COL_MONTO = "monto"
        const val COL_CATEGORIA = "categoria"
        const val COL_FECHA = "fecha"
        const val COL_NOTA = "nota"
        const val COL_RUTA_COMPROBANTE = "ruta_comprobante"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_MOVIMIENTOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_TIPO TEXT NOT NULL,
                $COL_MONTO REAL NOT NULL,
                $COL_CATEGORIA TEXT NOT NULL,
                $COL_FECHA TEXT NOT NULL,
                $COL_NOTA TEXT,
                $COL_RUTA_COMPROBANTE TEXT
            )
        """.trimIndent()

        db.execSQL(createTable)
        insertarDatosEjemplo(db)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 2) {

            db.execSQL(
                """
                ALTER TABLE $TABLE_MOVIMIENTOS
                ADD COLUMN $COL_RUTA_COMPROBANTE TEXT
                """.trimIndent()
            )
        }
    }

    private fun insertarDatosEjemplo(db: SQLiteDatabase) {

        insertar(db, "Almuerzo en la soda", "Gasto", 3500.0, "Comida", "2026-10-28", "Casado con pollo")

        insertar(db, "Compra de supermercado", "Gasto", 28500.0, "Comida", "2026-10-30", "Compras de la semana")

        insertar(db, "Bus a la universidad", "Gasto", 1200.0, "Transporte", "2026-10-31", "")

        insertar(db, "Gasolina", "Gasto", 15000.0, "Transporte", "2026-11-02", "Tanque medio")

        insertar(db, "Pago de electricidad", "Gasto", 22000.0, "Servicios", "2026-11-03", "Recibo de octubre")

        insertar(db, "Pago de internet", "Gasto", 18500.0, "Servicios", "2026-11-03", "")

        insertar(db, "Pago de salario", "Ingreso", 450000.0, "Salario", "2026-11-05", "Quincena")

        insertar(db, "Trabajo freelance", "Ingreso", 60000.0, "Salario", "2026-11-06", "Diseño de logo")

        insertar(db, "Cine con amigos", "Gasto", 6000.0, "Entretenimiento", "2026-11-07", "")

        insertar(db, "Suscripción de streaming", "Gasto", 5500.0, "Entretenimiento", "2026-11-08", "Plan mensual")
    }

    private fun insertar(
        db: SQLiteDatabase,
        nombre: String,
        tipo: String,
        monto: Double,
        categoria: String,
        fecha: String,
        nota: String
    ) {
        val values = ContentValues().apply {
            put(COL_NOMBRE, nombre)
            put(COL_TIPO, tipo)
            put(COL_MONTO, monto)
            put(COL_CATEGORIA, categoria)
            put(COL_FECHA, fecha)
            put(COL_NOTA, nota)
            putNull(COL_RUTA_COMPROBANTE)
        }
        db.insert(TABLE_MOVIMIENTOS, null, values)
    }
}