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

    // ID de usuario único generado por Firebase
    var uid: String!

    // Listeners para recibir las actualizaciones
    var listenerAula: ListenerRegistration!
    var listenerCola: ListenerRegistration!

    // Referencias al documento del aula y la posición en la cola
    var refAula: DocumentReference!

    // Outlets para el interfaz de usuario
    @IBOutlet weak var etiquetaNombreAlumno: UILabel!
    @IBOutlet weak var etiquetaBotonEnCola: UIButton!
    @IBOutlet weak var etiquetaBotonCodigoAula: UIButton!

    // Para simular el interfaz al hacer las capturas
    var n = 2

    override func viewDidLoad() {
        super.viewDidLoad()

        // El texto encoge a medida que hay más caracteres
        etiquetaBotonCodigoAula.titleLabel?.adjustsFontSizeToFitWidth = true

        log.info("Iniciando la aplicación...")

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            self.actualizarAula(codigo: "BE131", enCola: 2)
            self.actualizarMensaje(texto: "")
        } else {

            // Iniciar sesión y conectar al aula
            Auth.auth().signInAnonymously() { (result, error) in
                if let resultado = result {

                    self.uid = resultado.user.uid
                    log.info("Registrado como usuario con UID: \(self.uid ??? "[Desconocido]")")

                    // Cargar el aula y si no, crearla
                    db.collection("aulas").document(self.uid).getDocument() { (document, error) in

                        if !(document?.exists)! {
                            log.info("Creando nueva aula...")
                            self.crearAula()
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

    fileprivate func crearAula() {

        // Almacenar la referencia a la nueva aula
        self.refAula = db.collection("aulas").document(self.uid)

        // Guardar el documento con un Timestamp, para que se genere el código
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
    }

    fileprivate func conectarListener() {

        // Conectar el listener del aula para detectar cambios (por ejemplo, que se borra)
        if self.listenerAula == nil && self.refAula != nil {
            self.listenerAula = self.refAula
                .addSnapshotListener { documentSnapshot, error in

                    if (documentSnapshot?.exists)! {

                        if let aula = documentSnapshot?.data() {

                            log.info("Actualizando datos del aula...")

                            let codigoAula = aula["codigo"] as? String ?? "?"
                            log.debug("Aula: \(codigoAula ??? "[Desconocida]")")

                            self.actualizarAula(codigo: codigoAula)

                            // Listener de la cola
                            if self.listenerCola == nil {
                                self.listenerCola = db.collection("aulas").document(self.uid)
                                    .collection("cola").addSnapshotListener { querySnapshot, error in

                                        if let error = error {
                                            log.error("Error al recuperar datos: \(error.localizedDescription)")
                                        } else {
                                            self.actualizarAula(enCola: querySnapshot!.documents.count)
                                        }
                                }
                            }
                        }
                    } else {
                        log.info("El aula ha desaparecido")
                        self.actualizarAula(codigo: "?", enCola: 0)
                    }
            }
        }
    }

    @IBAction func botonSiguiente(_ sender: UIButton) {

        fadeIn(sender)

        log.info("Mostrando el siguiente alumno...")

        if self.refAula != nil {

            self.refAula.collection("cola").order(by: "timestamp").limit(to: 1).getDocuments() { (querySnapshot, error) in

                if let error = error {
                    log.error("Error al recuperar datos: \(error.localizedDescription)")
                } else {

                    if querySnapshot!.documents.count > 0 {
                        let refPosicion = querySnapshot!.documents[0].reference

                        refPosicion.getDocument { (document, error) in

                            if let posicion = document?.data() {

                                // Cargar el alumno
                                db.collection("alumnos").document(posicion["alumno"] as! String).getDocument { (document, error) in
                                    if let document = document {
                                        if document.exists {
                                            if let alumno = document.data() {

                                                // Mostrar el nombre
                                                self.actualizarMensaje(texto: alumno["nombre"] as! String)

                                                // Borrar la entrada de la cola
                                                refPosicion.delete()
                                            }
                                        } else {
                                            log.error("El alumno no existe")
                                            self.actualizarMensaje(texto: "?")
                                        }
                                    } else {
                                        log.error("Error al leer los datos")
                                    }
                                }
                            }
                        }
                    } else {
                        log.info("Cola vacía")
                        self.actualizarMensaje(texto: "")
                    }
                }
            }
        }
    }

    @IBAction func botonEnCola(_ sender: UIButton) {
        log.info("Este botón ya no hace nada :)")

        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            n -= 1
            if(n >= 0) {
                self.actualizarAula(enCola: n)
                self.actualizarMensaje(texto: nombreAleatorio())
            } else {
                self.actualizarAula(enCola: 0)
                self.actualizarMensaje(texto: "")
            }
        }

    }

    @IBAction func botonCodigoAulaCorto(_ sender: UIButton) {

        //log.info("Vaciando el aula...")
        // Pendiente de implementar en el servidor, no se puede borrar una colección desde el cliente
        // REF: https://firebase.google.com/docs/firestore/manage-data/delete-data?hl=es-419

        mostrarAcciones()

    }

    @IBAction func botonCodigoAulaLargo(_ sender: UILongPressGestureRecognizer) {

        if (sender.state == UIGestureRecognizer.State.ended) {

            log.info("Generando nueva aula...")

            if self.listenerAula != nil {

                listenerAula.remove()
                listenerAula = nil

                // Pendiente: Llamar a la función de vaciar la cola porque no se borra la subcolección

                db.collection("aulas").document(self.uid).delete() { error in
                    if let error = error {
                        log.error("Error al borrar el aula: \(error.localizedDescription)")
                    } else {
                        log.info("Aula borrada")
                        log.info("Creando nueva aula...")
                        self.crearAula()
                    }
                }

            } else {
                log.error("El listener no está conectado")
            }

        }
    }

    fileprivate func actualizarAula(codigo codigoAula: String, enCola recuento: Int) {
        actualizarAula(codigo: codigoAula)
        actualizarAula(enCola: recuento)
    }

    fileprivate func actualizarAula(codigo: String) {
        // Mostramos el código en la pantalla
        self.etiquetaBotonCodigoAula.setTitle(codigo, for: UIControl.State())
    }

    fileprivate func actualizarAula(enCola recuento: Int) {
        // Mostrar el recuento
        self.etiquetaBotonEnCola.setTitle("\(recuento)", for: UIControl.State())
        log.info("Alumnos en cola: \(recuento)")
    }

    fileprivate func actualizarMensaje(texto: String) {
        self.etiquetaNombreAlumno.text = texto
    }

    fileprivate func mostrarAcciones() {

        // REF: iOS Action Sheet: http://swiftdeveloperblog.com/actionsheet-example-in-swift/

        let alertController = UIAlertController(title: "Aula \(etiquetaBotonCodigoAula.titleLabel?.text ?? "?")", message: "PIN del aula: 1234", preferredStyle: .actionSheet)

        let accionConectarOtraAula = UIAlertAction(title: "Conectar a otra aula", style: .default, handler: { (action) -> Void in
            log.info("Conectar a otra aula")
        })

        let accionDesconectarAula = UIAlertAction(title: "Desconectar del aula", style: .destructive, handler: { (action) -> Void in
            log.info("Desconectar del aula")
        })

        let accionCancelar = UIAlertAction(title: "Cancelar", style: .cancel, handler: { (action) -> Void in
            log.info("Cancelar")
        })

        alertController.addAction(accionConectarOtraAula)
        alertController.addAction(accionDesconectarAula)
        alertController.addAction(accionCancelar)

        self.present(alertController, animated: true, completion: nil)
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
