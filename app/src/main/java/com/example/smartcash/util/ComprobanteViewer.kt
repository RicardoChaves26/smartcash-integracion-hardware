package com.example.smartcash.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import java.io.File

class ComprobanteViewer {

    enum class Resultado {
        EXITO,
        AUSENTE,
        DANADO
    }

    companion object {
        private const val TAG = "ComprobanteViewer"

        fun cargarEn(imageView: ImageView, archivo: File?): Resultado {
            if (archivo == null || !archivo.exists() || archivo.length() == 0L) {
                return Resultado.AUSENTE
            }

            return try {
                val opciones = BitmapFactory.Options()
                opciones.inJustDecodeBounds = true
                BitmapFactory.decodeFile(archivo.absolutePath, opciones)

                if (opciones.outWidth <= 0 || opciones.outHeight <= 0) {
                    return Resultado.DANADO
                }

                opciones.inJustDecodeBounds = false

                // Reducir la imagen para evitar problemas de memoria en la miniatura
                opciones.inSampleSize = calcularSampleSize(opciones, 800, 800)

                val bitmap = BitmapFactory.decodeFile(archivo.absolutePath, opciones)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Resultado.EXITO
                } else {
                    Resultado.DANADO
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar la imagen", e)
                Resultado.DANADO
            }
        }

        private fun calcularSampleSize(
            opciones: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            val (height: Int, width: Int) = opciones.run { outHeight to outWidth }
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2

                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }
    }
}