//
//  TurnoClaseProfesor
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

package com.jaureguialzo.turnoclaseprofesor

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
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // ID de usuario único generado por Firebase
    private var uid: String? = null

    // Listeners para recibir las actualizaciones
    private var listenerAula: ListenerRegistration? = null
    private var listenerCola: ListenerRegistration? = null

    // Referencias al documento del aula y la posición en la cola
    private var refAula: DocumentReference? = null

    // Para simular el interfaz al hacer las capturas
    private var n = 2

    // Activar Firestore
    private val db = FirebaseFirestore.getInstance()
    private var mAuth: FirebaseAuth? = null

    // Referencias a los objetos (borrar)
    private var aula: Aula? = null
    private var listener: ListenerRegistration? = null

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

        // Configurar las opciones de Firebase
        val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        db.firestoreSettings = settings

        // Ocultar la barra de título en horizontal
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_XLARGE))
            supportActionBar!!.hide()
        else
            supportActionBar!!.show()

        // Cargar el layout
        setContentView(R.layout.activity_main)

        // Limpiar el UI
        actualizarAula("...")
        actualizarMensaje("")

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if (isRunningTest) {
            actualizarAula("BE131", 2)
            actualizarMensaje("")
        } else {

            // Iniciar sesión y conectar al aula
            mAuth = FirebaseAuth.getInstance()

            mAuth!!.signInAnonymously()
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            uid = mAuth?.currentUser?.uid
                            Log.d(TAG, "Registrado como usuario con UID: $uid")

                            // Cargar el aula y si no, crearla
                            db.collection("aulas").document(uid!!)
                                    .get()
                                    .addOnCompleteListener {
                                        if (it.isSuccessful) {

                                            val document = it.result
                                            if (!document.exists()) {
                                                Log.d(TAG, "Creando nueva aula")
                                                crearAula()
                                            } else {
                                                Log.d(TAG, "Conectado a aula existente")
                                                refAula = document.reference
                                                conectarListener()
                                            }
                                        }
                                    }
                        } else {
                            Log.e(TAG, "Error de inicio de sesión", task.exception)
                        }
                    }
        }

        // Evento del botón botonCodigoAula (vaciar el aula)
        botonCodigoAula.setOnClickListener {
            Log.d(TAG, "Vaciando el aula...")

            if (this.aula != null) {
                this.aula?.cola = ArrayList<String>()

                // Actualizar el aula
                val datos = HashMap<String, Any>()
                datos.put("cola", ArrayList<String>())

                db.collection("aulas").document(uid!!)
                        .update(datos)
                        .addOnSuccessListener {
                            Log.d(TAG, "Aula vaciada")

                            etiquetaNombreAlumno.text = ""
                            conectarListener()
                        }
                        .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar el aula", e) }

            } else {
                Log.e(TAG, "No hay objeto aula")
            }

        }

        // Evento del botón botonCodigoAula (crear nueva aula)
        botonCodigoAula.setOnLongClickListener {

            Log.d(TAG, "Generando nueva aula...")

            if (this.aula != null) {

                this.aula = Aula("?", ArrayList<String>())

                listener?.remove()
                listener = null

                // Borrar el aula
                db.collection("aulas").document(uid!!)
                        .delete()
                        .addOnSuccessListener {
                            Log.d(TAG, "Aula borrada")

                            // Crear el aula
                            val datos = HashMap<String, Any>()
                            datos.put("cola", ArrayList<String>())

                            db.collection("aulas").document(uid!!)
                                    .set(datos)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Aula creada")

                                        etiquetaNombreAlumno.text = ""
                                        conectarListener()
                                    }
                                    .addOnFailureListener { e -> Log.e(TAG, "Error al crear el aula", e) }

                        }
                        .addOnFailureListener { e -> Log.e(TAG, "Error al borrar el aula", e) }

            } else {
                Log.e(TAG, "No hay objeto aula")
            }

            true
        }

        // Evento del botón Siguiente
        botonSiguiente.setOnClickListener {
            Log.d(TAG, "Mostrando el siguiente alumno...")

            if (aula != null) {

                if (aula?.cola?.size!! > 0) {

                    val siguiente = aula?.cola?.removeAt(0)

                    Log.d(TAG, "Siguiente: ${siguiente}")

                    // Cargar el alumno
                    db.collection("alumnos").document(siguiente!!)
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val document = task.result
                                    if (document.exists()) {
                                        val alumno = document.data

                                        val nombre = alumno!!["nombre"] as? String ?: "?"

                                        if (nombre.length >= 10) {
                                            etiquetaNombreAlumno.setTextSize(TypedValue.COMPLEX_UNIT_PT, 9f)
                                        } else if (nombre.length > 4 && nombre.length < 10) {
                                            etiquetaNombreAlumno.setTextSize(TypedValue.COMPLEX_UNIT_PT, 14f)
                                        } else
                                            etiquetaNombreAlumno.setTextSize(TypedValue.COMPLEX_UNIT_PT, 20f)

                                        etiquetaNombreAlumno.text = nombre

                                    } else {
                                        Log.d(TAG, "El alumno no existe")
                                        etiquetaNombreAlumno.text = ""
                                    }
                                } else {
                                    Log.e(TAG, "Error al recuperar el aula: ", task.exception)
                                }
                            }

                    // Actualizar el aula
                    val datos = HashMap<String, Any>()
                    datos.put("cola", aula?.cola!!)

                    db.collection("aulas").document(uid!!)
                            .update(datos)
                            .addOnSuccessListener {
                                Log.d(TAG, "Cola actualizada")
                                conectarListener()
                            }
                            .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar el aula", e) }

                } else {
                    Log.d(TAG, "El aula está vacía")
                    etiquetaNombreAlumno.text = ""
                }
            } else {
                Log.e(TAG, "No hay objeto aula")
            }

        }

        // Evento del botón botonEnCola
        botonEnCola.setOnClickListener {
            Log.d("TurnoClase", "Este botón ya no hace nada :)")

            if (isRunningTest) {
                n -= 1
                if (n >= 0) {
                    botonEnCola.text = n.toString()

                    val nombre = Nombres().aleatorio()

                    if (nombre.length >= 10) {
                        etiquetaNombreAlumno.setTextSize(TypedValue.COMPLEX_UNIT_PT, 9f)
                    } else if (nombre.length > 4 && nombre.length < 10) {
                        etiquetaNombreAlumno.setTextSize(TypedValue.COMPLEX_UNIT_PT, 14f)
                    } else
                        etiquetaNombreAlumno.setTextSize(TypedValue.COMPLEX_UNIT_PT, 20f)

                    etiquetaNombreAlumno.text = nombre

                } else {
                    botonEnCola.text = "0"
                    etiquetaNombreAlumno.text = ""
                }
            }

        }

        // Animación del botón Siguiente
        botonSiguiente.setOnTouchListener { v, event ->
            if (!isRunningTest) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Log.d("TurnoClase", "DOWN del botón botonSiguiente...")

                    // Difuminar
                    val anim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.15f)
                    anim.duration = 100
                    anim.start()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    Log.d("TurnoClase", "UP del botón botonSiguiente...")

                    // Restaurar
                    val anim = ObjectAnimator.ofFloat(v, "alpha", 0.15f, 1f)
                    anim.duration = 300
                    anim.start()
                }
            }
            false
        }

        // Animación del botón botonEnCola
        botonEnCola.setOnTouchListener { v, event ->
            if (!isRunningTest) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Log.d("TurnoClase", "DOWN del botón botonEnCola...")

                    // Difuminar
                    val anim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.15f)
                    anim.duration = 100
                    anim.start()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    Log.d("TurnoClase", "UP del botón botonEnCola...")

                    // Restaurar
                    val anim = ObjectAnimator.ofFloat(v, "alpha", 0.15f, 1f)
                    anim.duration = 300
                    anim.start()
                }
            }
            false
        }

        // Animación del botón botonCodigoAula
        botonCodigoAula.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d("TurnoClase", "DOWN del botón botonCodigoAula...")

                // Difuminar
                val anim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.15f)
                anim.duration = 100
                anim.start()
            } else if (event.action == MotionEvent.ACTION_UP) {
                Log.d("TurnoClase", "UP del botón botonCodigoAula...")

                // Restaurar
                val anim = ObjectAnimator.ofFloat(v, "alpha", 0.15f, 1f)
                anim.duration = 300
                anim.start()
            }
            false
        }

    }

    private fun crearAula() {

        // Almacenar la referencia a la nueva aula
        refAula = db.collection("aulas").document(uid!!)

        val datos = HashMap<String, Any>()
        datos["timestamp"] = FieldValue.serverTimestamp()

        // Guardar el documento con un Timestamp, para que se genere el código
        refAula!!.set(datos)
                .addOnSuccessListener {
                    Log.d(TAG, "Aula creada")
                    conectarListener()
                }
                .addOnFailureListener { e -> Log.e(TAG, "Error al crear el aula", e) }
    }

    private fun actualizarAula(codigoAula: String = "?", enCola: Int = -1) {
        actualizarAula(codigoAula)
        actualizarAula(enCola)
    }

    private fun actualizarAula(codigoAula: String) {
        botonCodigoAula.text = codigoAula
    }

    private fun actualizarAula(enCola: Int) {
        if (enCola != -1)
            botonEnCola.text = enCola.toString()
        else
            botonEnCola.text = "..."
    }

    private fun actualizarMensaje(texto: String = "?") {
        etiquetaNombreAlumno.text = texto
    }

    private fun conectarListener() {

        // Conectar el listener del aula para detectar cambios (por ejemplo, que se borra)
        if (listenerAula == null) {
            listenerAula = refAula!!.addSnapshotListener { snapshot, _ ->

                if (snapshot != null && snapshot.exists()) {
                    refAula = snapshot.reference

                    val aula = snapshot.data
                    Log.d(TAG, "Actualizando datos del aula")

                    val codigoAula = aula!!["codigo"] as? String ?: "?"
                    Log.d(TAG, "Aula: $codigoAula")

                    actualizarAula(codigoAula)

                    // Listener de la cola
                    if (listenerCola == null) {
                        listenerCola = refAula!!.collection("cola").addSnapshotListener { querySnapshot, error ->

                            if (error != null) {
                                Log.e(TAG, "Error al recuperar datos: ", error)
                            } else {
                                actualizarAula(querySnapshot!!.documents.count())
                            }
                        }
                    }

                } else {
                    Log.d(TAG, "El aula ha desaparecido")
                    actualizarAula("?", 0)
                }
            }
        }
    }

    private fun actualizarPantalla() {

        if (this.aula != null) {
            val aula = this.aula!!

            // Mostramos el código en la pantalla
            botonCodigoAula.text = aula.codigo

            botonEnCola.text = aula.cola.size.toString()
            Log.d(TAG, "Alumnos en cola: ${aula.cola.size}")

        } else {
            Log.e(TAG, "No hay objeto aula")
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

    companion object {

        private val TAG = "MainActivity"

    }

}
