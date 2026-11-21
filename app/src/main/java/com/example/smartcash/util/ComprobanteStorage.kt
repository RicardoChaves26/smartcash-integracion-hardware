package com.example.smartcash.util

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.UUID

class ComprobanteStorage(context: Context) {

    private val contexto = context.applicationContext

    companion object {

        private const val CARPETA_COMPROBANTES =
            "comprobantes"

        private const val CARPETA_TEMPORALES =
            "comprobantes_temporales"

        private const val TAG = "ComprobanteStorage"
    }

    // Obtiene o crea la carpeta definitiva.
    private fun carpetaComprobantes(): File {

        val carpeta = File(
            contexto.filesDir,
            CARPETA_COMPROBANTES
        )

        if (!carpeta.exists() && !carpeta.mkdirs()) {

            throw IOException(
                "No se pudo crear la carpeta de comprobantes"
            )
        }

        if (!carpeta.isDirectory) {
            throw IOException("Carpeta de comprobantes inválida")
        }

        return carpeta
    }

    // Obtiene o crea la carpeta temporal.
    private fun carpetaTemporales(): File {

        val carpeta = File(
            contexto.cacheDir,
            CARPETA_TEMPORALES
        )

        if (!carpeta.exists() && !carpeta.mkdirs()) {

            throw IOException(
                "No se pudo crear la carpeta temporal"
            )
        }

        if (!carpeta.isDirectory) {
            throw IOException("Carpeta temporal inválida")
        }

        return carpeta
    }

    // Crea un archivo para que la cámara
    // almacene temporalmente una fotografía.
    fun crearTemporal(): File? {

        return try {

            File.createTempFile(
                "temporal_",
                ".jpg",
                carpetaTemporales()
            )

        } catch (e: IOException) {

            Log.e(TAG, "Error al crear archivo temporal", e)

            null
        }
    }

    // Comprueba que el archivo sea una imagen reconocible.
    private fun esImagenValida(archivo: File): Boolean {

        if (!archivo.isFile || archivo.length() == 0L) {
            return false
        }

        val opciones = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(
            archivo.absolutePath,
            opciones
        )

        return opciones.outWidth > 0 &&
                opciones.outHeight > 0
    }

    // Comprueba que el archivo temporal pertenezca
    // a la carpeta administrada por SmartCash.
    private fun esTemporalPropio(archivo: File): Boolean {

        return archivo.canonicalFile.parentFile ==
                carpetaTemporales().canonicalFile
    }

    // Copia una fotografía temporal al
    // almacenamiento definitivo.
    // Devuelve únicamente el nombre del archivo.
    fun guardarComprobante(archivoTemporal: File): String? {

        var destino: File? = null

        return try {

            if (!esTemporalPropio(archivoTemporal) ||
                !esImagenValida(archivoTemporal)
            ) {

                return null
            }

            val nombre = "foto_${UUID.randomUUID()}.jpg"

            destino = File(
                carpetaComprobantes(),
                nombre
            )

            archivoTemporal.copyTo(
                destino,
                overwrite = false
            )

            nombre

        } catch (e: Exception) {

            destino?.delete()

            Log.e(TAG, "Error al guardar comprobante", e)

            null
        }
    }

    // Recupera el archivo de una fotografía guardada.
    fun obtenerArchivo(rutaComprobante: String?): File? {

        if (rutaComprobante.isNullOrBlank()) {
            return null
        }

        // Evita aceptar rutas externas o nombres inválidos.
        val patron = Regex(
            "^foto_[a-fA-F0-9-]{36}\\.jpg$"
        )

        if (!patron.matches(rutaComprobante)) {
            return null
        }

        return try {

            val carpeta = carpetaComprobantes()

            val archivo = File(
                carpeta,
                rutaComprobante
            ).canonicalFile

            if (
                archivo.parentFile == carpeta.canonicalFile &&
                archivo.isFile
            ) {

                archivo

            } else {

                null
            }

        } catch (e: IOException) {

            Log.e(TAG, "Error al recuperar comprobante", e)

            null
        }
    }

    // Comprueba que el comprobante exista
    // y tenga información.
    fun existeComprobante(rutaComprobante: String?): Boolean {

        val archivo = obtenerArchivo(rutaComprobante)

        return archivo != null && archivo.length() > 0L
    }

    // Elimina una fotografía del almacenamiento definitivo.
    fun eliminarComprobante(rutaComprobante: String?): Boolean {

        if (rutaComprobante.isNullOrBlank()) {
            return true
        }

        return try {

            val patron = Regex(
                "^foto_[a-fA-F0-9-]{36}\\.jpg$"
            )

            if (!patron.matches(rutaComprobante)) {
                return false
            }

            val carpeta = carpetaComprobantes()

            val archivo = File(
                carpeta,
                rutaComprobante
            ).canonicalFile

            if (archivo.parentFile != carpeta.canonicalFile) {
                return false
            }

            if (!archivo.exists()) {
                return true
            }

            archivo.isFile && archivo.delete()

        } catch (e: Exception) {

            Log.e(TAG, "Error al eliminar comprobante", e)

            false
        }
    }

    // Elimina un archivo temporal descartado.
    fun eliminarTemporal(archivoTemporal: File?): Boolean {

        if (archivoTemporal == null) {
            return true
        }

        return try {

            if (!esTemporalPropio(archivoTemporal)) {
                return false
            }

            if (!archivoTemporal.exists()) {
                return true
            }

            archivoTemporal.isFile &&
                    archivoTemporal.delete()

        } catch (e: Exception) {

            Log.e(TAG, "Error al eliminar temporal", e)

            false
        }
    }
}