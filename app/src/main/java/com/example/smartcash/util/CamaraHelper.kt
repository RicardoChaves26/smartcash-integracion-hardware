package com.example.smartcash.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

class CamaraHelper(
    context: Context,
    private val storage: ComprobanteStorage
) {

    private val contexto = context.applicationContext

    data class Captura(
        val archivo: File,
        val uri: Uri
    )

    fun crearCaptura(): Captura? {
        val archivo = storage.crearTemporal() ?: return null

        return try {
            val uri = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivo
            )
            Captura(archivo, uri)
        } catch (e: IllegalArgumentException) {
            storage.eliminarTemporal(archivo)
            Log.e(TAG, "No se pudo compartir el archivo temporal", e)
            null
        }
    }

    companion object {
        private const val TAG = "CamaraHelper"
    }
}
