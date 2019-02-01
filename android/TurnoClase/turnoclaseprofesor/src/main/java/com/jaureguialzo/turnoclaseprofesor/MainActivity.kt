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
import android.support.v4.view.MenuCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    // ID de usuario único generado por Firebase
    private var uid: String? = null

    // Conectar a otro aula
    private var invitado = false
    private var uidPropio: String? = null

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

    // Datos del aula
    private var codigoAula = "..."
    private var PIN = "..."

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
                .build()
        db.firestoreSettings = settings

        // Ocultar la barra de título en horizontal en pantallas pequeñas
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE))
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
                            uidPropio = uid
                            Log.d(TAG, "Registrado como usuario con UID: $uid")

                            conectarAula()

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

    private fun conectarAula() {
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
    }

    private fun crearAula() {

        // Almacenar la referencia a la nueva aula
        refAula = db.collection("aulas").document(uid!!)

        // REF: Enteros aleatorios en Kotlin: https://stackoverflow.com/a/45687695
        fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) + start

        // Guardar el documento con un Timestamp, para que se genere el código
        val datos = HashMap<String, Any>()
        datos["timestamp"] = FieldValue.serverTimestamp()
        datos["pin"] = "%04d".format((0..9999).random())

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
                    val pin = aula["pin"] as? String ?: "?"

                    actualizarAula(codigoAula)
                    actualizarPIN(pin)

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

                    // Detectar si el aula desaparece y si somos invitados, desconectar
                    if (!invitado) {
                        actualizarAula("?", 0)
                    } else {
                        desconectarAula()
                    }

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
        // Anulado
    }

    private fun desconectarListeners() {

        listenerAula?.remove()
        listenerAula = null

        listenerCola?.remove()
        listenerCola = null

    }

    private fun desconectarAula() {
        invitado = false
        uid = uidPropio
        desconectarListeners()
        conectarAula()
    }

    private fun borrarAula() {

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
    }

    private fun actualizarAula(codigo: String = "?", enCola: Int = -1) {
        actualizarAula(codigo)
        actualizarAula(enCola)
    }

    private fun actualizarAula(codigo: String) {
        botonCodigoAula.text = codigo
        codigoAula = codigo
        Log.d(TAG, "Aula: $codigoAula")
    }

    private fun actualizarAula(enCola: Int) {
        if (enCola != -1)
            botonEnCola.text = enCola.toString()
        else
            botonEnCola.text = "..."
        Log.d(TAG, "Alumnos en cola: $enCola")
    }

    private fun actualizarMensaje(texto: String = "?") {
        etiquetaNombreAlumno.text = texto
    }

    private fun actualizarPIN(pin: String) {
        this.PIN = pin
    }

    // Crear el menú de acciones
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // REF: Mostrar los separadores: https://stackoverflow.com/a/51500113
        MenuCompat.setGroupDividerEnabled(menu, true);

        return true
    }

    // Menú de acciones
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val result = super.onPrepareOptionsMenu(menu)

        // REF: Dar formato a strings localizados: https://developer.android.com/guide/topics/resources/string-resource?hl=es-419#dar-formato-a-las-strings
        if (!invitado) {
            menu.findItem(R.id.etiqueta_pin).title = String.format(getString(R.string.menu_etiqueta_pin), PIN)
        } else {
            menu.findItem(R.id.etiqueta_pin).title = getString(R.string.menu_etiqueta_invitado)
        }

        menu.findItem(R.id.accion_acerca_de).setOnMenuItemClickListener {
            startActivity(Intent(this@MainActivity, AcercaDe::class.java))
            true
        }

        if (!invitado) {
            menu.findItem(R.id.accion_generar).isVisible = true
            menu.findItem(R.id.accion_generar).setOnMenuItemClickListener {
                Log.d("TurnoClase", "Generar nueva aula")
                desconectarListeners()
                borrarAula()
                true
            }
        } else {
            menu.findItem(R.id.accion_generar).isVisible = false
        }

        if (!invitado) {
            menu.findItem(R.id.accion_conectar).isVisible = true
            menu.findItem(R.id.accion_desconectar).isVisible = false

            menu.findItem(R.id.accion_conectar).setOnMenuItemClickListener {
                Log.d("TurnoClase", "Conectar a otra aula")
                dialogoConexion()
                true
            }
        } else {
            menu.findItem(R.id.accion_conectar).isVisible = false
            menu.findItem(R.id.accion_desconectar).isVisible = true

            menu.findItem(R.id.accion_desconectar).setOnMenuItemClickListener {
                Log.d("TurnoClase", "Desconectar del aula")
                desconectarAula()
                true
            }
        }

        return result
    }

    private fun dialogoConexion() {

        // REF: AlertDialog: https://stackoverflow.com/a/10904665
        // REF: Diseño personalizado: https://developer.android.com/guide/topics/ui/dialogs?hl=es-419#CustomLayout

        val builder = AlertDialog.Builder(this)

        builder.setTitle(getString(R.string.dialogo_conexion_titulo))
        builder.setMessage(getString(R.string.dialogo_conexion_mensaje))

        val vista = layoutInflater.inflate(R.layout.dialogo_conectar, null)

        builder.setView(vista)

        // Set up the input
        val inputCodigo = vista.findViewById(R.id.conectar_codigo) as EditText
        val inputPIN = vista.findViewById(R.id.conectar_pin) as EditText

        inputCodigo.filters.plus(InputFilter.AllCaps())

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.dialogo_conexion_conectar)) { _, _ ->

            Log.d(TAG, "Conectando a otra aula")

            val codigoAula = inputCodigo.text.toString()
            val PIN = inputPIN.text.toString()

            if (codigoAula != this.codigoAula) {
                buscarAula(codigoAula, PIN)
            } else {
                Log.e(TAG, "No se puede conectar a la propia aula en que estamos")
                dialogoError()
            }

        }

        builder.setNegativeButton(getString(R.string.dialogo_conexion_cancelar)) { dialog, _ ->
            Log.d(TAG, "Cancelado")
            dialog.cancel()
        }

        builder.show()
    }

    private fun buscarAula(codigo: String, pin: String) {

        Log.d(TAG, "Buscando UID del aula:$codigo:$pin")

        // Buscar el aula
        db.collection("aulas")
                .whereEqualTo("codigo", codigo.toUpperCase())
                .whereEqualTo("pin", pin)
                .limit(1)
                .get()
                .addOnCompleteListener {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Error al recuperar datos: ", it.exception)
                    } else {

                        // Comprobar que se han recuperado registros
                        if (it.result!!.count() > 0) {

                            // Accedemos al primer documento
                            val document = it.result!!.documents[0]

                            val uid = document.reference.id
                            Log.d(TAG, "Aula encontrada")

                            invitado = true
                            desconectarListeners()
                            this.uid = uid
                            conectarAula()

                        } else {
                            Log.e(TAG, "Aula no encontrada")
                            dialogoError()
                        }
                    }
                }
    }

    fun dialogoError() {

        val builder = AlertDialog.Builder(this)

        builder.setTitle(getString(R.string.dialogo_error_titulo))
        builder.setMessage(getString(R.string.dialogo_error_mensaje))

        builder.setPositiveButton(getString(R.string.dialogo_error_ok)) { _, _ ->
            Log.e(TAG, "Error de conexión")
        }

        builder.show()
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

    companion object {

        private val TAG = "MainActivity"

    }
    //endregion

}
