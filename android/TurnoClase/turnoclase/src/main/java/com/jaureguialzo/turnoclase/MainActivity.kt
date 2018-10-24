//
//  TurnoClase
//  Copyright 2015 Ion Jaureguialzo Sarasola.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package com.jaureguialzo.turnoclase

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // REF: Detectar si estamos en modo test: https://stackoverflow.com/a/40220621/5136913
    private val isRunningTest: Boolean by lazy {
        try {
            Class.forName("android.support.test.espresso.Espresso")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ocultar la barra de título en horizontal
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE))
            supportActionBar!!.hide()
        else
            supportActionBar!!.show()

        // Cargar el layout
        setContentView(R.layout.activity_main)

        // Nombre aleatorio
        val nombreAleatorio = Nombres().aleatorio()
        campoNombre.hint = nombreAleatorio

        // Depuración
        if (BuildConfig.DEBUG) {
            campoAula.setText("BE131")
            campoNombre.setText(nombreAleatorio)
        }

        // Evento del botón conectar, pasamos a la siguiente actividad
        botonSiguiente.setOnClickListener {
            // Obtenemos los datos del interfaz
            val codigoAula = campoAula!!.text.toString().toUpperCase()
            val nombreUsuario = campoNombre!!.text.toString()

            // Si hay texto, pasamos a la siguiente actividad
            if (codigoAula.length >= 5 && nombreUsuario.length >= 2) {

                val intent = Intent(this@MainActivity, AlumnoTurno::class.java)
                intent.putExtra("CODIGO_AULA", codigoAula)
                intent.putExtra("NOMBRE_USUARIO", nombreUsuario)

                startActivity(intent)
            }
        }

        // Animación del botón
        botonSiguiente.setOnTouchListener { v, event ->
            animarBoton(event, v, "botonSiguiente")
            false
        }

    }

    private fun animarBoton(event: MotionEvent, v: View?, nombre: String) {

        if (!isRunningTest) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d("TurnoClase", "DOWN del botón $nombre...")

                // Difuminar
                val anim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.15f)
                anim.duration = 100
                anim.start()
            } else if (event.action == MotionEvent.ACTION_UP) {
                Log.d("TurnoClase", "UP del botón $nombre...")

                // Restaurar
                val anim = ObjectAnimator.ofFloat(v, "alpha", 0.15f, 1f)
                anim.duration = 300
                anim.start()
            }
        }
    }

    // Crear el menú "Acerca de..."
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Abrir la actividad "Acerca de..."
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val result = super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_main).setOnMenuItemClickListener {
            startActivity(Intent(this@MainActivity, AcercaDe::class.java))
            true
        }
        return result
    }

}
