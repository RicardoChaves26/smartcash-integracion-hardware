package com.example.smartcash.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smartcash.FormularioMovimientoActivity
import com.example.smartcash.R
import com.example.smartcash.adapters.MovimientoAdapter
import com.example.smartcash.database.SmartCashDbHelper
import com.example.smartcash.models.Movimiento
import com.example.smartcash.util.Formato

class InicioFragment : Fragment(R.layout.fragment_inicio) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnRegistrarGasto = view.findViewById<Button>(R.id.btnRegistrarGasto)
        val btnRegistrarIngreso = view.findViewById<Button>(R.id.btnRegistrarIngreso)

        btnRegistrarGasto.setOnClickListener {
            val intent = Intent(requireContext(), FormularioMovimientoActivity::class.java)
            intent.putExtra(FormularioMovimientoActivity.EXTRA_TIPO, "Gasto")
            startActivity(intent)
        }

        btnRegistrarIngreso.setOnClickListener {
            val intent = Intent(requireContext(), FormularioMovimientoActivity::class.java)
            intent.putExtra(FormularioMovimientoActivity.EXTRA_TIPO, "Ingreso")
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        val view = view ?: return
        val dbHelper = SmartCashDbHelper(requireContext())
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            SmartCashDbHelper.TABLE_MOVIMIENTOS,
            null,
            null,
            null,
            null,
            null,
            SmartCashDbHelper.COL_ID + " DESC"
        )

        var totalIngresos = 0.0
        var totalGastos = 0.0
        val topMovimientos = ArrayList<Movimiento>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_ID))
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_NOMBRE))
            val tipo = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_TIPO))
            val monto = cursor.getDouble(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_MONTO))
            val categoria = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_CATEGORIA))
            val fecha = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_FECHA))
            val nota = cursor.getString(cursor.getColumnIndexOrThrow(SmartCashDbHelper.COL_NOTA))

            val mov = Movimiento(id, nombre, tipo, monto, categoria, fecha, nota)

            if (tipo == "Ingreso") {
                totalIngresos += monto
            } else {
                totalGastos += monto
            }

            if (topMovimientos.size < 4) {
                topMovimientos.add(mov)
            }
        }
        cursor.close()

        val saldoDisponible = totalIngresos - totalGastos

        val txtSaldoDisponible = view.findViewById<TextView>(R.id.txtSaldoDisponible)
        val txtIngresosTotal = view.findViewById<TextView>(R.id.txtIngresosTotal)
        val txtGastosTotal = view.findViewById<TextView>(R.id.txtGastosTotal)
        val contenedorMovimientos = view.findViewById<LinearLayout>(R.id.contenedorMovimientos)

        txtSaldoDisponible.text = Formato.monto(saldoDisponible)
        txtIngresosTotal.text = "+" + Formato.monto(totalIngresos)
        txtGastosTotal.text = "-" + Formato.monto(totalGastos)

        contenedorMovimientos.removeAllViews()
        for (mov in topMovimientos) {
            val itemView = layoutInflater.inflate(R.layout.item_movimiento, contenedorMovimientos, false)
            MovimientoAdapter.llenarFila(itemView, mov)
            contenedorMovimientos.addView(itemView)
        }
    }
}
