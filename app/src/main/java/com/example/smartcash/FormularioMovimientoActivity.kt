
package com.example.smartcash

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcash.data.MovimientosRepository
import com.example.smartcash.models.Movimiento
import com.example.smartcash.util.Categorias
import com.example.smartcash.util.Fechas
import com.example.smartcash.util.Ventana

class FormularioMovimientoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MOVIMIENTO_ID = "movimientoId"
        const val EXTRA_TIPO = "tipo"
    }

    private lateinit var repositorio: MovimientosRepository

    private lateinit var etNombre: EditText
    private lateinit var etMonto: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var etFecha: EditText
    private lateinit var etNota: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnGasto: Button
    private lateinit var btnIngreso: Button

    private var movimientoId = -1
    private var tipoSeleccionado = ""
    private var guardandoMovimiento = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_formulario_movimiento)
        Ventana.ajustarMargenes(findViewById(R.id.main))

        findViewById<ImageButton>(R.id.btnAtras)
            .setOnClickListener { finish() }

        findViewById<TextView>(R.id.txtTituloBarra).text =
            "Formulario"

        repositorio = MovimientosRepository(this)

        etNombre = findViewById(R.id.etNombre)
        etMonto = findViewById(R.id.etMonto)
        spCategoria = findViewById(R.id.spCategoria)
        etFecha = findViewById(R.id.etFecha)
        etNota = findViewById(R.id.etNota)

        btnGuardar = findViewById(R.id.btnGuardar)
        btnGasto = findViewById(R.id.btnGasto)
        btnIngreso = findViewById(R.id.btnIngreso)

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            Categorias.todas
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCategoria.adapter = adaptador

        movimientoId = intent.getIntExtra(
            EXTRA_MOVIMIENTO_ID, -1
        )

        btnGasto.setOnClickListener {
            seleccionarTipo("Gasto")
        }

        btnIngreso.setOnClickListener {
            seleccionarTipo("Ingreso")
        }

        if (movimientoId > 0) {
            habilitarEdicion()

            findViewById<TextView>(
                R.id.txtTituloBarra
            ).text = "Editar movimiento"

            btnGuardar.text = "Guardar cambios"

            cargarMovimiento()

        } else {

            findViewById<TextView>(
                R.id.txtTituloBarra
            ).text = "Nuevo movimiento"

            btnGuardar.text = "Guardar movimiento"

            etFecha.setText(Fechas.hoy())

            val tipo = intent.getStringExtra(EXTRA_TIPO)

            if (tipo == "Gasto" || tipo == "Ingreso") {
                seleccionarTipo(tipo)
            }
        }

        btnGuardar.setOnClickListener {
            guardarMovimiento()
        }
    }

    private fun habilitarEdicion() {
        listOf(etNombre, etMonto, etFecha, etNota).forEach { campo ->
            campo.isEnabled = true
            campo.isFocusable = true
            campo.isFocusableInTouchMode = true
            campo.isClickable = true
        }

        spCategoria.isEnabled = true
        spCategoria.isClickable = true
        btnGasto.isEnabled = true
        btnIngreso.isEnabled = true
        btnGuardar.isEnabled = true
    }

    private fun seleccionarTipo(tipo: String) {

        tipoSeleccionado = tipo

        btnGasto.alpha =
            if (tipo == "Gasto") 1f else 0.5f

        btnIngreso.alpha =
            if (tipo == "Ingreso") 1f else 0.5f
    }

    private fun cargarMovimiento() {

        val movimiento =
            repositorio.obtenerPorId(movimientoId)

        if (movimiento == null) {
            mostrarMensaje("Movimiento no encontrado")
            finish()
            return
        }

        etNombre.setText(movimiento.nombre)
        etMonto.setText(movimiento.monto.toString())
        etFecha.setText(Fechas.aMostrar(movimiento.fecha))
        etNota.setText(movimiento.nota)

        seleccionarTipo(movimiento.tipo)

        val posicion = Categorias.todas.indexOf(
            movimiento.categoria
        )

        if (posicion >= 0) {
            spCategoria.setSelection(posicion)
        }
    }

    private fun guardarMovimiento() {
        if (guardandoMovimiento) {
            return
        }

        val nombre = etNombre.text.toString().trim()

        val monto = etMonto.text.toString()
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()

        val fecha = etFecha.text.toString().trim()

        val nota = etNota.text.toString().trim()

        if (tipoSeleccionado.isEmpty()) {
            mostrarMensaje("Seleccione Gasto o Ingreso")
            return
        }

        if (nombre.isEmpty()) {
            mostrarMensaje("Ingrese el nombre")
            return
        }

        if (monto == null || !monto.isFinite() || monto <= 0) {
            mostrarMensaje("Ingrese un monto válido")
            return
        }

        if (spCategoria.selectedItem == null) {
            mostrarMensaje("Seleccione una categoría")
            return
        }

        if (!Fechas.esValida(fecha)) {
            mostrarMensaje("Fecha inválida. Use dd/MM/yyyy")
            return
        }

        val movimiento = Movimiento(
            id = if (movimientoId > 0) movimientoId else 0,
            nombre = nombre,
            tipo = tipoSeleccionado,
            monto = monto,
            categoria = spCategoria.selectedItem.toString(),
            fecha = Fechas.aIso(fecha),
            nota = nota
        )

        if (movimientoId > 0) {
            actualizarMovimiento(movimiento)
        } else {
            agregarMovimiento(movimiento)
        }
    }

    private fun agregarMovimiento(movimiento: Movimiento) {
        if (guardandoMovimiento) {
            return
        }

        guardandoMovimiento = true
        btnGuardar.isEnabled = false
        val resultado = repositorio.agregar(movimiento)

        if (resultado > 0L) {

            mostrarMensaje(
                "Movimiento guardado correctamente"
            )

            setResult(RESULT_OK)
            finish()

        } else {
            guardandoMovimiento = false
            btnGuardar.isEnabled = true

            mostrarMensaje(
                "Error al guardar movimiento"
            )
        }
    }

    private fun actualizarMovimiento(movimiento: Movimiento) {
        if (guardandoMovimiento) {
            return
        }

        guardandoMovimiento = true
        btnGuardar.isEnabled = false
        val resultado = repositorio.actualizar(movimiento)

        if (resultado > 0) {

            mostrarMensaje(
                "Movimiento actualizado correctamente"
            )

            setResult(RESULT_OK)
            finish()

        } else {
            guardandoMovimiento = false
            btnGuardar.isEnabled = true

            mostrarMensaje(
                "Error al actualizar movimiento"
            )
        }
    }

    private fun mostrarMensaje(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }
}
