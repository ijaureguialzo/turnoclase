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

class TurnoViewController: UIViewController {

    // Datos que introduce el usuario
    var codigoAula: String!
    var nombreUsuario: String!

    // ID de usuario único generado por Firebase
    var uid: String!

    // Listeners para recibir las actualizaciones
    var listenerAula: ListenerRegistration!
    var listenerCola: ListenerRegistration!

    // Pedir turno una sola vez
    var pedirTurno = true

    // Referencias al documento del aula y la posición en la cola
    var refAula: DocumentReference!
    var refPosicion: DocumentReference!

    // Para simular el interfaz al hacer las capturas
    var n = 2

    // UI
    @IBOutlet weak var etiquetaAula: UILabel!
    @IBOutlet weak var etiquetaNumero: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        log.debug("Valores recibidos: \(codigoAula ??? "[Aula desconocida]") - \(nombreUsuario ??? "[Usuario desconocido]")")

        log.info("Iniciando la aplicación...")

        // Detectar si estamos haciendo capturas de pantalla para la App Store
        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            etiquetaAula.text = "BE131"
            etiquetaNumero.text = "2"
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
            "nombre": self.nombreUsuario
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
        db.collection("aulas").whereField("codigo", isEqualTo: self.codigoAula).limit(to: 1).getDocuments() { (querySnapshot, error) in

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
                    self.etiquetaAula.text = "?"
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
                } else {
                    log.info("El aula ha desaparecido")
                    self.desconectarListeners()
                    self.cerrarPantalla()
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

    fileprivate func buscarAlumnoEnCola() {

        self.refAula.collection("cola").whereField("alumno", isEqualTo: self.uid).limit(to: 1).getDocuments() { (resultados, error) in

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

            self.refPosicion = self.refAula.collection("cola").addDocument(data: [
                "alumno": self.uid,
                "timestamp": FieldValue.serverTimestamp()
                ]) { error in
                if let error = error {
                    log.error("Error al añadir el documento: \(error.localizedDescription)")
                } else {
                    self.actualizarPantalla()
                }
            }

        } else if querySnapshot!.documents.count > 0 {
            log.error("Alumno encontrado, ya está en la cola")
            self.pedirTurno = false
            self.refPosicion = querySnapshot!.documents[0].reference
            self.actualizarPantalla()

        } else if querySnapshot!.documents.count == 0 {
            log.info("La cola se ha vaciado")
            self.etiquetaNumero.text = ""
        }
    }

    fileprivate func actualizarPantalla() {

        if self.refAula != nil && self.refPosicion != nil {

            // Mostramos el código en la pantalla
            self.etiquetaAula.text = self.codigoAula

            self.refPosicion.getDocument { (document, error) in

                if let alumno = document?.data() {

                    self.refAula.collection("cola").whereField("timestamp", isLessThanOrEqualTo: alumno["timestamp"]!).getDocuments() { (querySnapshot, error) in
                        if let error = error {
                            log.error("Error al recuperar datos: \(error.localizedDescription)")
                        } else {

                            let posicion = querySnapshot!.documents.count
                            log.info("Posicion en la cola: \(posicion)")

                            if posicion > 1 {
                                self.etiquetaNumero.text = String(posicion - 1)
                            } else if posicion == 1 {
                                self.etiquetaNumero.text = NSLocalizedString("ES_TU_TURNO", comment: "Mensaje de que ha llegado el turno")
                            } else {
                                self.etiquetaNumero.text = ""
                            }

                        }
                    }
                }
            }

        } else {
            self.etiquetaAula.text = "?"
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
    }

    @IBAction func botonCancelar(_ sender: UIButton) {

        fadeIn(sender)
        log.info("Cancelando...")

        // Nos borramos de la cola
        if self.refAula != nil && self.refPosicion != nil {
            self.refPosicion.delete()
        }

        desconectarListeners()
        cerrarPantalla()
    }

    @IBAction func botonActualizar(_ sender: UIButton) {
        fadeIn(sender)
        log.info("Este botón ya no hace nada :)")

        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            if(n > 0) {
                self.etiquetaNumero.text = "\(n)"
            } else if (n == 0) {
                self.etiquetaNumero.text = NSLocalizedString("ES_TU_TURNO", comment: "Mensaje de que ha llegado el turno")
            } else {
                self.etiquetaNumero.text = ""
            }
            n -= 1
        }
    }

    @IBAction func fadeOut(_ sender: UIButton) {

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if !UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {

            // Difuminar
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

            // Restaurar
            UIView.animate(withDuration: 0.3,
                delay: 0,
                options: UIView.AnimationOptions.curveLinear.intersection(UIView.AnimationOptions.allowUserInteraction).intersection(UIView.AnimationOptions.beginFromCurrentState),
                animations: {
                    sender.alpha = 1
                }, completion: nil)
        }
    }

}
