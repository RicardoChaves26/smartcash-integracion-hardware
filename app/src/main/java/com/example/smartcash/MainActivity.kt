package com.example.smartcash

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartcash.fragments.HistorialFragment
import com.example.smartcash.fragments.InicioFragment
import com.example.smartcash.util.Ventana
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        Ventana.ajustarMargenes(findViewById(R.id.main))

        val menuInferior = findViewById<BottomNavigationView>(R.id.bottomNav)

        menuInferior.setOnItemSelectedListener { opcion ->
            when (opcion.itemId) {
                R.id.nav_inicio -> {
                    loadFragment(InicioFragment())
                    true
                }
                R.id.nav_historial -> {
                    loadFragment(HistorialFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            menuInferior.selectedItemId = R.id.nav_inicio
        }
    }

    private fun loadFragment(fragmento: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedorFragmentos, fragmento)
            .commit()
    }
}
