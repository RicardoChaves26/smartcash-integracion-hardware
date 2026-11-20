
package com.example.smartcash.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smartcash.DetalleMovimientoActivity
import com.example.smartcash.R
import com.example.smartcash.adapters.MovimientoAdapter
import com.example.smartcash.data.MovimientosRepository
import com.example.smartcash.util.Categorias
import com.example.smartcash.util.Fechas

class HistorialFragment : Fragment(R.layout.fragment_historial) {

    private lateinit var repositorio: MovimientosRepository
    private lateinit var adaptadorMovimientos: MovimientoAdapter

    private lateinit var spinnerMes: Spinner
    private lateinit var spinnerCategoria: Spinner
    private lateinit var listaMovimientos: ListView
    private lateinit var txtSinResultados: TextView

    private var mesesDisponibles = emptyList<String>()
    private var mesAplicado: String? = null
    private var categoriaAplicada: String? = null

    // Conecta los componentes del XML con Kotlin.
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        repositorio = MovimientosRepository(requireContext())
        adaptadorMovimientos = MovimientoAdapter(requireContext())

        spinnerMes = view.findViewById(R.id.spinnerMes)
        spinnerCategoria = view.findViewById(R.id.spinnerCategoria)
        listaMovimientos = view.findViewById(R.id.listaMovimientos)
        txtSinResultados = view.findViewById(R.id.txtSinResultados)

        val btnAplicar = view.findViewById<Button>(R.id.btnAplicarFiltros)
        val btnLimpiar = view.findViewById<Button>(R.id.btnLimpiarFiltros)

        listaMovimientos.adapter = adaptadorMovimientos

        configurarCategorias()

        // Aplica los filtros seleccionados.
        btnAplicar.setOnClickListener {
            val posicionMes = spinnerMes.selectedItemPosition
            val posicionCategoria = spinnerCategoria.selectedItemPosition

            mesAplicado = if (posicionMes == 0) {
                null
            } else {
                mesesDisponibles[posicionMes - 1]
            }

            categoriaAplicada = if (posicionCategoria == 0) {
                null
            } else {
                Categorias.todas[posicionCategoria - 1]
            }

            cargarMovimientos()
        }

        // Limpia los filtros y muestra todos los movimientos.
        btnLimpiar.setOnClickListener {
            spinnerMes.setSelection(0)
            spinnerCategoria.setSelection(0)

            mesAplicado = null
            categoriaAplicada = null

            cargarMovimientos()
        }

        // Abre el detalle del movimiento seleccionado.
        listaMovimientos.setOnItemClickListener { _, _, posicion, _ ->
            val movimiento = adaptadorMovimientos.getItem(posicion)

            val intent = Intent(
                requireContext(),
                DetalleMovimientoActivity::class.java
            )

            intent.putExtra(
                DetalleMovimientoActivity.EXTRA_MOVIMIENTO_ID,
                movimiento.id
            )

            startActivity(intent)
        }
    }

    // Carga nuevamente los datos al volver a la pantalla.
    override fun onResume() {
        super.onResume()
        cargarMeses()
        cargarMovimientos()
    }

    // Llena el Spinner con los meses de SQLite.
    private fun cargarMeses() {
        mesesDisponibles = repositorio.obtenerMeses()

        val opciones = mutableListOf(
            getString(R.string.historial_todos_meses)
        )

        for (mes in mesesDisponibles) {
            opciones.add(Fechas.nombreMes(mes))
        }

        val adaptador = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            opciones
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerMes.adapter = adaptador

        val mesActual = mesAplicado

        val indice = if (mesActual != null) {
            mesesDisponibles.indexOf(mesActual)
        } else {
            -1
        }

        if (indice >= 0) {
            spinnerMes.setSelection(indice + 1)
        } else {
            spinnerMes.setSelection(0)
            mesAplicado = null
        }
    }

    // Llena el Spinner con las categorias disponibles.
    private fun configurarCategorias() {
        val opciones = listOf(
            getString(R.string.historial_todas_categorias)
        ) + Categorias.todas

        val adaptador = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            opciones
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerCategoria.adapter = adaptador

        val categoriaActual = categoriaAplicada

        val indice = if (categoriaActual != null) {
            Categorias.todas.indexOf(categoriaActual)
        } else {
            -1
        }

        spinnerCategoria.setSelection(
            if (indice >= 0) indice + 1 else 0
        )
    }

    // Consulta y muestra los movimientos segun los filtros.
    private fun cargarMovimientos() {
        val movimientos = repositorio.obtenerFiltrados(
            mesAplicado,
            categoriaAplicada
        )

        adaptadorMovimientos.actualizar(movimientos)

        if (movimientos.isEmpty()) {
            listaMovimientos.visibility = View.GONE
            txtSinResultados.visibility = View.VISIBLE
        } else {
            listaMovimientos.visibility = View.VISIBLE
            txtSinResultados.visibility = View.GONE
        }
    }

    // Cierra el acceso a la base de datos.
    override fun onDestroyView() {
        super.onDestroyView()
        repositorio.cerrar()
    }
}

