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
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.rd.PageIndicatorView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {

    // ID de usuario único generado por Firebase
    private var uid: String? = null

    // Conectar a otro aula
    private var invitado: Boolean = false
        set(value) {
            field = value
            pageIndicatorView?.visibility = if (value) View.INVISIBLE else View.VISIBLE
        }

    // Listeners para recibir las actualizaciones
    private var listenerAula: ListenerRegistration? = null
    private var listenerCola: ListenerRegistration? = null

    // Referencias al documento del aula y la posición en la cola
    private var refAula: DocumentReference? = null
    private var refMisAulas: CollectionReference? = null

    // Para simular el interfaz al hacer las capturas
    private var n = 2

    // Activar Firestore
    private val db = FirebaseFirestore.getInstance()
    private var mAuth: FirebaseAuth? = null

    private var functions = FirebaseFunctions.getInstance("europe-west1")

    // Datos del aula
    private var codigoAula = "..."
    private var PIN = "..."
    private var tiempoEspera = -1

    // Almacenar el número de alumnos anterior para detectar el paso de 0 a 1 y reproducir el sonido
    private var recuentoAnterior = 0

    // REF: Detectar si estamos en modo test: https://stackoverflow.com/a/40220621/5136913
    private val isRunningTest: Boolean by lazy {
        try {
            Class.forName("androidx.test.espresso.Espresso")
            Log.d(TAG, "Estamos en modo test")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private val MAX_AULAS = 16
    private var aulaActual = 0
    private var numAulas = 0

    private var adapter: ScreenSlidePagerAdapter? = null
    private var pageIndicatorView: PageIndicatorView? = null
    private var indicadorActividad: ProgressBar? = null

    // Soporte para varias aulas
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int = numAulas

        override fun getItem(position: Int): Fragment = Fragment()

        fun incrementar() {
            ocultarIndicador()
            numAulas += 1
            notifyDataSetChanged()
        }

        fun decrementar() {
            ocultarIndicador()
            if (numAulas == 2)
                viewPager.currentItem -= 1
            numAulas -= 1
            notifyDataSetChanged()
        }
    }

    fun ocultarIndicador() {
        indicadorActividad?.visibility = View.INVISIBLE
    }

    fun mostrarIndicador() {
        indicadorActividad?.visibility = View.VISIBLE
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

        // Paginador: https://github.com/romandanylyk/PageIndicatorView
        pageIndicatorView = findViewById(R.id.pageIndicatorView)

        indicadorActividad = findViewById(R.id.progressBar)

        adapter = ScreenSlidePagerAdapter(supportFragmentManager)
        viewPager.adapter = adapter

        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                if (!invitado) {
                    pageIndicatorView?.selection = position

                    aulaActual = position
                    Log.d(TAG, "Cargado aula en posición: " + aulaActual)

                    desconectarListeners()
                    conectarAula(aulaActual)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if (isRunningTest) {
            actualizarAula("BE131", 0)
            actualizarMensaje("")
        } else {

            // Limpiar el UI
            actualizarAula("...", 0)
            actualizarMensaje("")

            ocultarIndicador()

            // Iniciar sesión y conectar al aula
            mAuth = FirebaseAuth.getInstance()

            mAuth!!.signInAnonymously()
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {

                            uid = mAuth?.currentUser?.uid
                            Log.d(TAG, "Registrado como usuario con UID: $uid")

                            conectarAula()

                        } else {
                            Log.e(TAG, "Error de inicio de sesión", task.exception)
                            actualizarAula("?", 0)
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

    private fun conectarAula(posicion: Int = 0) {

        // Colección que contiene las aulas del usuario
        refMisAulas = db.collection("profesores").document(uid!!).collection("aulas")

        // Cargar el aula y si no, crearla
        refMisAulas!!.orderBy("timestamp")
                .get()
                .addOnSuccessListener { result ->

                    numAulas = result.documents.count()
                    adapter?.notifyDataSetChanged()

                    if (posicion >= 0 && posicion < numAulas) {

                        var seleccionada = result.documents[posicion]

                        if (seleccionada != null) {
                            Log.i(TAG, "Conectado a aula existente")
                            this.refAula = seleccionada.reference
                            conectarListener()
                        }
                    } else {
                        Log.i(TAG, "Creando nueva aula...")
                        crearAula()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al conectar al aula", exception)
                    actualizarAula("?", 0)
                }
    }

    private fun crearAula() {

        mostrarIndicador()

        // REF: Llamar a la función Cloud (en Android se hace en dos pasos): https://firebase.google.com/docs/functions/callable#call_the_function
        obtenerNuevoCodigo()
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        val e = task.exception
                        if (e is FirebaseFunctionsException) {
                            Log.e(TAG, e.details as String)
                        }
                    } else {

                        val codigo = task.result?.get("codigo") as String

                        // REF: Enteros aleatorios en Kotlin: https://stackoverflow.com/a/45687695
                        fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) + start

                        // Guardar el documento
                        val datos = HashMap<String, Any>()
                        datos["codigo"] = codigo
                        datos["timestamp"] = FieldValue.serverTimestamp()
                        datos["pin"] = "%04d".format((0..9999).random())
                        datos["espera"] = 5

                        // Almacenar la referencia a la nueva aula
                        refMisAulas!!.add(datos)
                                .addOnSuccessListener { nueva ->
                                    Log.d(TAG, "Aula creada")
                                    refAula = nueva
                                    adapter?.incrementar()
                                    conectarListener()
                                }
                                .addOnFailureListener { e -> Log.e(TAG, "Error al crear el aula", e) }
                    }
                })
    }

    private fun anyadirAula() {

        mostrarIndicador()

        // REF: Llamar a la función Cloud (en Android se hace en dos pasos): https://firebase.google.com/docs/functions/callable#call_the_function
        obtenerNuevoCodigo()
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        val e = task.exception
                        if (e is FirebaseFunctionsException) {
                            Log.e(TAG, e.details as String)
                        }
                    } else {

                        val codigo = task.result?.get("codigo") as String

                        // REF: Enteros aleatorios en Kotlin: https://stackoverflow.com/a/45687695
                        fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) + start

                        // Guardar el documento
                        val datos = HashMap<String, Any>()
                        datos["codigo"] = codigo
                        datos["timestamp"] = FieldValue.serverTimestamp()
                        datos["pin"] = "%04d".format((0..9999).random())
                        datos["espera"] = 5

                        // Almacenar la referencia a la nueva aula
                        refMisAulas!!.add(datos)
                                .addOnSuccessListener { nueva ->
                                    Log.d(TAG, "Aula creada")
                                    adapter?.incrementar()
                                }
                                .addOnFailureListener { e -> Log.e(TAG, "Error al crear el aula", e) }
                    }
                })
    }

    private fun obtenerNuevoCodigo(): Task<HashMap<String, String>> {

        val data = hashMapOf(
                "keepalive" to false
        )

        return functions
                .getHttpsCallable("nuevoCodigo")
                .call(data)
                .continueWith { task ->
                    task.result?.data as HashMap<String, String>
                }
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
                    tiempoEspera = (aula["espera"] as? Long ?: 5).toInt()

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

            refAula!!.collection("cola").orderBy("timestamp").get()
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

                                                            // Marcar cuando hemos atendido al alumno
                                                            val datos = HashMap<String, Any>()
                                                            datos["timestamp"] = FieldValue.serverTimestamp()

                                                            refAula!!.collection("espera").document(posicion["alumno"] as String).set(datos)
                                                                    .addOnFailureListener { e -> Log.e(TAG, "Error al añadir el documento", e) }

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
        desconectarListeners()
        conectarAula(aulaActual)
    }

    private fun borrarAulaReconectar(codigoAula: String) {

        mostrarIndicador()

        desconectarListeners()

        refMisAulas!!.whereEqualTo("codigo", codigoAula.toUpperCase())
                .get()
                .addOnSuccessListener { result ->

                    result.documents.first().reference.delete().addOnCompleteListener {
                        if (!it.isSuccessful) {
                            Log.e(TAG, "Error al borrar el aula: ", it.exception)
                        } else {
                            Log.d(TAG, "Aula borrada")
                            adapter?.decrementar()
                            conectarAula(aulaActual)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al borrar el aula", exception)
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
        if (enCola != -1) {

            if (recuentoAnterior == 0 && enCola == 1) {
                try {
                    val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(applicationContext, notification)
                    r.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            recuentoAnterior = enCola

            botonEnCola.text = enCola.toString()
        } else
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
            if (codigoAula == "?") {
                PIN = "?"
            }
            menu.findItem(R.id.etiqueta_pin).title = String.format(getString(R.string.menu_etiqueta_pin), PIN)
        } else {
            menu.findItem(R.id.etiqueta_pin).title = getString(R.string.menu_etiqueta_invitado)
        }

        menu.findItem(R.id.accion_acerca_de).setOnMenuItemClickListener {
            startActivity(Intent(this@MainActivity, AcercaDe::class.java))
            true
        }

        menu.findItem(R.id.accion_ajustes).setOnMenuItemClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            true
        }

        if (!invitado && numAulas < MAX_AULAS && codigoAula != "?") {
            menu.findItem(R.id.accion_anyadir_aula).isVisible = true
            menu.findItem(R.id.accion_anyadir_aula).setOnMenuItemClickListener {
                Log.d("TurnoClase", "Añadir aula")
                anyadirAula()
                true
            }
        } else {
            menu.findItem(R.id.accion_anyadir_aula).isVisible = false
        }

        if (!invitado && numAulas > 1 && codigoAula != "?") {
            menu.findItem(R.id.accion_borrar_aula).isVisible = true
            menu.findItem(R.id.accion_borrar_aula).setOnMenuItemClickListener {
                Log.d("TurnoClase", "Borrar aula")
                dialogoConfirmarBorrado()
                true
            }
        } else {
            menu.findItem(R.id.accion_borrar_aula).isVisible = false
        }

        if (!invitado && codigoAula != "?") {
            menu.findItem(R.id.accion_establecer_espera).isVisible = true
            menu.findItem(R.id.accion_establecer_espera).setOnMenuItemClickListener {
                Log.d("TurnoClase", "Establecer tiempo de espera")
                dialogoTiempoEspera()
                true
            }
        } else {
            menu.findItem(R.id.accion_establecer_espera).isVisible = false
        }

        if (codigoAula != "?") {
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
        } else {
            menu.findItem(R.id.accion_conectar).isVisible = false
            menu.findItem(R.id.accion_desconectar).isVisible = false
        }

        if (codigoAula == "?") {
            menu.findItem(R.id.accion_recuperar_aula).isVisible = true

            menu.findItem(R.id.accion_recuperar_aula).setOnMenuItemClickListener {
                Log.d("TurnoClase", "Recuperar aula")
                desconectarListeners()
                conectarAula()
                true
            }
        } else {
            menu.findItem(R.id.accion_recuperar_aula).isVisible = false
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

        builder.setNegativeButton(getString(R.string.dialogo_cancelar)) { dialog, _ ->
            Log.d(TAG, "Cancelado")
            dialog.cancel()
        }

        builder.show()
    }

    private fun dialogoTiempoEspera() {

        // REF: AlertDialog: https://stackoverflow.com/a/10904665
        // REF: Diseño personalizado: https://developer.android.com/guide/topics/ui/dialogs?hl=es-419#CustomLayout

        val builder = AlertDialog.Builder(this)

        builder.setTitle(getString(R.string.dialogo_establecer_espera_titulo))

        val vista = layoutInflater.inflate(R.layout.dialogo_establecer_espera, null)

        builder.setView(vista)

        // Set up the input

        val picker = vista.findViewById(R.id.picker) as NumberPicker

        val tiempos = arrayOf("1", "2", "3", "5", "10", "15", "20", "30", "45", "60")

        picker.minValue = 0
        picker.maxValue = tiempos.size - 1
        picker.displayedValues = tiempos
        picker.wrapSelectorWheel = false
        picker.value = tiempos.indexOfFirst {
            it == tiempoEspera.toString()
        }

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.dialogo_guardar)) { _, _ ->

            Log.d(TAG, "Conectando a otra aula")

            tiempoEspera = tiempos[picker.value].toInt()
            Log.d(TAG, "Establecer tiempo de espera en $tiempoEspera minutos...")

            val datos = HashMap<String, Any>()
            datos["espera"] = tiempoEspera

            refAula!!.update(datos)
                    .addOnSuccessListener {
                        Log.d(TAG, "Aula actualizada")
                        conectarListener()
                    }
                    .addOnFailureListener { e -> Log.e(TAG, "Error al actualizar el aula", e) }
        }

        builder.setNegativeButton(getString(R.string.dialogo_cancelar)) { dialog, _ ->
            Log.d(TAG, "Cancelado")
            dialog.cancel()
        }

        builder.show()
    }

    private fun dialogoConfirmarBorrado() {

        val builder = AlertDialog.Builder(this)

        builder.setTitle(getString(R.string.dialogo_confirmar_borrado_titulo))
        builder.setMessage(getString(R.string.dialogo_confirmar_borrado_mensaje))

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.dialogo_ok)) { _, _ ->
            Log.d(TAG, "Borrando el aula...")
            borrarAulaReconectar(codigoAula)
        }

        builder.setNegativeButton(getString(R.string.dialogo_cancelar)) { dialog, _ ->
            Log.d(TAG, "Cancelado")
            dialog.cancel()
        }

        builder.show()
    }

    private fun buscarAula(codigo: String, pin: String) {

        Log.d(TAG, "Buscando UID del aula:$codigo:$pin")

        // Buscar el aula
        db.collectionGroup("aulas")
                .whereEqualTo("codigo", codigo.toUpperCase())
                .whereEqualTo("pin", pin)
                .get()
                .addOnCompleteListener {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Error al recuperar datos: ", it.exception)
                    } else {

                        // Comprobar que se han recuperado registros
                        if (it.result!!.count() > 0) {
                            // Accedemos al primer documento
                            val document = it.result!!.documents.first()

                            Log.d(TAG, "Aula encontrada")

                            desconectarListeners()
                            invitado = true
                            refAula = document.reference
                            conectarListener()
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

        builder.setPositiveButton(getString(R.string.dialogo_ok)) { _, _ ->
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
