//
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
//
//  TurnoViewController.swift
//  TurnoClase
//
//  Created by widemos on 29/6/15.
//

import UIKit

import XCGLogger

import Firebase
import FirebaseFirestore

import TurnoClaseShared

class TurnoViewController: UIViewController {

    // Datos que introduce el usuario
    var codigoAula: String!
    var nombreUsuario: String!

    // ID de usuario único generado por Firebase
    var uid: String!

    // Listeners para recibir las actualizaciones
    var listenerAula: ListenerRegistration!
    var listenerCola: ListenerRegistration!
    var listenerPosicion: ListenerRegistration!

    // Pedir turno una sola vez
    var pedirTurno = true

    // Controlar si ya hemos sido atendidos para poder mostrar el mensaje
    var atendido = false

    // Referencias al documento del aula y la posición en la cola
    var refAula: DocumentReference!
    var refPosicion: DocumentReference!

    // Para simular el interfaz al hacer las capturas
    var n = 2

    // UI
    @IBOutlet weak var etiquetaAula: UILabel!
    @IBOutlet weak var etiquetaMensaje: UILabel!
    @IBOutlet weak var etiquetaMinutos: UILabel!
    @IBOutlet weak var etiquetaSegundos: UILabel!
    @IBOutlet weak var contenedorCronometro: UIView!
    @IBOutlet weak var botonActualizar: UIButton!

    // REF: Barra de navegación en color claro: https://stackoverflow.com/a/52443917/5136913
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        log.debug("Valores recibidos: \(codigoAula ??? "[Aula desconocida]") - \(nombreUsuario ??? "[Usuario desconocido]")")

        log.info("Iniciando la aplicación...")

