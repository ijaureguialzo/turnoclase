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
import android.view.View
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

    // REF: Detectar si estamos en modo test: https://stackoverflow.com/a/40220621/5136913
    private val isRunningTest: Boolean by lazy {
        try {
            Class.forName("android.support.test.espresso.Espresso")
            Log.d(TAG, "Estamos en modo test")
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

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if (isRunningTest) {
            actualizarAula("BE131", 0)
            actualizarMensaje("")
        } else {

            // Limpiar el UI
            actualizarAula("...", 0)
            actualizarMensaje("")

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

                                            val document = it.result!!
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
            botonCodigoAulaCorto()
        }

        // Evento del botón botonCodigoAula (crear nueva aula)
        botonCodigoAula.setOnLongClickListener {
            botonCodigoAulaLargo()
            true
        }

        // Evento del botón Siguiente
        botonSiguiente.setOnClickListener {
            botonSiguiente()
        }

        // Evento del botón botonEnCola
        botonEnCola.setOnClickListener {
            botonEnCola()
        }

        // Animación del botón Siguiente
        botonSiguiente.setOnTouchListener { v, event ->
            animarBoton(event, v, "botonSiguiente")
            false
        }

        // Animación del botón botonEnCola
        botonEnCola.setOnTouchListener { v, event ->
            animarBoton(event, v, "botonEnCola")
            false
        }

        // Animación del botón botonCodigoAula
        botonCodigoAula.setOnTouchListener { v, event ->
            animarBoton(event, v, "botonCodigoAula")
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
                                mostrarSiguiente()
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

    private fun mostrarSiguiente(avanzarCola: Boolean = false) {

        Log.d(TAG, "Mostrando el siguiente alumno...")

        if (refAula != null) {

            refAula!!.collection("cola").orderBy("timestamp").limit(1).get()
                    .addOnCompleteListener { querySnapshot ->
                        if (!querySnapshot.isSuccessful) {
                            Log.e(TAG, "Error al recuperar datos: ", querySnapshot.exception)
                        } else {
                            if (querySnapshot.result!!.count() > 0) {
                                val refPosicion = querySnapshot.result!!.documents[0].reference

                                refPosicion.get().addOnCompleteListener {
                                    val posicion = it.result

                                    // Cargar el alumno
                                    db.collection("alumnos").document(posicion!!["alumno"] as String)
                                            .get()
                                            .addOnCompleteListener { document ->
                                                if (document.isSuccessful) {
                                                    if (document.result!!.exists()) {
                                                        val alumno = document.result

                                                        // Mostrar el nombre
                                                        actualizarMensaje(alumno!!["nombre"] as String)

                                                        // Borrar la entrada de la cola
                                                        if (avanzarCola) {
                                                            refPosicion.delete()
                                                        }
                                                    } else {
                                                        Log.e(TAG, "El alumno no existe")
                                                        actualizarMensaje("?")
                                                    }
                                                } else {
                                                    Log.e(TAG, "Error al recuperar datos: ", it.exception)
                                                }
                                            }
                                }
                            } else {
                                Log.d(TAG, "Cola vacía")
                                actualizarMensaje("")
                            }
                        }
                    }
        }
    }

    private fun botonSiguiente() {
        mostrarSiguiente(true)
    }

    private fun botonEnCola() {

        Log.d("TurnoClase", "Este botón sólo se usa para los test de UI")

        if (isRunningTest) {
            if (n >= 0) {
                actualizarAula(n)
                actualizarMensaje(Nombres().aleatorio())
            } else {
                actualizarAula(0)
                actualizarMensaje("")
            }

            n -= 1
        }
    }

    private fun botonCodigoAulaCorto() {

        //Log.d(TAG, "Vaciando el aula...")
        // Pendiente de implementar en el servidor, no se puede borrar una colección desde el cliente
        // REF: https://firebase.google.com/docs/firestore/manage-data/delete-data?hl=es-419

        // Menú de acciones para gestionar múltiples profesores
        //mostrarAcciones()

    }

    private fun botonCodigoAulaLargo() {

        Log.d(TAG, "Generando nueva aula...")

        if (listenerAula != null) {

            listenerAula!!.remove()
            listenerAula = null

            // Pendiente: Llamar a la función de vaciar la cola porque no se borra la subcolección

            db.collection("aulas").document(uid!!).delete().addOnCompleteListener {
                if (!it.isSuccessful) {
                    Log.e(TAG, "Error al borrar el aula: ", it.exception)
                } else {
                    Log.d(TAG, "Aula borrada")
                    Log.d(TAG, "Creando nueva aula")
                    crearAula()
                }
            }

        } else {
            Log.e(TAG, "El listener no está conectado")
        }

    }

    private fun actualizarAula(codigo: String = "?", enCola: Int = -1) {
        actualizarAula(codigo)
        actualizarAula(enCola)
    }

    private fun actualizarAula(codigo: String) {
        botonCodigoAula.text = codigo
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

    //region Funciones exclusivas de la versión Android
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

    companion object {

        private val TAG = "MainActivity"

    }
    //endregion

}
