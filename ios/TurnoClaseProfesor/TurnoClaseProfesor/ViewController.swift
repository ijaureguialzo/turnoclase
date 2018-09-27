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
//  ViewController.swift
//  TurnoClaseProfesor
//
//  Created by widemos on 19/6/15.
//

import UIKit

import XCGLogger

import Firebase
import FirebaseFirestore

class ViewController: UIViewController {

    // Objeto aula que contiene la cola de alumnos
    var aula: Aula!
    var uid: String!

    // Listeners para recibir las actualizaciones
    var listenerAula: ListenerRegistration!
    var listenerCola: ListenerRegistration!

    // Referencias al documento del aula y la posición en la cola
    var refAula: DocumentReference!
    var refCola: DocumentReference!

    // Outlets para el interfaz de usuario
    @IBOutlet weak var etiquetaNombreAlumno: UILabel!
    @IBOutlet weak var etiquetaBotonEnCola: UIButton!
    @IBOutlet weak var etiquetaBotonCodigoAula: UIButton!

    // Para simular el interfaz al hacer las capturas
    var n = 2

    override func viewDidLoad() {
        super.viewDidLoad()

        etiquetaBotonCodigoAula.titleLabel?.adjustsFontSizeToFitWidth = true

        log.info("Iniciando la aplicación...")

        //try? Auth.auth().signOut()

        // Registrarse como usuario anónimo
        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            self.etiquetaBotonCodigoAula.setTitle("BE131", for: UIControl.State())
            self.etiquetaBotonEnCola.setTitle("2", for: UIControl.State())
            self.etiquetaNombreAlumno.text = ""
        } else {
            Auth.auth().signInAnonymously() { (result, error) in

                if let resultado = result {

                    self.uid = resultado.user.uid
                    log.info("Registrado como usuario con UID: \(self.uid ??? "[Desconocido]")")

                    db.collection("aulas").document(self.uid).getDocument() { (document, error) in

                        if !(document?.exists)! {
                            log.info("Creando nueva aula...")

                            self.refAula = db.collection("aulas").document(self.uid)

                            self.refAula.setData([
                                "timestamp": FieldValue.serverTimestamp()
                                ]) { error in
                                if let error = error {
                                    log.error("Error al crear el aula: \(error.localizedDescription)")
                                } else {
                                    log.info("Aula creada")
                                    self.conectarListener()
                                }
                            }
                        } else {
                            log.info("Conectado a aula existente")
                            self.refAula = document?.reference
                            self.conectarListener()
                        }
                    }

                } else {
                    log.error("Error de inicio de sesión: \(error!.localizedDescription)")
                }
            }
        }
    }

    func conectarListener() {

        if self.listenerAula == nil && self.refAula != nil {
            self.listenerAula = self.refAula
                .addSnapshotListener { documentSnapshot, error in

                    if (documentSnapshot?.exists)! {

                        if let aula = documentSnapshot?.data() {

                            log.info("Actualizando datos del aula...")

                            let codigoAula = aula["codigo"] as? String ?? "?"
                            log.debug("Aula: \(codigoAula ??? "[Desconocida]")")

                            self.actualizarCodigo(codigoAula)

                            if self.listenerCola == nil {
                                self.listenerCola = db.collection("aulas").document(self.uid)
                                    .collection("cola").addSnapshotListener { querySnapshot, error in

                                        if let error = error {
                                            log.error("Error al recuperar datos: \(error.localizedDescription)")
                                        } else {
                                            self.actualizarRecuento(querySnapshot!.documents.count)
                                        }
                                }
                            }
                        }
                    } else {
                        log.info("El aula ha desaparecido")
                        self.actualizarPantalla("?", recuento: 0)
                    }
            }
        }
    }

    fileprivate func actualizarCodigo(_ codigo: String) {
        // Mostramos el código en la pantalla
        self.etiquetaBotonCodigoAula.setTitle(codigo, for: UIControl.State())
    }

    fileprivate func actualizarRecuento(_ recuento: Int) {
        // Mostrar el recuento
        self.etiquetaBotonEnCola.setTitle("\(recuento)", for: UIControl.State())
        log.info("Alumnos en cola: \(recuento)")
    }

    func actualizarPantalla(_ codigo: String, recuento: Int) {
        actualizarCodigo(codigo)
        actualizarRecuento(recuento)
    }

    @IBAction func botonCodigoAulaCorto(_ sender: UIButton) {

        log.info("Vaciando el aula...")

        if self.aula != nil {

            self.aula.cola = []

            db.collection("aulas").document(self.uid).setData([
                "cola": []
                ], merge: true) { error in
                if let error = error {
                    log.error("Error al actualizar el aula: \(error.localizedDescription)")
                } else {
                    log.info("Aula vaciada")
                    self.etiquetaNombreAlumno.text = ""
                    self.conectarListener()
                }
            }

        } else {
            log.error("No hay objeto aula")
        }

    }

    @IBAction func botonCodigoAulaLargo(_ sender: UILongPressGestureRecognizer) {

        if (sender.state == UIGestureRecognizer.State.ended) {

            log.info("Generando nueva aula...")

            if self.aula != nil {

                self.aula = Aula(codigo: "?", cola: [])

                listenerAula.remove()
                listenerAula = nil

                db.collection("aulas").document(self.uid).delete() { error in
                    if let error = error {
                        log.error("Error al borrar el aula: \(error.localizedDescription)")
                    } else {
                        log.info("Aula borrada")

                        db.collection("aulas").document(self.uid).setData([
                            "cola": []
                            ]) { error in
                            if let error = error {
                                log.error("Error al crear el aula: \(error.localizedDescription)")
                            } else {
                                log.info("Aula creada")
                                self.etiquetaNombreAlumno.text = ""
                                self.conectarListener()
                            }
                        }
                    }
                }

            } else {
                log.error("No hay objeto aula")
            }

        }
    }

    @IBAction func botonEnCola(_ sender: UIButton) {
        log.info("Este botón ya no hace nada :)")

        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            n -= 1
            if(n >= 0) {
                self.etiquetaBotonEnCola.setTitle("\(n)", for: UIControl.State())
                self.etiquetaNombreAlumno.text = nombreAleatorio()
            } else {
                self.etiquetaBotonEnCola.setTitle("0", for: UIControl.State())
                self.etiquetaNombreAlumno.text = ""
            }
        }

    }

    @IBAction func botonSiguiente(_ sender: UIButton) {

        fadeIn(sender)

        log.info("Mostrando el siguiente alumno...")

        if self.aula != nil {

            if self.aula.cola.count > 0 {

                let siguiente = self.aula.cola.remove(at: 0)

                log.debug("Siguiente: \(siguiente)")

                // Cargar el alumno
                db.collection("alumnos").document(siguiente).getDocument { (document, error) in
                    if let document = document {
                        if document.exists {
                            if let alumno = document.data() {
                                self.etiquetaNombreAlumno.text = alumno["nombre"] as? String ?? "?"
                            }
                        } else {
                            log.error("El alumno no existe")
                            self.etiquetaNombreAlumno.text = "?"
                        }
                    } else {
                        log.error("Error al leer los datos")
                    }
                }

                // Actualizar el aula
                db.collection("aulas").document(self.uid).setData([
                    "cola": self.aula.cola
                    ], merge: true) { error in
                    if let error = error {
                        log.error("Error al actualizar el aula: \(error.localizedDescription)")
                    } else {
                        log.info("Cola actualizada")
                        self.conectarListener()
                    }
                }

            } else {
                log.info("El aula está vacía")
                self.etiquetaNombreAlumno.text = ""
            }

        } else {
            log.error("No hay objeto aula")
        }

    }

    @IBAction func fadeOut(_ sender: UIButton) {

        // Difuminar
        UIView.animate(withDuration: 0.1,
            delay: 0,
            options: UIView.AnimationOptions.curveLinear.intersection(UIView.AnimationOptions.allowUserInteraction).intersection(UIView.AnimationOptions.beginFromCurrentState),
            animations: {
                sender.alpha = 0.15
            }, completion: nil)
    }

    @IBAction func fadeIn(_ sender: UIButton) {

        // Restaurar
        UIView.animate(withDuration: 0.3,
            delay: 0,
            options: UIView.AnimationOptions.curveLinear.intersection(UIView.AnimationOptions.allowUserInteraction).intersection(UIView.AnimationOptions.beginFromCurrentState),
            animations: {
                sender.alpha = 1
            }, completion: nil)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
}
