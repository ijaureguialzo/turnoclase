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

import WatchConnectivity

import Localize_Swift

class ViewController: UIViewController, UITextFieldDelegate {

    // ID de usuario único generado por Firebase
    var uid: String!

    // Conectar a otro aula
    var invitado = false
    var uidPropio: String!

    // Para visualizar el diálogo de login
    var alertController: UIAlertController!

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

    // Datos del aula
    var codigoAula = "..."
    var PIN = "..."

    fileprivate func conectarAula() {
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
    }

    var session: WCSession?

    override func viewDidLoad() {
        super.viewDidLoad()

        // REF: Tutorial sobre el Watch: https://www.raywenderlich.com/117329/watchos-2-tutorial-part-4-watch-connectivity
        // REF: Tutorial sobre conectividad iPhone<->Watch: http://www.techotopia.com/index.php/A_watchOS_2_WatchConnectivity_Messaging_Tutorial

        if WCSession.isSupported() {
            session = WCSession.default
            session?.delegate = self
            session?.activate()
        }

        // El texto encoge a medida que hay más caracteres
        etiquetaBotonCodigoAula.titleLabel?.adjustsFontSizeToFitWidth = true

        log.info("Iniciando la aplicación...")

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            self.actualizarAula(codigo: "BE131", enCola: 0)
            self.actualizarMensaje(texto: "")
        } else {

            // Limpiar el UI
            self.actualizarAula(codigo: "...", enCola: 0)
            self.actualizarMensaje(texto: "")

            // Iniciar sesión y conectar al aula
            Auth.auth().signInAnonymously() { (result, error) in
                if let resultado = result {

                    self.uid = resultado.user.uid
                    self.uidPropio = self.uid
                    log.info("Registrado como usuario con UID: \(self.uid ??? "[Desconocido]")")

                    self.conectarAula()

                } else {
                    log.error("Error de inicio de sesión: \(error!.localizedDescription)")
                }
            }
        }
    }

    fileprivate func crearAula() {

        // Almacenar la referencia a la nueva aula
        self.refAula = db.collection("aulas").document(self.uid)

        // Generar el PIN del aula
        // REF: Números aleatorios en Swift 4.2: https://www.hackingwithswift.com/articles/102/how-to-generate-random-numbers-in-swift
        // REF: Formatear un número con 0s a la izquierda: https://stackoverflow.com/a/25566860

        // Guardar el documento con un Timestamp, para que se genere el código
        self.refAula.setData([
            "timestamp": FieldValue.serverTimestamp(),
            "pin": String(format: "%04d", Int.random(in: 0...9999))
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

                            self.actualizarAula(codigo: aula["codigo"] ??? "?")
                            self.actualizarPIN(aula["pin"] ??? "?")

                            // Listener de la cola
                            if self.listenerCola == nil {
                                self.listenerCola = db.collection("aulas").document(self.uid)
                                    .collection("cola").addSnapshotListener { querySnapshot, error in

                                        if let error = error {
                                            log.error("Error al recuperar datos: \(error.localizedDescription)")
                                        } else {
                                            self.actualizarAula(codigo: self.codigoAula, enCola: querySnapshot!.documents.count)
                                            self.mostrarSiguiente()
                                            self.feedbackTactil(alerta: true)
                                        }
                                }
                            }
                        }
                    } else {
                        log.info("El aula ha desaparecido")

                        // Detectar si el aula desaparece y si somos invitados, desconectar
                        if !self.invitado {
                            self.actualizarAula(codigo: "?", enCola: 0)
                        } else {
                            self.desconectarAula()
                        }
                    }
            }
        }
    }

    fileprivate func mostrarSiguiente(avanzarCola: Bool = false) {

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
                                                if avanzarCola {
                                                    refPosicion.delete()
                                                }
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

    @IBAction func botonSiguiente(_ sender: UIButton) {
        efectoBoton(sender)
        mostrarSiguiente(avanzarCola: true)
    }

    @IBAction func botonEnCola(_ sender: UIButton) {

        log.info("Este botón sólo se usa para los test de UI")

        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            if n >= 0 {
                self.actualizarAula(enCola: n)
                self.actualizarMensaje(texto: nombreAleatorio())
            } else {
                self.actualizarAula(enCola: 0)
                self.actualizarMensaje(texto: "")
            }

            n -= 1
        }
    }

    @IBAction func botonCodigoAulaCorto(_ sender: UIButton) {

        // Menú de acciones para gestionar múltiples profesores
        mostrarAcciones()
        feedbackTactil()
    }

    fileprivate func borrarAula() {

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
    }

    @IBAction func botonCodigoAulaLargo(_ sender: UILongPressGestureRecognizer) {
        // Anulado
    }

    fileprivate func actualizarAula(codigo codigoAula: String, enCola recuento: Int) {
        actualizarAula(codigo: codigoAula)
        actualizarAula(enCola: recuento)
    }

    fileprivate func enviarWatch(campo: String, _ dato: String) {

        if !UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") && session != nil {
            self.session!.sendMessage([campo: dato], replyHandler: { (response) -> Void in
                    log.info("Enviado al Watch")
                }, errorHandler: { (error) -> Void in
                    log.error("Error al enviar datos al Watch \(error)")
                })
        }
    }

    fileprivate func actualizarAula(codigo: String) {
        self.etiquetaBotonCodigoAula.setTitle(codigo, for: UIControl.State())
        self.codigoAula = codigo
        log.info("Código de aula: \(codigo)")
        enviarWatch(campo: "codigoAula", codigo)
    }

    fileprivate func actualizarAula(enCola recuento: Int) {
        self.etiquetaBotonEnCola.setTitle("\(recuento)", for: UIControl.State())
        log.info("Alumnos en cola: \(recuento)")
        enviarWatch(campo: "enCola", String(recuento))
    }

    fileprivate func actualizarMensaje(texto: String) {
        self.etiquetaNombreAlumno.text = texto
        enviarWatch(campo: "mensaje", texto)
    }

    fileprivate func actualizarPIN(_ pin: String) {
        self.PIN = pin
    }

    fileprivate func desconectarListeners() {

        self.listenerAula?.remove()
        self.listenerAula = nil

        self.listenerCola?.remove()
        self.listenerCola = nil

    }

    fileprivate func desconectarAula() {
        self.invitado = false
        self.uid = self.uidPropio
        self.desconectarListeners()
        self.conectarAula()
    }

    fileprivate func mostrarAcciones() {

        // REF: iOS Action Sheet: http://swiftdeveloperblog.com/actionsheet-example-in-swift/

        let alertController: UIAlertController = {
            // REF: Localizar una cadena con interpolación: https://github.com/marmelroy/Localize-Swift/issues/89#issuecomment-331673546
            if !invitado {
                return UIAlertController(title: String(format: "Aula %@".localized(), codigoAula),
                    message: String(format: "PIN para compartir este aula: %@".localized(), PIN),
                    preferredStyle: .actionSheet)
            } else {
                return UIAlertController(title: String(format: "Aula %@".localized(), codigoAula),
                    message: "Conectado como invitado".localized(),
                    preferredStyle: .actionSheet)
            }
        }()

        let accionGenerarNuevoCodigo = UIAlertAction(title: "Generar nueva aula".localized(), style: .destructive, handler: { (action) -> Void in
                log.info("Generar nueva aula")
                self.desconectarListeners()
                self.borrarAula()
            })

        let accionConectarOtraAula = UIAlertAction(title: "Conectar a otra aula".localized(), style: .default, handler: { (action) -> Void in
                log.info("Conectar a otra aula")
                self.dialogoConexion()
            })

        let accionDesconectarAula = UIAlertAction(title: "Desconectar del aula".localized(), style: .destructive, handler: { (action) -> Void in
                log.info("Desconectar del aula")
                self.desconectarAula()
            })

        let accionCancelar = UIAlertAction(title: "Cancelar".localized(), style: .cancel, handler: { (action) -> Void in
                log.info("Cancelar")
            })

        if(!invitado) {
            alertController.addAction(accionGenerarNuevoCodigo)
            alertController.addAction(accionConectarOtraAula)
        } else {
            alertController.addAction(accionDesconectarAula)
        }

        alertController.addAction(accionCancelar)

        self.present(alertController, animated: true, completion: nil)
    }

    fileprivate func buscarAula(codigo: String?, pin: String?) {

        if let codigo = codigo, let pin = pin {
            log.debug("Buscando UID del aula: \(codigo):\(pin)")

            // Buscar el aula
            db.collection("aulas")
                .whereField("codigo", isEqualTo: codigo.uppercased())
                .whereField("pin", isEqualTo: pin)
                .limit(to: 1)
                .getDocuments() { (querySnapshot, error) in

                    if let error = error {
                        log.error("Error al recuperar datos: \(error.localizedDescription)")
                    } else {

                        // Comprobar que se han recuperado registros
                        if querySnapshot!.documents.count > 0 {

                            // Accedemos al primer documento
                            let document = querySnapshot!.documents[0]

                            let uid = document.reference.documentID
                            log.info("Aula encontrada: \(uid)")

                            self.invitado = true
                            self.desconectarListeners()
                            self.uid = uid
                            self.conectarAula()

                        } else {
                            log.error("Aula no encontrada")
                            self.dialogoError()
                        }
                    }
            }
        }
    }

    fileprivate func dialogoError() {
        self.alertController = UIAlertController(title: "Error de conexión".localized(),
            message: "No se ha podido acceder al aula con los datos proporcionados.".localized(),
            preferredStyle: .alert)
        let cancelAction = UIAlertAction(title: "Ok".localized(),
            style: .default) { (_) in }
        alertController.addAction(cancelAction)
        self.present(self.alertController, animated: true, completion: nil)
    }

    // Para gestionar si se activa el botón de conectar
    fileprivate var loginAulaOk = false
    fileprivate var loginPinOk = false

    fileprivate func dialogoConexion() {

        // REF: Crear un cuadro de diálogo modal: https://www.simplifiedios.net/ios-dialog-box-with-input/

        // Cada vez que se vuelva a mostrar el cuadro hay que reiniciarlos
        loginAulaOk = false
        loginPinOk = false

        alertController = UIAlertController(title: "Conectar a otra aula".localized(),
            message: "Introduce los datos del aula a la que quieres conectar.".localized(),
            preferredStyle: .alert)

        // Conectar
        let confirmAction = UIAlertAction(title: "Conectar".localized(),
            style: .default) { (_) in

            log.info("Conectando a otra aula")

            let codigoAula = self.alertController.textFields?[0].text
            let PIN = self.alertController.textFields?[1].text

            self.buscarAula(codigo: codigoAula, pin: PIN)

        }

        // Desactivar el botón de confirmar por defecto
        confirmAction.isEnabled = false

        // Cancelar
        let cancelAction = UIAlertAction(title: "Cancelar".localized(),
            style: .cancel) { (_) in }

        // Cuadros de texto
        alertController.addTextField { (textField) in
            textField.placeholder = "Código de aula".localized()
            textField.tag = 10
            textField.autocapitalizationType = .allCharacters
            textField.keyboardType = .asciiCapable
            textField.autocorrectionType = .no
            textField.enablesReturnKeyAutomatically = true
            textField.delegate = self
        }
        alertController.addTextField { (textField) in
            textField.placeholder = "PIN".localized()
            textField.tag = 20
            textField.keyboardType = .numberPad
            textField.autocorrectionType = .no
            textField.delegate = self
        }

        // Añadir los controles al cuadro de diálogo
        alertController.addAction(confirmAction)
        alertController.addAction(cancelAction)

        // Presentarlo asociado a este ViewController
        self.present(alertController, animated: true, completion: nil)

    }

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {

        // REF: Gestionar la longitud máxima: https://stackoverflow.com/a/31363255/5136913
        if (range.length + range.location > textField.text!.count) {
            return false
        }

        let newLength = textField.text!.count + string.count - range.length

        // REF: Habilitar el control: https://stackoverflow.com/a/39542428/5136913
        switch textField.tag {
        case 10:
            loginAulaOk = newLength >= 5
            self.alertController.actions[0].isEnabled = loginAulaOk && loginPinOk
            return newLength <= 5
        case 20:
            loginPinOk = newLength >= 4
            self.alertController.actions[0].isEnabled = loginAulaOk && loginPinOk
            return newLength <= 4
        default:
            return newLength <= 0
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

}

extension ViewController: WCSessionDelegate {

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        log.debug("iPhone: sesión activa")
    }

    func sessionDidBecomeInactive(_ session: WCSession) {
        log.debug("iPhone: sesión inactiva")
    }

    func sessionDidDeactivate(_ session: WCSession) {
        log.debug("iPhone: sesión desactivada")
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {

        switch message["comando"] as! String {
        case "siguiente":
            mostrarSiguiente(avanzarCola: true)
        case "actualizar":
            self.actualizarAula(codigo: codigoAula)
            mostrarSiguiente(avanzarCola: false)
        default:
            break
        }

        // No se si es necesario enviar una respuesta vacía
        replyHandler([String: String]())
    }

}
