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
import android.os.CountDownTimer
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_alumno_turno.*
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit

class AlumnoTurno : AppCompatActivity() {

    // Parámetros que llegan en el Intent
    private var codigoAula: String? = null
    private var nombreUsuario: String? = null

    // ID de usuario único generado por Firebase
    private var uid: String? = null

    // Listeners para recibir las actualizaciones
    private var listenerAula: ListenerRegistration? = null
    private var listenerCola: ListenerRegistration? = null
    private var listenerPosicion: ListenerRegistration? = null

    // Pedir turno una sola vez
    // App.pedirTurno = true

    // Controlar si ya hemos sido atendidos para poder mostrar el mensaje
    // App.atendido = false

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
            Class.forName("androidx.test.espresso.Espresso")
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

        // Ocultar la barra de título en horizontal
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE))
            supportActionBar!!.hide()
        else
            supportActionBar!!.show()

        // Cargar el layout
        setContentView(R.layout.activity_alumno_turno)

        Log.d(TAG, "Iniciando la aplicación...")

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if (isRunningTest) {
            // No usar la llamada a la función actualizar, no vuelve a tiempo para el test
            etiquetaAula.text = "BE131"
            etiquetaMensaje.text = "2"
            mostrarBoton()
        } else {

            // Extraer los parámetros desde el Intent
            val intent = intent
            this.codigoAula = intent.getStringExtra("CODIGO_AULA")
            this.nombreUsuario = intent.getStringExtra("NOMBRE_USUARIO")

            // Registrarse como usuario anónimo
            this.mAuth = FirebaseAuth.getInstance()

            this.mAuth!!.signInAnonymously()
                    .addOnCompleteListener(this) {
                        if (it.isSuccessful) {
                            uid = mAuth?.currentUser?.uid
                            Log.d(TAG, "Registrado como usuario con UID: $uid")

                            actualizarAlumno()

                            encolarAlumno()
                        } else {
                            Log.e(TAG, "Error de inicio de sesión", it.exception)
                            actualizarAula("?", resources.getString(R.string.MENSAJE_ERROR))
                            mostrarError()
                        }
                    }
        }

        // Evento del botón Actualizar
        botonActualizar.setOnClickListener {
            botonActualizar()
        }

        // Evento del botón Cancelar
        botonCancelar.setOnClickListener { botonCancelar() }

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
                        if (it.result!!.count() > 0) {

                            // Accedemos al primer documento
                            val document = it.result!!.documents[0]
                            Log.d(TAG, "Conectado a aula existente")

                            conectarListenerAula(document)
                        } else {
                            Log.e(TAG, "Aula no encontrada")
                            actualizarAula("?", resources.getString(R.string.MENSAJE_ERROR))
                            mostrarError()
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

                    val tiempoEspera = snapshot.data!!["espera"] as? Long ?: 5
                    segundosEspera = tiempoEspera.toInt() * 60
                } else {
                    Log.d(TAG, "El aula ha desaparecido")
                    desconectarListeners()
                    abandonarCola()
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

    private fun conectarListenerPosicion(refPosicion: DocumentReference) {

        if (listenerPosicion == null) {
            listenerPosicion = refPosicion.addSnapshotListener { snapshot, _ ->

                if (snapshot != null && !snapshot.exists()) {
                    App.atendido = true
                    Log.d(TAG, "Nos han borrado de la cola")
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
                        pedirTurno(it.result!!)
                    }
                }
    }

    private fun pedirTurno(querySnapshot: QuerySnapshot) {

        if (App.pedirTurno && querySnapshot.documents.count() == 0) {
            App.pedirTurno = false

            Log.d(TAG, "Alumno no encontrado, lo añadimos")

            recuperarUltimaPeticion {

                if (tiempoEspera() <= 0) {
                    mostrarBoton()
                    reiniciarCronometro()

                    val datos = HashMap<String, Any>()
                    datos["alumno"] = uid!!
                    datos["timestamp"] = FieldValue.serverTimestamp()

                    refAula!!.collection("cola").add(datos)
                            .addOnSuccessListener { documentReference ->
                                refPosicion = documentReference
                                conectarListenerPosicion(refPosicion!!)
                                actualizarPantalla()
                            }
                            .addOnFailureListener { e -> Log.e(TAG, "Error al añadir el documento", e) }

                } else {
                    actualizarAula(codigoAula!!, resources.getString(R.string.ESPERA))
                    mostrarCronometro()
                    iniciarCronometro()
                }
            }

        } else if (querySnapshot.documents.count() > 0) {
            Log.e(TAG, "Alumno encontrado, ya está en la cola")
            refPosicion = querySnapshot.documents[0].reference
            conectarListenerPosicion(refPosicion!!)
            actualizarPantalla()
            mostrarBoton()

        } else if (querySnapshot.documents.count() == 0) {
            Log.d(TAG, "La cola se ha vaciado")

            recuperarUltimaPeticion {

                if (App.atendido && tiempoEspera() <= 0) {
                    mostrarBoton()
                    reiniciarCronometro()
                    actualizarAula(codigoAula!!, resources.getString(R.string.VOLVER_A_EMPEZAR))
                } else {
                    actualizarAula(codigoAula!!, resources.getString(R.string.ESPERA))
                    mostrarCronometro()
                    iniciarCronometro()
                }
            }
        }
    }

    private fun actualizarAula(codigo: String, mensaje: String? = null) {
        etiquetaAula.text = codigo
        Log.d(TAG, "Código de aula: $codigo")
        if (mensaje != null) {
            actualizarAula(mensaje)
        }
    }

    private fun actualizarAula(mensaje: String) {

        if (mensaje.contains('\n')) {
            etiquetaMensaje.maxLines = 2
        } else {
            etiquetaMensaje.maxLines = 1
        }

        etiquetaMensaje.text = mensaje
        Log.d(TAG, "Mensaje: $mensaje")
    }

    private fun actualizarPantalla() {

        if (refAula != null && refPosicion != null) {

            // Mostramos el código en la pantalla
            actualizarAula(codigo = codigoAula!!)

            refPosicion!!.get().addOnCompleteListener { document ->

                val alumno = document.result

                alumno?.get("timestamp")?.let {
                    refAula?.collection("cola")
                            ?.whereLessThanOrEqualTo("timestamp", it)
                            ?.get()
                            ?.addOnCompleteListener {
                                if (!it.isSuccessful) {
                                    Log.e(TAG, "Error al recuperar datos: ", it.exception)
                                } else {

                                    val posicion = it.result!!.count()
                                    Log.d(TAG, "Posicion en la cola: $posicion")

                                    when {
                                        posicion > 1 -> actualizarAula((posicion - 1).toString())
                                        posicion == 1 -> actualizarAula(resources.getString(R.string.ES_TU_TURNO))
                                    }

                                }
                            }
                }
            }
        } else {
            Log.e(TAG, "No hay referencia al aula")
            actualizarAula("?", resources.getString(R.string.MENSAJE_ERROR))
            mostrarError()
        }
    }

    private fun cerrarPantalla() {
        numeroTurno = ""
        App.pedirTurno = true
        App.atendido = false

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

        if (listenerPosicion != null) {
            listenerPosicion?.remove()
            listenerPosicion = null
        }

    }

    private fun abandonarCola() {

        // Nos borramos de la cola
        if (refPosicion != null) {
            refPosicion!!.delete().addOnCompleteListener {
                cerrarPantalla()
            }
        } else {
            cerrarPantalla()
        }
    }

    private fun botonCancelar() {

        Log.d(TAG, "Cancelando...")

        desconectarListeners()
        abandonarCola()
        reiniciarCronometro()
    }

    private fun botonActualizar() {

        if (isRunningTest) {

            // Simulamos el interfaz en modo test
            when {
                n > 0 -> actualizarAula(n.toString())
                n == 0 -> actualizarAula(resources.getString(R.string.ES_TU_TURNO))
                else -> actualizarAula(resources.getString(R.string.VOLVER_A_EMPEZAR))
            }
            n -= 1

        } else if (App.atendido) {

            // Volvemos a pedir turno
            Log.d(TAG, "Pidiendo nuevo turno")

            desconectarListeners()
            App.atendido = false
            App.pedirTurno = true
            encolarAlumno()

        } else {

            // No hay que hacer nada
            Log.d(TAG, "Ya tenemos turno")
        }
    }

    //region Funciones exclusivas de la versión Android

    // Detectamos el botón de retorno del teléfono y quitamos al usuario de la cola
    override fun onBackPressed() {
        botonCancelar()
        super.onBackPressed()
    }

    private fun animarBoton(event: MotionEvent, v: View?, nombre: String) {

        if (!isRunningTest) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.v("TurnoClase", "DOWN del botón $nombre...")

                // Difuminar
                val anim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.15f)
                anim.duration = 100
                anim.start()
            } else if (event.action == MotionEvent.ACTION_UP) {
                Log.v("TurnoClase", "UP del botón $nombre...")

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
    //endregion

    var timer: CountDownTimer? = null
    var ultimaPeticion: LocalDateTime? = null
    var segundosEspera = 10

    fun mostrarCronometro() {
        actualizarCronometro()
        botonActualizar.visibility = View.INVISIBLE
        etiquetaCronometro.visibility = View.VISIBLE
    }

    fun mostrarBoton() {
        etiquetaCronometro.visibility = View.INVISIBLE
        botonActualizar.visibility = View.VISIBLE
    }

    fun mostrarError() {
        botonActualizar.visibility = View.INVISIBLE
        etiquetaCronometro.visibility = View.INVISIBLE
        etiquetaError.visibility = View.VISIBLE
    }

    fun iniciarCronometro() {

        if (timer == null) {
            timer = object : CountDownTimer((tiempoEspera() * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    actualizarCronometro()
                }

                override fun onFinish() {
                    actualizarCronometro()

                    mostrarBoton()
                    reiniciarCronometro()

                    App.atendido = true
                    actualizarAula(codigoAula!!, resources.getString(R.string.VOLVER_A_EMPEZAR))
                }
            }
            timer?.start()
        }
    }

    fun reiniciarCronometro() {
        timer?.cancel()
        timer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        reiniciarCronometro()
    }

    fun tiempoEspera(): Int {

        if (ultimaPeticion != null) {

            // REF: Librería de fechas java.time para Android antiguos: https://github.com/JakeWharton/ThreeTenABP
            // REF: Diferencia entre fechas: https://www.baeldung.com/java-date-difference

            val segundosTranscurridos = ChronoUnit.SECONDS.between(ultimaPeticion, LocalDateTime.now()).toInt()
            return segundosEspera - segundosTranscurridos

        } else {
            return -1
        }
    }

    fun actualizarCronometro() {
        val tiempoRestante = tiempoEspera()

        if (tiempoRestante >= 0) {
            val segundosRestantes = tiempoRestante % 60
            val minutosRestantes = tiempoRestante / 60
            etiquetaCronometro.text = "%02d:%02d".format(minutosRestantes, segundosRestantes)
        }
    }

    fun recuperarUltimaPeticion(completado: () -> Unit) {

        refAula!!.collection("espera").document(uid!!)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val datos = it.result!!
                        if (datos.exists()) {

                            // REF: Librería de fechas java.time para Android antiguos: https://github.com/JakeWharton/ThreeTenABP
                            // REF: Fechas con Kotlin y Firestore: https://code.luasoftware.com/tutorials/google-cloud-firestore/understanding-date-in-firestore/

                            val timestamp = datos["timestamp"] as Timestamp
                            val milliseconds = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
                            val tz = ZoneId.systemDefault()
                            ultimaPeticion = LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), tz)

                            Log.d(TAG, "Última petición: $ultimaPeticion")
                        }
                        completado()
                    }
                }
    }
}
