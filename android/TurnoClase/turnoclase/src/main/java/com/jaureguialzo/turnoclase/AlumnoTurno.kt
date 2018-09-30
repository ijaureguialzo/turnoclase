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
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_alumno_turno.*

class AlumnoTurno : AppCompatActivity() {

    // Parámetros que llegan en el Intent
    private var codigoAula: String? = null
    private var nombreUsuario: String? = null

    // ID de usuario único generado por Firebase
    private var uid: String? = null

    // Listeners para recibir las actualizaciones
    private var listenerAula: ListenerRegistration? = null
    private var listenerCola: ListenerRegistration? = null

    // Referencias al documento del aula y la posición en la cola
    private var refAula: DocumentReference? = null
    private var refPosicion: DocumentReference? = null

    // Para simular el interfaz al hacer las capturas
    private var n = 2

    // Activar Firestore
    private val db = FirebaseFirestore.getInstance()
    private var mAuth: FirebaseAuth? = null

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
                    .addOnCompleteListener(this) {
                        if (it.isSuccessful) {
                            uid = mAuth?.currentUser?.uid
                            Log.d(TAG, "Registrado como usuario con UID: $uid")

                            actualizarAlumno()

                            encolarAlumno()
                        } else {
                            Log.e(TAG, "Error de inicio de sesión", it.exception)
                        }
                    }
        }

        // Evento del botón Actualizar
        botonActualizar.setOnClickListener {

            Log.d("TurnoClase", "Este botón ya no hace nada...")

            if (isRunningTest) {

                numeroTurno = when {
                    n > 0 -> {
                        etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
                        n.toString()
                    }
                    n == 0 -> {
                        etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 16f)
                        resources.getString(R.string.mensaje_turno)
                    }
                    else -> {
                        etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
                        ""
                    }
                }
                etiquetaNumero.text = numeroTurno

                n -= 1
            }

        }

        // Evento del botón Cancelar
        botonCancelar.setOnClickListener { cancelar() }

        // Animación del botón Actualizar
        botonActualizar.setOnTouchListener { v, event ->
            animarBoton(event, v, "botonActualizar")
            false
        }

        // Animación del botón Cancelar
        botonCancelar.setOnTouchListener { v, event ->
            animarBoton(event, v, "botonCancelar")
            false
        }

    }

    private fun actualizarAlumno() {

        // Crear el alumno en la colección alumnos
        val datos = HashMap<String, Any>()
        datos["nombre"] = nombreUsuario!!

        db.collection("alumnos").document(uid!!)
                .set(datos)
                .addOnSuccessListener { Log.d(TAG, "Alumno actualizado") }
                .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar el alumno", e) }
    }

    private fun encolarAlumno() {

        // Buscar el aula
        db.collection("aulas")
                .whereEqualTo("codigo", codigoAula)
                .limit(1)
                .get()
                .addOnCompleteListener {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Error al recuperar datos: ", it.exception)
                    } else {

                        // Comprobar que se han recuperado registros
                        if (it.result.count() > 0) {

                            // Accedemos al primer documento
                            val document = it.result.documents[0]
                            Log.d(TAG, "Conectado a aula existente")

                            conectarListenerAula(document)
                        } else {
                            Log.d(TAG, "Aula no encontrada")
                            etiquetaAula.text = "?"
                        }
                    }
                }
    }

    private fun conectarListenerAula(document: DocumentSnapshot) {

        // Conectar el listener del aula para detectar cambios (por ejemplo, que se borra)
        if (listenerAula == null) {
            listenerAula = document.reference.addSnapshotListener { snapshot, _ ->

                if (snapshot != null && snapshot.exists() && snapshot.data!!["codigo"] as? String == codigoAula) {
                    refAula = snapshot.reference
                    conectarListenerCola()
                } else {
                    Log.d(TAG, "El aula ha desaparecido")
                    desconectarListeners()
                    cerrarPantalla()
                }
            }
        }
    }

    private fun conectarListenerCola() {

        if (listenerCola == null) {
            listenerCola = refAula!!.collection("cola").addSnapshotListener { _, error ->

                if (error != null) {
                    Log.e(TAG, "Error al recuperar datos: ", error)
                } else {
                    buscarAlumnoEnCola()
                }
            }
        }
    }

    private fun buscarAlumnoEnCola() {

        refAula!!.collection("cola").whereEqualTo("alumno", uid).limit(1).get()
                .addOnCompleteListener {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Error al recuperar datos: ", it.exception)
                    } else {
                        pedirTurno(it.result)
                    }
                }
    }

    private fun pedirTurno(querySnapshot: QuerySnapshot) {

        if (App.pedirTurno && querySnapshot.documents.count() == 0) {
            App.pedirTurno = false

            Log.d(TAG, "Alumno no encontrado, lo añadimos")

            val datos = HashMap<String, Any>()
            datos["alumno"] = uid!!
            datos["timestamp"] = FieldValue.serverTimestamp()

            refAula!!.collection("cola").add(datos)
                    .addOnSuccessListener { documentReference ->
                        refPosicion = documentReference
                        actualizarPantalla()
                    }
                    .addOnFailureListener { e -> Log.e(TAG, "Error al añadir el documento", e) }

        } else if (querySnapshot.documents.count() > 0) {
            Log.e(TAG, "Alumno encontrado, ya está en la cola")
            App.pedirTurno = false
            refPosicion = querySnapshot.documents[0].reference
            actualizarPantalla()

        } else if (querySnapshot.documents.count() == 0) {
            Log.d(TAG, "La cola se ha vaciado")
            etiquetaNumero.text = ""
        }
    }

    private fun actualizarPantalla() {

        if (refAula != null && refPosicion != null) {

            // Mostramos el código en la pantalla
            etiquetaAula.text = codigoAula

            refPosicion!!.get().addOnCompleteListener { document ->

                val alumno = document.result

                refAula!!.collection("cola").whereLessThanOrEqualTo("timestamp", alumno["timestamp"] as Any).get()
                        .addOnCompleteListener {
                            if (!it.isSuccessful) {
                                Log.e(TAG, "Error al recuperar datos: ", it.exception)
                            } else {

                                val posicion = it.result.count()
                                Log.d(TAG, "Posicion en la cola: $posicion")

                                numeroTurno = when {
                                    posicion > 1 -> {
                                        etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
                                        (posicion - 1).toString()
                                    }
                                    posicion == 1 -> {
                                        etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 16f)
                                        resources.getString(R.string.mensaje_turno)
                                    }
                                    else -> {
                                        etiquetaNumero.setTextSize(TypedValue.COMPLEX_UNIT_PT, 48f)
                                        ""
                                    }
                                }
                                etiquetaNumero.text = numeroTurno

                            }
                        }
            }
        } else {
            etiquetaAula.text = ""
            Log.e(TAG, "No hay referencia al aula")
        }
    }

    private fun cerrarPantalla() {
        numeroTurno = ""
        App.pedirTurno = true

        // Volver a la pantalla inicial
        this.finish()
    }

    private fun desconectarListeners() {

        if (listenerAula != null) {
            listenerAula?.remove()
            listenerAula = null
        }

        if (listenerCola != null) {
            listenerCola?.remove()
            listenerCola = null
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

    // Detectamos el botón de retorno del teléfono y quitamos al usuario de la cola
    override fun onBackPressed() {
        cancelar()
        super.onBackPressed()
    }

    // Quitamos al usuario de la cola
    private fun cancelar() {

        Log.d(TAG, "Cancelando...")

        // Nos borramos de la cola
        if (refAula != null && refPosicion != null) {

            desconectarListeners()

            refPosicion!!.delete().addOnCompleteListener {
                cerrarPantalla()
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
            startActivity(Intent(this@AlumnoTurno, AcercaDe::class.java))
            true
        }
        return result
    }

    companion object {

        // Valor para mostrar en la etiqueta.
        // Es estático para que sobreviva cuando rota la pantalla y se destruye la actividad
        private var numeroTurno = ""
        private const val TAG = "AlumnoTurno"

    }

}