        // Detectar si estamos haciendo capturas de pantalla para la App Store
        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            self.actualizarAula(codigo: "BE131", mensaje: "2")
        } else {

            // Registrarse como usuario anónimo
            Auth.auth().signInAnonymously() { (result, error) in

                if let resultado = result {

                    self.uid = resultado.user.uid
                    log.info("Registrado como usuario con UID: \(self.uid ??? "[Desconocido]")")

                    self.actualizarAlumno()
                    self.encolarAlumno()

                } else {
                    log.error("Error de inicio de sesión: \(error!.localizedDescription)")
                }
            }
        }
    }

    fileprivate func actualizarAlumno() {

        // Guarda el nombre en el UID de este usuario. Si existe, lo sobreescribe.
        db.collection("alumnos").document(self.uid).setData([
            "nombre": self.nombreUsuario!
        ], merge: true) { error in
            if let error = error {
                log.error("Error al actualizar el alumno: \(error.localizedDescription)")
            } else {
                log.info("Alumno actualizado")
            }
        }
    }

    fileprivate func encolarAlumno() {

        // Buscar el aula
        db.collection("aulas").whereField("codigo", isEqualTo: self.codigoAula!).limit(to: 1).getDocuments() { (querySnapshot, error) in

            if let error = error {
                log.error("Error al recuperar datos: \(error.localizedDescription)")
            } else {

                // Comprobar que se han recuperado registros
                if querySnapshot!.documents.count > 0 {

                    // Accedemos al primer documento
                    let document = querySnapshot!.documents[0]
                    log.info("Conectado a aula existente")

                    self.conectarListenerAula(document)
                } else {
                    log.info("Aula no encontrada")
                    self.actualizarAula(codigo: "?", mensaje: "")
                }
            }
        }
    }

    fileprivate func conectarListenerAula(_ document: QueryDocumentSnapshot) {

        // Conectar el listener del aula para detectar cambios (por ejemplo, que se borra)
        if self.listenerAula == nil {
            self.listenerAula = document.reference.addSnapshotListener { documentSnapshot, error in

                if (documentSnapshot?.exists)! && documentSnapshot?.data()?["codigo"] as? String == self.codigoAula {
                    self.refAula = documentSnapshot?.reference
                    self.conectarListenerCola()

                    let tiempoEspera = documentSnapshot?.data()?["espera"] as? Int ?? 5
                    self.segundosEspera = tiempoEspera * 60
                } else {
                    log.info("El aula ha desaparecido")
                    self.desconectarListeners()
                    self.abandonarCola()
                }
            }
        }
    }

    fileprivate func conectarListenerCola() {

        if self.listenerCola == nil {
            self.listenerCola = self.refAula.collection("cola").addSnapshotListener { querySnapshot, error in

                if let error = error {
                    log.error("Error al recuperar datos: \(error.localizedDescription)")
                } else {
                    self.buscarAlumnoEnCola()
                }
            }
        }
    }

    fileprivate func conectarListenerPosicion(_ refPosicion: DocumentReference) {

        if self.listenerPosicion == nil {
            self.listenerPosicion = refPosicion.addSnapshotListener { documentSnapshot, error in

                if !(documentSnapshot?.exists)! {
                    self.atendido = true
                    log.info("Nos han borrado de la cola")
                }
            }
        }
    }

    fileprivate func buscarAlumnoEnCola() {

        self.refAula.collection("cola").whereField("alumno", isEqualTo: self.uid!).limit(to: 1).getDocuments() { (resultados, error) in

            if let error = error {
                log.error("Error al recuperar datos: \(error.localizedDescription)")
            } else {
                self.pedirTurno(resultados)
            }
        }
    }

    fileprivate func pedirTurno(_ querySnapshot: QuerySnapshot?) {

        if self.pedirTurno && querySnapshot!.documents.count == 0 {
            self.pedirTurno = false

            log.info("Alumno no encontrado, lo añadimos")

            self.recuperarUltimaPeticion() {

                if !(self.tiempoEspera() > 0) {
                    self.ocultarCronometro()
                    self.reiniciarCronometro()

                    self.refPosicion = self.refAula.collection("cola").addDocument(data: [
                        "alumno": self.uid!,
                        "timestamp": FieldValue.serverTimestamp()
                    ]) { error in
                        if let error = error {
                            log.error("Error al añadir el documento: \(error.localizedDescription)")
                        } else {
                            self.conectarListenerPosicion(self.refPosicion)
                            self.actualizarPantalla()
                        }
                    }
                } else {
                    self.actualizarAula(codigo: self.codigoAula, mensaje: NSLocalizedString("ESPERA", comment: "Mensaje de que te toca esperar"))
                    self.iniciarCronometro()
                    self.mostrarCronometro()
                }
            }

        } else if querySnapshot!.documents.count > 0 {
            log.error("Alumno encontrado, ya está en la cola")
            self.refPosicion = querySnapshot!.documents[0].reference
            self.conectarListenerPosicion(self.refPosicion)
            self.actualizarPantalla()

        } else if querySnapshot!.documents.count == 0 {
            log.info("La cola se ha vaciado")

            self.recuperarUltimaPeticion() {

                if self.atendido && !(self.tiempoEspera() > 0) {
                    self.ocultarCronometro()
                    self.reiniciarCronometro()
                    self.actualizarAula(codigo: self.codigoAula, mensaje: NSLocalizedString("VOLVER_A_EMPEZAR", comment: "Mensaje de que ya nos han atendido"))
                } else {
                    self.actualizarAula(codigo: self.codigoAula, mensaje: NSLocalizedString("ESPERA", comment: "Mensaje de que te toca esperar"))
                    self.iniciarCronometro()
                    self.mostrarCronometro()
                }
            }
        }
    }

    fileprivate func actualizarAula(codigo codigoAula: String, mensaje textoMensaje: String? = nil) {
        etiquetaAula.text = codigoAula
        log.info("Código de aula: \(codigoAula)")
        if textoMensaje != nil {
            actualizarAula(mensaje: textoMensaje!)
        }
    }

    fileprivate func actualizarAula(mensaje: String) {
        etiquetaMensaje.text = mensaje
        log.info("Mensaje: \(mensaje)")
    }

    fileprivate func actualizarPantalla() {

        if self.refAula != nil && self.refPosicion != nil {

            // Mostramos el código en la pantalla
            self.actualizarAula(codigo: self.codigoAula)

            self.refPosicion.getDocument { (document, error) in

                if let alumno = document?.data() {

                    self.refAula.collection("cola").whereField("timestamp", isLessThanOrEqualTo: alumno["timestamp"]!).getDocuments() { (querySnapshot, error) in
                        if let error = error {
                            log.error("Error al recuperar datos: \(error.localizedDescription)")
                        } else {

                            let posicion = querySnapshot!.documents.count
                            log.info("Posicion en la cola: \(posicion)")

                            if posicion > 1 {
                                self.actualizarAula(mensaje: String(posicion - 1))
                            } else if posicion == 1 {
                                self.actualizarAula(mensaje: NSLocalizedString("ES_TU_TURNO", comment: "Mensaje de que ha llegado el turno"))
                            }

                        }
                    }
                }
            }

        } else {
            self.actualizarAula(codigo: "?", mensaje: "")
            log.error("No hay referencia al aula")
        }
    }

    fileprivate func cerrarPantalla() {
        // Volver a la pantalla inicial
        self.dismiss(animated: true, completion: { })
    }

    fileprivate func desconectarListeners() {

        if self.listenerAula != nil {
            self.listenerAula.remove()
            self.listenerAula = nil
        }

        if self.listenerCola != nil {
            self.listenerCola.remove()
            self.listenerCola = nil
        }

        if self.listenerPosicion != nil {
            self.listenerPosicion.remove()
            self.listenerPosicion = nil
        }

    }

    fileprivate func abandonarCola() {

        // Nos borramos de la cola
        if self.refPosicion != nil {
            self.refPosicion.delete() { _ in
                self.cerrarPantalla()
            }
        } else {
            self.cerrarPantalla()
        }
    }

    @IBAction func botonCancelar(_ sender: UIButton) {

        efectoBoton(sender)

        log.info("Cancelando...")

        desconectarListeners()
        abandonarCola()

        reiniciarCronometro()
    }

    @IBAction func botonActualizar(_ sender: UIButton) {

        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {

            // Simulamos el interfaz en modo test
            if n > 0 {
                actualizarAula(mensaje: String(n))
            } else if n == 0 {
                actualizarAula(mensaje: NSLocalizedString("ES_TU_TURNO", comment: "Mensaje de que ha llegado el turno"))
            } else {
                actualizarAula(mensaje: NSLocalizedString("VOLVER_A_EMPEZAR", comment: "Mensaje de que ya nos han atendido"))
            }
            n -= 1

        } else if self.atendido {

            efectoBoton(sender)

            // Volvemos a pedir turno
            log.info("Pidiendo nuevo turno")

            self.desconectarListeners()
            self.atendido = false
            self.pedirTurno = true
            self.encolarAlumno()

        } else {

            efectoBoton(sender)

            // No hay que hacer nada
            log.info("Ya tenemos turno")
        }

    }

    // MARK: Funciones exclusivas de la versión iOS

    fileprivate func efectoBoton(_ sender: UIButton) {
        fadeIn(sender)
        feedbackTactil()
    }

    @IBAction func fadeOut(_ sender: UIButton) {

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if !UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {

            // Difuminar un botón
            UIView.animate(withDuration: 0.1,
                           delay: 0,
                           options: UIView.AnimationOptions.curveLinear.intersection(UIView.AnimationOptions.allowUserInteraction).intersection(UIView.AnimationOptions.beginFromCurrentState),
                           animations: {
                               sender.alpha = 0.15
                           }, completion: nil)
        }
    }

    @IBAction func fadeIn(_ sender: UIButton) {

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if !UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {

            // Restaurar el botón
            UIView.animate(withDuration: 0.3,
                           delay: 0,
                           options: UIView.AnimationOptions.curveLinear.intersection(UIView.AnimationOptions.allowUserInteraction).intersection(UIView.AnimationOptions.beginFromCurrentState),
                           animations: {
                               sender.alpha = 1
                           }, completion: nil)
        }
    }

    var timer: Timer?
    var ultimaPeticion: Date!
    var segundosEspera = 10

    func mostrarCronometro() {
        botonActualizar.isHidden = true
        contenedorCronometro.isHidden = false
    }

    func ocultarCronometro() {
        botonActualizar.isHidden = false
        contenedorCronometro.isHidden = true
    }

    func iniciarCronometro() {
        actualizarCronometro()
        timer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(actualizarCronometro), userInfo: nil, repeats: true)
    }

    func reiniciarCronometro() {
        timer?.invalidate()
    }

    func tiempoEspera() -> Int {
        if(ultimaPeticion != nil) {
            let segundosTranscurridos = Int(Date().timeIntervalSince(ultimaPeticion))
            return segundosEspera - segundosTranscurridos
        } else {
            return -1
        }
    }

    @objc func actualizarCronometro()
    {
        let tiempoRestante = tiempoEspera()

        if(tiempoRestante >= 0) {
            let segundosRestantes = tiempoRestante % 60
            let minutosRestantes = tiempoRestante / 60
            etiquetaMinutos.text = String(format: "%02d", minutosRestantes)
            etiquetaSegundos.text = String(format: "%02d", segundosRestantes)
        } else {
            ocultarCronometro()
            reiniciarCronometro()
            self.actualizarAula(codigo: self.codigoAula, mensaje: NSLocalizedString("VOLVER_A_EMPEZAR", comment: "Mensaje de que ya nos han atendido"))
            self.atendido = true
        }
    }

    func recuperarUltimaPeticion(completado: @escaping () -> Void) {

        self.refAula.collection("espera").document(self.uid!).getDocument() { (document, error) in

            if let error = error {
                log.error("Error al recuperar datos: \(error.localizedDescription)")
            } else {
                if let datos = document?.data() {
                    let stamp = datos["timestamp"] as? Timestamp
                    self.ultimaPeticion = stamp?.dateValue()
                }
                completado()
            }
        }
    }
}
