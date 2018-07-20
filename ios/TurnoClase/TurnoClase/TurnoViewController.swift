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

    var codigoAula: String!
    var nombreUsuario: String!

    // Objeto aula que contiene la cola de alumnos
    var aula: Aula!
    var uid: String!
    var listener: ListenerRegistration!

    var pedirTurno = true
    var refAula: DocumentReference!

    // Para simular el interfaz al hacer las capturas
    var n = 2

    @IBOutlet weak var etiquetaAula: UILabel!
    @IBOutlet weak var etiquetaNumero: UILabel!

    override func viewDidLoad() {
        super.viewDidLoad()

        log.debug("Valores recibidos: \(codigoAula ??? "[Aula desconocida]") - \(nombreUsuario ??? "[Usuario desconocido]")")

        log.info("Iniciando la aplicación...")

        //try? Auth.auth().signOut()

        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            etiquetaAula.text = "BE131"
            etiquetaNumero.text = "2"
        } else {
            // Registrarse como usuario anónimo
            Auth.auth().signInAnonymously() { (result, error) in

                if let resultado = result {

                    self.uid = resultado.user.uid
                    log.info("Registrado como usuario con UID: \(self.uid ??? "[Desconocido]")")

                    db.collection("alumnos").document(self.uid).setData([
                        "nombre": self.nombreUsuario
                        ], merge: true) { error in
                        if let error = error {
                            log.error("Error al actualizar el alumno: \(error.localizedDescription)")
                        } else {
                            log.info("Alumno actualizado")
                        }
                    }

                    db.collection("aulas").whereField("codigo", isEqualTo: self.codigoAula).limit(to: 1).getDocuments() { (querySnapshot, err) in
                        if let err = err {
                            print("Error getting documents: \(err)")
                        } else {
                            if querySnapshot!.documents.count > 0 {
                                for document in querySnapshot!.documents {

                                    log.info("Conectado a aula existente")

                                    if self.listener == nil {
                                        self.listener = document.reference
                                            .addSnapshotListener { documentSnapshot, error in

                                                if (documentSnapshot?.exists)! {

                                                    self.refAula = documentSnapshot?.reference

                                                    if let aula = documentSnapshot?.data() {

                                                        log.info("Actualizando datos del aula...")
                                                        let cola = aula["cola"] as? [String] ?? []
                                                        let codigo = aula["codigo"] as? String ?? "?"

                                                        self.aula = Aula(codigo: codigo, cola: cola)

                                                        // Si el usuario no está en la cola, lo añadimos
                                                        if self.pedirTurno && !cola.contains(self.uid) {

                                                            self.pedirTurno = false
                                                            self.aula.cola.append(self.uid)

                                                            documentSnapshot?.reference.setData([
                                                                "cola": self.aula.cola
                                                                ], merge: true) { error in
                                                                if let error = error {
                                                                    log.error("Error al actualizar el aula: \(error.localizedDescription)")
                                                                } else {
                                                                    log.info("Cola actualizada")
                                                                }
                                                            }
                                                        }

                                                        log.debug("Aula: \(self.aula ??? "[Desconocida]")")

                                                        self.actualizarPantalla()

                                                    }
                                                } else {
                                                    log.info("El aula ha desaparecido")

                                                    if self.listener != nil {
                                                        self.listener.remove()
                                                        self.listener = nil
                                                        self.aula = nil
                                                    }

                                                    // Volver a la pantalla inicial
                                                    self.dismiss(animated: true, completion: { })

                                                }
                                        }
                                    }
                                }
                            } else {
                                log.info("Aula no encontrada")
                                self.etiquetaAula.text = "?"
                            }
                        }
                    }

                } else {
                    log.error("Error de inicio de sesión: \(error!.localizedDescription)")
                }
            }
        }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    @IBAction func botonCancelar(_ sender: UIButton) {

        fadeIn(sender)
        log.info("Cancelando...")

        // Nos borramos de la cola
        if self.aula != nil && self.aula.cola.contains(self.uid) {

            if let index = self.aula.cola.index(of: self.uid) {
                self.aula.cola.remove(at: index)
            }

            refAula.setData([
                "cola": self.aula.cola
                ], merge: true) { error in
                if let error = error {
                    log.error("Error al actualizar el aula: \(error.localizedDescription)")
                } else {
                    log.info("Cola actualizada")
                }
            }
        }

        if listener != nil {
            self.listener.remove()
            self.listener = nil
            self.aula = nil
        }

        // Volver a la pantalla inicial
        self.dismiss(animated: true, completion: { })
    }


    func actualizarPantalla() {

        if let aula = self.aula {

            // Mostramos el código en la pantalla
            self.etiquetaAula.text = aula.codigo

            if let posicion = self.aula.cola.index(of: self.uid) {
                if posicion > 0 {
                    self.etiquetaNumero.text = String(posicion)
                } else {
                    self.etiquetaNumero.text = NSLocalizedString("ES_TU_TURNO", comment: "Mensaje de que ha llegado el turno")
                }
            } else {
                self.etiquetaNumero.text = ""
            }

            log.info("Alumnos en cola: \(aula.cola.count)")

        } else {
            log.error("No hay objeto aula")
        }

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

        // Difuminar
        UIView.animate(withDuration: 0.1,
            delay: 0,
            options: UIView.AnimationOptions.curveLinear.intersection(.allowUserInteraction).intersection(.beginFromCurrentState),
            animations: {
                sender.alpha = 0.15
            }, completion: nil)
    }

    @IBAction func fadeIn(_ sender: UIButton) {

        // Restaurar
        UIView.animate(withDuration: 0.3,
            delay: 0,
            options: UIView.AnimationOptions.curveLinear.intersection(.allowUserInteraction).intersection(.beginFromCurrentState),
            animations: {
                sender.alpha = 1
            }, completion: nil)
    }
}
