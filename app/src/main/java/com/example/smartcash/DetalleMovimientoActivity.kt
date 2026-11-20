package com.example.smartcash

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcash.data.MovimientosRepository
import com.example.smartcash.models.Movimiento
import com.example.smartcash.util.Ventana
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DetalleMovimientoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MOVIMIENTO_ID = "movimientoId"
    }

    private lateinit var repositorio: MovimientosRepository
    private var movimientoId = -1

    // Inicializa la pantalla y configura los botones.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_movimiento)
        Ventana.ajustarMargenes(findViewById(R.id.main))

        repositorio = MovimientosRepository(this)

        // Recibe el identificador del movimiento seleccionado.
        movimientoId = intent.getIntExtra(EXTRA_MOVIMIENTO_ID, -1)

        findViewById<ImageButton>(R.id.btnAtras)
            .setOnClickListener { finish() }

        findViewById<TextView>(R.id.txtTituloBarra).text = "Detalle"

        findViewById<MaterialButton>(R.id.btnEditarMovimiento)
            .setOnClickListener { editarMovimiento() }

        findViewById<MaterialButton>(R.id.btnEliminarMovimiento)
            .setOnClickListener { confirmarEliminacion() }

        if (movimientoId <= 0) {
            Toast.makeText(
                this,
                "Movimiento no válido",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    // Recarga los datos al abrir o regresar a la pantalla.
    override fun onResume() {
        super.onResume()

        if (movimientoId > 0) {
            cargarMovimiento()
        }
    }

    // Busca el movimiento seleccionado en SQLite.
    private fun cargarMovimiento() {

        val movimiento = repositorio.obtenerPorId(movimientoId)

        if (movimiento == null) {
            Toast.makeText(
                this,
                "El movimiento no existe",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        mostrarMovimiento(movimiento)
    }

    // Muestra la información del movimiento en pantalla.
    private fun mostrarMovimiento(movimiento: Movimiento) {

        findViewById<TextView>(R.id.txtDetalleNombre).text =
            movimiento.nombre

        findViewById<TextView>(R.id.txtDetalleTipo).text =
            movimiento.tipo

        findViewById<TextView>(R.id.txtDetalleCategoria).text =
            movimiento.categoria

        findViewById<TextView>(R.id.txtDetalleNota).text =
            movimiento.nota.ifBlank { "Sin nota" }

        // Convierte la fecha guardada a formato de pantalla.
        val fechaPantalla = try {
            val formatoBase = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )
            formatoBase.isLenient = false

            val formatoVista = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            val fecha = formatoBase.parse(movimiento.fecha)
            if (fecha != null) {
                formatoVista.format(fecha)
            } else {
                movimiento.fecha
            }
        } catch (e: Exception) {
            movimiento.fecha
        }

        findViewById<TextView>(R.id.txtDetalleFecha).text =
            fechaPantalla

        // Formatea el monto en colones.
        val formatoMonto = NumberFormat.getNumberInstance(
            Locale("es", "CR")
        )
        formatoMonto.maximumFractionDigits = 2
        formatoMonto.minimumFractionDigits = 0

        val esGasto = movimiento.tipo.equals(
            "Gasto",
            ignoreCase = true
        )

        val montoTexto = if (esGasto) {
            "-₡${formatoMonto.format(movimiento.monto)}"
        } else {
            "+₡${formatoMonto.format(movimiento.monto)}"
        }

        val txtMonto = findViewById<TextView>(R.id.txtDetalleMonto)
        txtMonto.text = montoTexto

        txtMonto.setTextColor(
            getColor(
                if (esGasto) {
                    R.color.smartcash_gasto
                } else {
                    R.color.smartcash_ingreso
                }
            )
        )

        // Muestra el icono según la categoría.
        val icono = when (movimiento.categoria) {
            "Comida" -> R.drawable.ic_comida
            "Transporte" -> R.drawable.ic_transporte
            "Servicios" -> R.drawable.ic_servicios
            "Salario" -> R.drawable.ic_salario
            "Entretenimiento" -> R.drawable.ic_entretenimiento
            else -> R.drawable.ic_otros
        }

        findViewById<ImageView>(R.id.imgDetalleCategoria)
            .setImageResource(icono)
    }

    // Abre el formulario de edición con el mismo ID.
    private fun editarMovimiento() {

        val intentEditar = Intent(
            this,
            FormularioMovimientoActivity::class.java
        )

        intentEditar.putExtra(
            FormularioMovimientoActivity.EXTRA_MOVIMIENTO_ID,
            movimientoId
        )

        startActivity(intentEditar)
    }

    // Solicita confirmación antes de eliminar.
    private fun confirmarEliminacion() {

        AlertDialog.Builder(this)
            .setTitle("Eliminar movimiento")
            .setMessage(
                "¿Estás seguro de que deseas eliminar este movimiento?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarMovimiento()
            }
            .show()
    }

    // Elimina el movimiento seleccionado de SQLite.
    private fun eliminarMovimiento() {

        val filasEliminadas = repositorio.eliminar(movimientoId)

        if (filasEliminadas > 0) {

            Toast.makeText(
                this,
                "Movimiento eliminado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            finish()

        } else {

            Toast.makeText(
                this,
                "No se pudo eliminar el movimiento",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Cierra el acceso a SQLite al destruir la Activity.
    override fun onDestroy() {
        super.onDestroy()
        if (::repositorio.isInitialized) {
            repositorio.cerrar()
        }
    }
}
