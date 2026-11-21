package com.example.smartcash

import android.content.ActivityNotFoundException
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcash.data.MovimientosRepository
import com.example.smartcash.models.Movimiento
import com.example.smartcash.util.CamaraHelper
import com.example.smartcash.util.Categorias
import com.example.smartcash.util.ComprobanteStorage
import com.example.smartcash.util.Fechas
import com.example.smartcash.util.Ventana
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormularioMovimientoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MOVIMIENTO_ID = "movimientoId"
        const val EXTRA_TIPO = "tipo"

        private const val TAG = "FormularioMovimiento"
        private const val ESTADO_TEMPORAL_SELECCIONADO =
            "comprobanteTemporalSeleccionado"
        private const val ESTADO_TEMPORAL_EN_CAPTURA =
            "comprobanteTemporalEnCaptura"
        private const val ESTADO_COMPROBANTE_QUITADO =
            "comprobanteQuitado"
    }

    private lateinit var repositorio: MovimientosRepository
    private lateinit var storage: ComprobanteStorage
    private lateinit var camaraHelper: CamaraHelper

    private lateinit var etNombre: EditText
    private lateinit var etMonto: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var etFecha: EditText
    private lateinit var etNota: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnGasto: Button
    private lateinit var btnIngreso: Button
    private lateinit var btnTomarFotografia: Button
    private lateinit var imgComprobante: ImageView
    private lateinit var txtSinComprobante: TextView
    private lateinit var grupoAccionesComprobante: LinearLayout

    private var movimientoId = -1
    private var tipoSeleccionado = ""
    private var guardandoMovimiento = false
    private var rutaComprobanteOriginal: String? = null
    private var archivoTemporalSeleccionado: File? = null
    private var archivoTemporalEnCaptura: File? = null
    private var comprobanteQuitado = false

    private val tomarFotografia =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
            procesarResultadoCaptura(exito)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_formulario_movimiento)
        Ventana.ajustarMargenes(findViewById(R.id.main))

        findViewById<ImageButton>(R.id.btnAtras)
            .setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtTituloBarra).text = "Formulario"

        repositorio = MovimientosRepository(this)
        storage = ComprobanteStorage(this)
        camaraHelper = CamaraHelper(this, storage)

        etNombre = findViewById(R.id.etNombre)
        etMonto = findViewById(R.id.etMonto)
        spCategoria = findViewById(R.id.spCategoria)
        etFecha = findViewById(R.id.etFecha)
        etNota = findViewById(R.id.etNota)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnGasto = findViewById(R.id.btnGasto)
        btnIngreso = findViewById(R.id.btnIngreso)
        btnTomarFotografia = findViewById(R.id.btnTomarFotografia)
        imgComprobante = findViewById(R.id.imgComprobante)
        txtSinComprobante = findViewById(R.id.txtSinComprobante)
        grupoAccionesComprobante =
            findViewById(R.id.grupoAccionesComprobante)

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            Categorias.todas
        )
        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spCategoria.adapter = adaptador

        configurarSelectorFecha()

        movimientoId = intent.getIntExtra(EXTRA_MOVIMIENTO_ID, -1)
        btnGasto.setOnClickListener { seleccionarTipo("Gasto") }
        btnIngreso.setOnClickListener { seleccionarTipo("Ingreso") }

        if (movimientoId > 0) {
            habilitarEdicion()
            findViewById<TextView>(R.id.txtTituloBarra).text =
                "Editar movimiento"
            btnGuardar.text = "Guardar cambios"
            cargarMovimiento()
        } else {
            findViewById<TextView>(R.id.txtTituloBarra).text =
                "Nuevo movimiento"
            btnGuardar.text = "Guardar movimiento"
            etFecha.setText(Fechas.hoy())

            val tipo = intent.getStringExtra(EXTRA_TIPO)
            if (tipo == "Gasto" || tipo == "Ingreso") {
                seleccionarTipo(tipo)
            }
        }

        btnGuardar.setOnClickListener { guardarMovimiento() }
        btnTomarFotografia.setOnClickListener { abrirCamara() }
        findViewById<Button>(R.id.btnVolverTomar)
            .setOnClickListener { abrirCamara() }
        findViewById<Button>(R.id.btnQuitarFotografia)
            .setOnClickListener { quitarComprobanteSeleccionado() }

        if (savedInstanceState != null) {
            archivoTemporalSeleccionado = savedInstanceState
                .getString(ESTADO_TEMPORAL_SELECCIONADO)
                ?.let(::File)
            archivoTemporalEnCaptura = savedInstanceState
                .getString(ESTADO_TEMPORAL_EN_CAPTURA)
                ?.let(::File)
            comprobanteQuitado = savedInstanceState
                .getBoolean(ESTADO_COMPROBANTE_QUITADO)
            actualizarVistaComprobante()
        } else {
            actualizarVistaComprobante()
        }
    }

    private fun habilitarEdicion() {
        listOf(etNombre, etMonto, etNota).forEach { campo ->
            campo.isEnabled = true
            campo.isFocusable = true
            campo.isFocusableInTouchMode = true
            campo.isClickable = true
        }

        etFecha.isEnabled = true
        etFecha.isClickable = true
        etFecha.isFocusable = false

        spCategoria.isEnabled = true
        spCategoria.isClickable = true
        btnGasto.isEnabled = true
        btnIngreso.isEnabled = true
        btnGuardar.isEnabled = true
    }

    private fun configurarSelectorFecha() {
        etFecha.keyListener = null
        etFecha.isFocusable = false
        etFecha.isFocusableInTouchMode = false
        etFecha.isClickable = true
        etFecha.setOnClickListener { abrirSelectorFecha() }
    }

    private fun abrirSelectorFecha() {
        val calendario = Calendar.getInstance()
        val textoFecha = etFecha.text.toString().trim()
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        formato.isLenient = false

        if (textoFecha.isNotEmpty()) {
            try {
                val fechaInicial = formato.parse(textoFecha)
                if (fechaInicial != null) {
                    calendario.time = fechaInicial
                }
            } catch (_: Exception) {
                // Si el contenido no es válido, se conserva el día actual.
            }
        }

        DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                val seleccionada = Calendar.getInstance()
                seleccionada.set(anio, mes, dia)
                etFecha.setText(formato.format(seleccionada.time))
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = Calendar.getInstance().timeInMillis
        }.show()
    }

    private fun seleccionarTipo(tipo: String) {
        tipoSeleccionado = tipo
        btnGasto.alpha = if (tipo == "Gasto") 1f else 0.5f
        btnIngreso.alpha = if (tipo == "Ingreso") 1f else 0.5f
    }

    private fun cargarMovimiento() {
        val movimiento = repositorio.obtenerPorId(movimientoId)
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

        val posicion = Categorias.todas.indexOf(movimiento.categoria)
        if (posicion >= 0) {
            spCategoria.setSelection(posicion)
        }

        rutaComprobanteOriginal = movimiento.rutaComprobante
    }

    private fun abrirCamara() {
        val captura = camaraHelper.crearCaptura()
        if (captura == null) {
            mostrarMensaje(getString(R.string.formulario_error_camara))
            return
        }

        archivoTemporalEnCaptura = captura.archivo
        try {
            tomarFotografia.launch(captura.uri)
        } catch (e: ActivityNotFoundException) {
            manejarErrorAlAbrirCamara(e)
        } catch (e: SecurityException) {
            manejarErrorAlAbrirCamara(e)
        } catch (e: IllegalStateException) {
            manejarErrorAlAbrirCamara(e)
        }
    }

    private fun manejarErrorAlAbrirCamara(error: RuntimeException) {
        limpiarCapturaEnCurso()
        Log.e(TAG, "No se pudo iniciar la captura con la cámara", error)
        mostrarMensaje(getString(R.string.formulario_error_camara))
    }

    private fun procesarResultadoCaptura(exito: Boolean) {
        val archivoCapturado = archivoTemporalEnCaptura
        archivoTemporalEnCaptura = null

        if (!exito || archivoCapturado == null) {
            storage.eliminarTemporal(archivoCapturado)
            actualizarVistaComprobante()
            return
        }

        if (!archivoCapturado.isFile || archivoCapturado.length() == 0L) {
            storage.eliminarTemporal(archivoCapturado)
            mostrarMensaje(getString(R.string.formulario_error_captura))
            actualizarVistaComprobante()
            return
        }

        val temporalAnterior = archivoTemporalSeleccionado
        archivoTemporalSeleccionado = archivoCapturado
        comprobanteQuitado = false
        storage.eliminarTemporal(temporalAnterior)
        actualizarVistaComprobante()
    }

    private fun quitarComprobanteSeleccionado() {
        storage.eliminarTemporal(archivoTemporalSeleccionado)
        archivoTemporalSeleccionado = null
        comprobanteQuitado = rutaComprobanteOriginal != null
        actualizarVistaComprobante()
    }

    private fun actualizarVistaComprobante() {
        val archivoSeleccionado = archivoTemporalSeleccionado
            ?.takeIf { it.isFile }
        if (archivoTemporalSeleccionado != null &&
            archivoSeleccionado == null
        ) {
            storage.eliminarTemporal(archivoTemporalSeleccionado)
            archivoTemporalSeleccionado = null
        }

        val archivoExistente = if (
            archivoSeleccionado == null &&
            !comprobanteQuitado &&
            rutaComprobanteOriginal != null
        ) {
            storage.obtenerArchivo(rutaComprobanteOriginal)
        } else {
            null
        }
        val archivoVista = archivoSeleccionado ?: archivoExistente

        if (archivoVista != null) {
            imgComprobante.setImageURI(Uri.fromFile(archivoVista))
            imgComprobante.visibility = ImageView.VISIBLE
            txtSinComprobante.visibility = TextView.GONE
            grupoAccionesComprobante.visibility = LinearLayout.VISIBLE
            btnTomarFotografia.visibility = Button.GONE
        } else {
            imgComprobante.setImageDrawable(null)
            imgComprobante.visibility = ImageView.GONE
            txtSinComprobante.setText(
                if (rutaComprobanteOriginal != null &&
                    !comprobanteQuitado
                ) {
                    R.string.formulario_foto_no_disponible
                } else {
                    R.string.formulario_sin_comprobante
                }
            )
            txtSinComprobante.visibility = TextView.VISIBLE
            grupoAccionesComprobante.visibility =
                if (rutaComprobanteOriginal != null &&
                    !comprobanteQuitado
                ) {
                    LinearLayout.VISIBLE
                } else {
                    LinearLayout.GONE
                }
            btnTomarFotografia.visibility =
                if (rutaComprobanteOriginal == null ||
                    comprobanteQuitado
                ) {
                    Button.VISIBLE
                } else {
                    Button.GONE
                }
        }
    }

    private fun limpiarCapturaEnCurso() {
        storage.eliminarTemporal(archivoTemporalEnCaptura)
        archivoTemporalEnCaptura = null
    }

    private fun guardarMovimiento() {
        if (guardandoMovimiento) return

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
            when {
                archivoTemporalSeleccionado != null ->
                    actualizarMovimientoConComprobante(
                        movimiento,
                        archivoTemporalSeleccionado!!
                    )
                comprobanteQuitado ->
                    actualizarSinComprobante(movimiento)
                else -> actualizarMovimiento(movimiento)
            }
        } else if (archivoTemporalSeleccionado != null) {
            agregarMovimientoConComprobante(
                movimiento,
                archivoTemporalSeleccionado!!
            )
        } else {
            agregarMovimiento(movimiento)
        }
    }

    private fun agregarMovimiento(movimiento: Movimiento) {
        if (guardandoMovimiento) return

        guardandoMovimiento = true
        btnGuardar.isEnabled = false
        val resultado = repositorio.agregar(movimiento)
        if (resultado > 0L) {
            mostrarMensaje("Movimiento guardado correctamente")
            setResult(RESULT_OK)
            finish()
        } else {
            guardandoMovimiento = false
            btnGuardar.isEnabled = true
            mostrarMensaje("Error al guardar movimiento")
        }
    }

    private fun agregarMovimientoConComprobante(
        movimiento: Movimiento,
        archivoTemporal: File
    ) {
        if (guardandoMovimiento) return

        guardandoMovimiento = true
        btnGuardar.isEnabled = false
        val resultado = repositorio.agregarConComprobante(
            movimiento,
            archivoTemporal
        )
        if (resultado > 0L) {
            mostrarMensaje("Movimiento guardado correctamente")
            setResult(RESULT_OK)
            finish()
        } else {
            guardandoMovimiento = false
            btnGuardar.isEnabled = true
            mostrarMensaje("Error al guardar movimiento")
        }
    }

    private fun actualizarMovimiento(movimiento: Movimiento) {
        if (guardandoMovimiento) return

        guardandoMovimiento = true
        btnGuardar.isEnabled = false
        val resultado = repositorio.actualizar(movimiento)
        if (resultado > 0) {
            mostrarMensaje("Movimiento actualizado correctamente")
            setResult(RESULT_OK)
            finish()
        } else {
            guardandoMovimiento = false
            btnGuardar.isEnabled = true
            mostrarMensaje("Error al actualizar movimiento")
        }
    }

    private fun actualizarMovimientoConComprobante(
        movimiento: Movimiento,
        archivoTemporal: File
    ) {
        if (guardandoMovimiento) return

        guardandoMovimiento = true
        btnGuardar.isEnabled = false
        val resultado = repositorio.actualizarConComprobante(
            movimiento,
            archivoTemporal
        )
        if (resultado > 0) {
            mostrarMensaje("Movimiento actualizado correctamente")
            setResult(RESULT_OK)
            finish()
        } else {
            guardandoMovimiento = false
            btnGuardar.isEnabled = true
            mostrarMensaje("Error al actualizar movimiento")
        }
    }

    private fun actualizarSinComprobante(movimiento: Movimiento) {
        if (guardandoMovimiento) return

        guardandoMovimiento = true
        btnGuardar.isEnabled = false
        val resultado = repositorio.actualizarSinComprobante(movimiento)
        if (resultado > 0) {
            mostrarMensaje("Movimiento actualizado correctamente")
            setResult(RESULT_OK)
            finish()
        } else {
            guardandoMovimiento = false
            btnGuardar.isEnabled = true
            mostrarMensaje("Error al actualizar movimiento")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            ESTADO_TEMPORAL_SELECCIONADO,
            archivoTemporalSeleccionado?.absolutePath
        )
        outState.putString(
            ESTADO_TEMPORAL_EN_CAPTURA,
            archivoTemporalEnCaptura?.absolutePath
        )
        outState.putBoolean(
            ESTADO_COMPROBANTE_QUITADO,
            comprobanteQuitado
        )
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (isFinishing && ::storage.isInitialized) {
            storage.eliminarTemporal(archivoTemporalSeleccionado)
            limpiarCapturaEnCurso()
        }
        super.onDestroy()
        if (::repositorio.isInitialized) {
            repositorio.cerrar()
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}
