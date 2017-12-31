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
import android.util.TypedValue
import android.view.Menu
import android.view.MotionEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.activity_alumno_turno.*

class AlumnoTurno : AppCompatActivity() {

    // Referencias a los objetos
    private var aula: Aula? = null
    private var uid: String? = null
    private var listener: ListenerRegistration? = null

    private var refAula: DocumentReference? = null

    // Parámetros que llegan en el Intent
    private var codigoAula: String? = null
    private var nombreUsuario: String? = null

    private var mAuth: FirebaseAuth? = null

    // Activar Firestore
    private val db = FirebaseFirestore.getInstance()

    // REF: Detectar si estamos en modo test: https://stackoverflow.com/a/40220621/5136913
    private val isRunningTest: Boolean by lazy {
        try {
            Class.forName("android.support.test.espresso.Espresso")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    // Para simular el interfaz al hacer las capturas
    private var n = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ocultar la barra de título en horizontal
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_XLARGE))
            supportActionBar!!.hide()
        else
            supportActionBar!!.show()

        // Cargar el layout
        setContentView(R.layout.activity_alumno_turno)

        // Mostrar el número almacenado
        etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 16f)
        etiquetaNumero.text = numeroTurno

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if (isRunningTest) {
            etiquetaAula.text = "BE131"
            etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
            etiquetaNumero.text = "2"
        } else {

            // Extraer los parámetros desde el Intent
            val intent = intent
            codigoAula = intent.getStringExtra("CODIGO_AULA")
            nombreUsuario = intent.getStringExtra("NOMBRE_USUARIO")

            mAuth = FirebaseAuth.getInstance()

            mAuth!!.signInAnonymously()
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            uid = mAuth?.currentUser?.uid
                            Log.d(TAG, "Registrado como usuario con UID: " + uid)

                            // Crear el alumno en la colección alumnos
                            var datos = HashMap<String, Any>()
                            datos.put("nombre", nombreUsuario!!)

                            db.collection("alumnos").document(uid!!)
                                    .set(datos)
                                    .addOnSuccessListener { Log.d(TAG, "Alumno actualizado") }
                                    .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar el alumno", e) }

                            // Añadir al alumno a la cola del aula
                            db.collection("aulas")
                                    .whereEqualTo("codigo", codigoAula)
                                    .limit(1)
                                    .get()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            if (task.result.count() > 0) {
                                                for (document in task.result) {
                                                    Log.d(TAG, "Conectado a aula existente")

                                                    // Añadir el listener
                                                    if (listener == null) {

                                                        listener = document.reference.addSnapshotListener({ snapshot, _ ->
                                                            if (snapshot != null && snapshot.exists()) {

                                                                refAula = snapshot.reference

                                                                val aula = snapshot.data
                                                                Log.d(TAG, "Actualizando datos del aula")

                                                                val cola = aula["cola"] as? ArrayList<String> ?: ArrayList<String>()
                                                                val codigo = aula["codigo"] as? String ?: "?"

                                                                this.aula = Aula(codigo, cola)

                                                                // Si el usuario no está en la cola, lo añadimos

                                                                if (App.pedirTurno && !cola.contains(uid!!)) {

                                                                    App.pedirTurno = false
                                                                    this.aula?.cola?.add(uid!!)

                                                                    datos = HashMap<String, Any>()
                                                                    datos.put("cola", this.aula?.cola!!)

                                                                    snapshot.reference.update(datos)
                                                                            .addOnSuccessListener { Log.d(TAG, "Cola actualizada") }
                                                                            .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar el aula", e) }

                                                                }

                                                                Log.d(TAG, "Aula: " + this.aula)

                                                                this.actualizar()

                                                            } else {
                                                                Log.d(TAG, "El aula ha desaparecido")

                                                                if (listener != null) {
                                                                    listener?.remove()
                                                                    listener = null
                                                                    aula = null
                                                                    numeroTurno = ""

                                                                    App.pedirTurno = true
                                                                }

                                                                // Volver a la pantalla inicial
                                                                this.finish()
                                                            }
                                                        })
                                                    }
                                                }
                                            } else {
                                                Log.d(TAG, "Aula no encontrada")
                                                etiquetaAula.text = "?"
                                            }
                                        } else {
                                            Log.d(TAG, "Error getting documents: ", task.exception)
                                        }
                                    }
                        } else {
                            Log.e(TAG, "Error de inicio de sesión", task.exception)
                        }
                    }
        }

        // Evento del botón Actualizar
        botonActualizar.setOnClickListener {

            Log.d("TurnoClase", "Este botón ya no hace nada...")

            if (isRunningTest) {

                if (n > 0) {
                    etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
                    numeroTurno = n.toString()
                } else if (n == 0) {
                    etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 16f)
                    numeroTurno = getResources().getString(R.string.mensaje_turno)
                } else {
                    etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
                    numeroTurno = ""
                }
                etiquetaNumero.setText(numeroTurno)

                n -= 1
            }

        }

        // Evento del botón Cancelar
        botonCancelar.setOnClickListener { cancelar() }

        // Animación del botón Actualizar
        botonActualizar.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d("TurnoClase", "DOWN del botón botonActualizar...")
                val anim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.15f)
                anim.duration = 100
                anim.start()
            } else if (event.action == MotionEvent.ACTION_UP) {
                Log.d("TurnoClase", "UP del botón botonActualizar...")
                val anim = ObjectAnimator.ofFloat(v, "alpha", 0.15f, 1f)
                anim.duration = 300
                anim.start()
            }
            false
        }

        // Animación del botón Cancelar
        botonCancelar.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d("TurnoClase", "DOWN del botón botonActualizar...")
                val anim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.15f)
                anim.duration = 100
                anim.start()
            } else if (event.action == MotionEvent.ACTION_UP) {
                Log.d("TurnoClase", "UP del botón botonActualizar...")
                val anim = ObjectAnimator.ofFloat(v, "alpha", 0.15f, 1f)
                anim.duration = 300
                anim.start()
            }
            false
        }

    }

    private fun actualizar() {

        if (this.aula != null) {
            val aula = this.aula!!

            // Mostramos el código en la pantalla
            etiquetaAula.text = aula.codigo

            val posicion = aula.cola.indexOf(uid)

            if (posicion > 0) {
                etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
                numeroTurno = posicion.toString()
            } else if (posicion == 0) {
                etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 16f)
                numeroTurno = getResources().getString(R.string.mensaje_turno)
            } else {
                etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
                numeroTurno = ""
            }
            etiquetaNumero.setText(numeroTurno)

            Log.d(TAG, "Alumnos en cola: ${aula.cola.size}")

        } else {
            Log.e(TAG, "No hay objeto aula")
        }

    }

    // Detectamos el botón de retorno del teléfono y quitamos al usuario de la cola
    override fun onBackPressed() {
        cancelar()
        super.onBackPressed()
    }

    // Quitamos al usuario de la cola
    private fun cancelar() {

        if (this.aula != null && this.aula?.cola!!.contains(uid)) {

            this.aula?.cola?.remove(uid)

            val datos = HashMap<String, Any>()
            datos.put("cola", this.aula?.cola!!)

            refAula!!.update(datos)
                    .addOnSuccessListener { Log.d(TAG, "Cola actualizada") }
                    .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar el aula", e) }

        }

        if (listener != null) {
            listener?.remove()
            listener = null
            aula = null
            numeroTurno = ""

            App.pedirTurno = true
        }

        // Cerramos la actividad
        this.finish()
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
            startActivity(Intent(this@AlumnoTurno, AcercaDe::class.java))
            true
        }
        return result
    }

    companion object {

        // Valor para mostrar en la etiqueta.
        // Es estático para que sobreviva cuando rota la pantalla y se destruye la actividad
        private var numeroTurno = ""
        private val TAG = "AlumnoTurno"

    }

}
