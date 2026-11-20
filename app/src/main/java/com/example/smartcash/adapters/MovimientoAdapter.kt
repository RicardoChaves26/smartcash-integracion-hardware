package com.example.smartcash.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.smartcash.R
import com.example.smartcash.models.Movimiento
import com.example.smartcash.util.Categorias
import com.example.smartcash.util.Fechas
import com.example.smartcash.util.Formato

class MovimientoAdapter(private val context: Context) : BaseAdapter() {

    private val movimientos = mutableListOf<Movimiento>()

    companion object {
        fun llenarFila(vista: View, movimiento: Movimiento) {
            val contexto = vista.context

            vista.findViewById<ImageView>(R.id.imgCategoria)
                .setImageResource(Categorias.icono(movimiento.categoria))
            vista.findViewById<TextView>(R.id.txtNombre).text = movimiento.nombre
            vista.findViewById<TextView>(R.id.txtCategoria).text =
                "${movimiento.categoria} • ${Fechas.aMostrar(movimiento.fecha)}"

            val txtMonto = vista.findViewById<TextView>(R.id.txtMonto)
            if (movimiento.tipo == "Gasto") {
                txtMonto.text = "- ${Formato.monto(movimiento.monto)}"
                txtMonto.setTextColor(ContextCompat.getColor(contexto, R.color.smartcash_gasto))
            } else {
                txtMonto.text = "+ ${Formato.monto(movimiento.monto)}"
                txtMonto.setTextColor(ContextCompat.getColor(contexto, R.color.smartcash_ingreso))
            }
        }
    }

    override fun getCount(): Int = movimientos.size

    override fun getItem(position: Int): Movimiento = movimientos[position]

    override fun getItemId(position: Int): Long = movimientos[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vista = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_movimiento, parent, false)

        llenarFila(vista, getItem(position))
        return vista
    }

    fun actualizar(nuevaLista: List<Movimiento>) {
        movimientos.clear()
        movimientos.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}
