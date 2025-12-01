package com.erickballas.pruebaconceptoalgoritmolpa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.erickballas.pruebaconceptoalgoritmolpa.view.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Llamamos a tu pantalla de inicio
            // (Más adelante aquí configurarás la navegación real)
            HomeScreen()
        }
    }
}