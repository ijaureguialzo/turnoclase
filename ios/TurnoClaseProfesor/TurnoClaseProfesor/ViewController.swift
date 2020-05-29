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
import FirebaseFunctions

import WatchConnectivity

import Localize_Swift

import TurnoClaseShared

import AudioToolbox

import Reachability

class ViewController: UIViewController, UITextFieldDelegate, UIPickerViewDelegate, UIPickerViewDataSource {

    // ID de usuario único generado por Firebase
    var uid: String!

    // Conectar a otro aula
    var invitado: Bool = false {
        didSet {
            pageControl.isHidden = invitado
        }
    }

    // Para visualizar el diálogo de login
    var alertController: UIAlertController!

    // Listeners para recibir las actualizaciones
    var listenerAula: ListenerRegistration!
    var listenerCola: ListenerRegistration!

    // Referencias al documento del aula y la posición en la cola
    var refAula: DocumentReference!
    var refMisAulas: CollectionReference!

    // Outlets para el interfaz de usuario
    @IBOutlet weak var etiquetaNombreAlumno: UILabel!
    @IBOutlet weak var etiquetaBotonEnCola: UIButton!
    @IBOutlet weak var etiquetaBotonCodigoAula: UIButton!
    @IBOutlet weak var pageControl: UIPageControl!
    @IBOutlet weak var indicadorActividad: UIActivityIndicatorView!

    // Soporte para varias aulas
    let MAX_AULAS = 16
    var aulaActual = 0
    var numAulas: Int = 0 {
        didSet {
            DispatchQueue.main.async {
                self.pageControl.numberOfPages = self.numAulas
                self.pageControl.isHidden = false
                self.ocultarIndicador()
            }
        }
    }

    // Para llamar a las funciones Cloud
    lazy var functions = Functions.functions(region: "europe-west1")

    // Para simular el interfaz al hacer las capturas
    var n = 2

    // Datos del aula
    var codigoAula = "..."
    var PIN = "..."
    var tiempoEspera = -1

    // Almacenar el número de alumnos anterior para detectar el paso de 0 a 1 y reproducir el sonido
    var recuentoAnterior = 0

    fileprivate func conectarAula(posicion: Int = 0) {

        // Colección que contiene las aulas del usuario
        refMisAulas = db.collection("profesores").document(self.uid).collection("aulas")

        // Recuperar las aulas del usuario
        refMisAulas.order(by: "timestamp").getDocuments() { (querySnapshot, error) in

            if let error = error {
                log.error("Error al recuperar la lista de aulas \(error.localizedDescription)")
                self.actualizarAula(codigo: "?", enCola: 0)
            } else {

                self.numAulas = querySnapshot?.documents.count ?? 0

                if posicion >= 0 && posicion < self.numAulas {
                    if let seleccionada = querySnapshot?.documents[posicion] {
                        log.info("Conectado a aula existente")
                        self.refAula = seleccionada.reference
                        self.conectarListener()
                    }
                } else {
                    log.info("Creando nueva aula...")
                    self.crearAula()
                }
            }
        }
    }

    var session: WCSession?

    fileprivate func ocultarIndicador() {
        indicadorActividad.stopAnimating()
        indicadorActividad.isHidden = true
    }

    fileprivate func mostrarIndicador() {
        indicadorActividad.isHidden = false
        indicadorActividad.startAnimating()
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // REF: Tutorial sobre el Watch: https://www.raywenderlich.com/117329/watchos-2-tutorial-part-4-watch-connectivity
        // REF: Tutorial sobre conectividad iPhone<->Watch: http://www.techotopia.com/index.php/A_watchOS_2_WatchConnectivity_Messaging_Tutorial

        if WCSession.isSupported() {
            session = WCSession.default
            session?.delegate = self
            session?.activate()
        }

        // Detectar el estado de la conexión de red
        reachability.whenReachable = { reachability in
            if reachability.connection == .wifi {
                log.info("Red Wifi")
            } else {
                log.info("Red móvil")
            }

            // Reconectar
            if self.uid != nil {
                self.conectarAula()
            }
        }

        reachability.whenUnreachable = { _ in
            self.actualizarAula(codigo: "?", enCola: 0)
            self.actualizarMensaje(texto: "No hay conexión de red".localized())
            self.pageControl.isHidden = true
            self.ocultarIndicador()
            self.desconectarListeners()
            log.emergency("Red no disponible")
        }

        do {
            try reachability.startNotifier()
        } catch {
            log.error("No se ha podido iniciar el notificador de estado de red")
        }

        // El texto encoge a medida que hay más caracteres
        etiquetaBotonCodigoAula.titleLabel?.adjustsFontSizeToFitWidth = true

        if #available(iOS 13.0, *) {
            indicadorActividad.style = .medium
        } else {
            indicadorActividad.style = .gray
        }

        log.info("Iniciando la aplicación...")

        // Ver si estamos en modo test, haciendo capturas de pantalla
        if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
            self.actualizarAula(codigo: "BE131", enCola: 0)
            self.actualizarMensaje(texto: "")
            self.PIN = "1234"
            self.numAulas = 2
        } else {

            // Limpiar el UI
            self.actualizarAula(codigo: "...", enCola: 0)
            self.actualizarMensaje(texto: "")

            ocultarIndicador()

            // Cargar el número de aulas creadas por el usuario
            pageControl.numberOfPages = numAulas

            // Iniciar sesión y conectar al aula
            Auth.auth().signInAnonymously() { (result, error) in
                if let resultado = result {

                    self.uid = resultado.user.uid
                    log.info("Registrado como usuario con UID: \(self.uid ??? "[Desconocido]")")

                    self.conectarAula()

                } else {
                    log.error("Error de inicio de sesión: \(error!.localizedDescription)")
                    self.actualizarAula(codigo: "?", enCola: 0)
                }
            }
        }
    }

    fileprivate func crearAula() {

        mostrarIndicador()

        // Generar el PIN del aula
        // REF: Números aleatorios en Swift 4.2: https://www.hackingwithswift.com/articles/102/how-to-generate-random-numbers-in-swift
        // REF: Formatear un número con 0s a la izquierda: https://stackoverflow.com/a/25566860

        // Llamar a la función Cloud que devuelve el código y crear el registro cuando lo retorne
        // REF: https://firebase.google.com/docs/functions/callable#call_the_function
        functions.httpsCallable("nuevoCodigo").call(["keepalive": false]) { (result, error) in
            if let error = error as NSError? {
                if error.domain == FunctionsErrorDomain {
                    log.error(error.localizedDescription)
                }
            }

            if let codigo = (result?.data as? [String: Any])?["codigo"] as? String {

                log.info("Nuevo código de aula: \(codigo)")

                // Guardar el documento
                self.refAula = self.refMisAulas.addDocument(data: [
                    "codigo": codigo,
                    "timestamp": FieldValue.serverTimestamp(),
                    "pin": String(format: "%04d", Int.random(in: 0...9999)),
                    "espera": 5,
                ]) { error in
                    if let error = error {
                        log.error("Error al crear el aula: \(error.localizedDescription)")
                    } else {
                        log.info("Aula creada")
                        self.numAulas += 1
                        self.conectarListener()
                    }
                }
            }
        }
    }

    fileprivate func anyadirAula() {

        mostrarIndicador()

        // Generar el PIN del aula
        // REF: Números aleatorios en Swift 4.2: https://www.hackingwithswift.com/articles/102/how-to-generate-random-numbers-in-swift
        // REF: Formatear un número con 0s a la izquierda: https://stackoverflow.com/a/25566860

        // Llamar a la función Cloud que devuelve el código y crear el registro cuando lo retorne
        // REF: https://firebase.google.com/docs/functions/callable#call_the_function
        functions.httpsCallable("nuevoCodigo").call(["keepalive": false]) { (result, error) in
            if let error = error as NSError? {
                if error.domain == FunctionsErrorDomain {
                    log.error(error.localizedDescription)
                }
            }

            if let codigo = (result?.data as? [String: Any])?["codigo"] as? String {

                log.info("Nuevo código de aula: \(codigo)")

                // Guardar el documento
                self.refMisAulas.addDocument(data: [
                    "codigo": codigo,
                    "timestamp": FieldValue.serverTimestamp(),
                    "pin": String(format: "%04d", Int.random(in: 0...9999)),
                    "espera": 5,
                ]) { error in
                    if let error = error {
                        log.error("Error al crear el aula: \(error.localizedDescription)")
                    } else {
                        log.info("Aula creada")
                        self.numAulas += 1
                    }
                }
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
                            self.tiempoEspera = aula["espera"] as? Int ?? 5

                            // Listener de la cola
                            if self.listenerCola == nil {
                                self.listenerCola = self.refAula.collection("cola").addSnapshotListener { querySnapshot, error in

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
                            self.PIN = "?"
                            self.desconectarListeners()
                            self.conectarAula()
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

            self.refAula.collection("cola").order(by: "timestamp").getDocuments() { (querySnapshot, error) in

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

                                                    // Marcar cuando hemos atendido al alumno
                                                    self.refAula.collection("espera").document(posicion["alumno"] as! String).setData([
                                                        "timestamp": FieldValue.serverTimestamp()
                                                    ]) { error in
                                                        if let error = error {
                                                            log.error("Error al añadir el documento: \(error.localizedDescription)")
                                                        }
                                                    }

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
                self.actualizarMensaje(texto: Nombres.aleatorio())
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

    fileprivate func borrarAulaReconectar(codigo: String) {

        mostrarIndicador()

        self.desconectarListeners()

        refMisAulas.whereField("codigo", isEqualTo: codigo.uppercased())
            .getDocuments() { (querySnapshot, error) in

                if let error = error {
                    log.error("Error al recuperar datos: \(error.localizedDescription)")
                } else {

                    querySnapshot!.documents.first?.reference.delete() { error in
                        if let error = error {
                            log.error("Error al borrar el aula: \(error.localizedDescription)")
                        } else {
                            log.info("Aula borrada")

                            self.numAulas -= 1
                            if self.aulaActual == self.numAulas {
                                self.aulaActual -= 1
                            }

                            self.conectarAula(posicion: self.aulaActual)
                        }
                    }
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

        if !UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") && session?.isReachable == true {
            self.session!.sendMessage([campo: dato], replyHandler: { (response) -> Void in
                log.info("Enviado al Watch")
            }, errorHandler: { (error) -> Void in
                log.error("Error al enviar datos al Watch \(error)")
            })
        }
    }

    fileprivate func actualizarAula(codigo: String) {
        self.codigoAula = codigo
        DispatchQueue.main.async {
            self.etiquetaBotonCodigoAula.setTitle(codigo, for: UIControl.State())
            self.pageControl.currentPage = self.aulaActual
        }
        log.info("Código de aula: \(codigo)")
        self.enviarWatch(campo: "codigoAula", codigo)
    }

    fileprivate func actualizarAula(enCola recuento: Int) {

        let sonidoActivado = UserDefaults.standard.bool(forKey: "QUEUE_NOT_EMPTY_SOUND")

        if sonidoActivado && self.recuentoAnterior == 0 && recuento == 1 {
            AudioServicesPlaySystemSound(SystemSoundID(1315))
        }
        self.recuentoAnterior = recuento

        DispatchQueue.main.async {
            self.etiquetaBotonEnCola.setTitle("\(recuento)", for: UIControl.State())
        }
        log.info("Alumnos en cola: \(recuento)")
        enviarWatch(campo: "enCola", String(recuento))
    }

    fileprivate func actualizarMensaje(texto: String) {
        DispatchQueue.main.async {
            self.etiquetaNombreAlumno.text = texto
        }
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
        self.desconectarListeners()
        self.conectarAula(posicion: self.aulaActual)
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

        let accionAnyadirAula = UIAlertAction(title: "Añadir aula".localized(), style: .default, handler: { (action) -> Void in
            log.info("Añadir aula")
            self.anyadirAula()
        })

        let accionBorrarAula = UIAlertAction(title: "Borrar aula".localized(), style: .destructive, handler: { (action) -> Void in
            log.info("Borrar aula")
            self.confirmarBorrado()
        })

        let accionConectarOtraAula = UIAlertAction(title: "Conectar a otra aula".localized(), style: .default, handler: { (action) -> Void in
            log.info("Conectar a otra aula")
            self.dialogoConexion()
        })

        let accionDesconectarAula = UIAlertAction(title: "Desconectar del aula".localized(), style: .destructive, handler: { (action) -> Void in
            log.info("Desconectar del aula")
            self.desconectarAula()
        })

        let accionRecuperarAula = UIAlertAction(title: "Recuperar aula".localized(), style: .destructive, handler: { (action) -> Void in
            log.info("Recuperar aula")
            self.desconectarListeners()
            self.conectarAula()
        })

        let accionCancelar = UIAlertAction(title: "Cancelar".localized(), style: .cancel, handler: { (action) -> Void in
            log.info("Cancelar")
        })

        let accionEstablecerTiempoEspera = UIAlertAction(title: "Establecer tiempo de espera".localized(), style: .default, handler: { (action) -> Void in
            log.info("Establecer tiempo de espera")
            self.dialogoTiempoEspera()
        })

        if !invitado {
            if codigoAula != "?" {
                alertController.addAction(accionEstablecerTiempoEspera)
                if numAulas < MAX_AULAS {
                    alertController.addAction(accionAnyadirAula)
                }
                if numAulas > 1 {
                    alertController.addAction(accionBorrarAula)
                }
                alertController.addAction(accionConectarOtraAula)
            } else {
                alertController.addAction(accionRecuperarAula)
            }
        } else {
            alertController.addAction(accionDesconectarAula)
        }

        alertController.addAction(accionCancelar)

        // En iPad, el alert se tiene que mostrar como popup: https://medium.com/@nickmeehan/actionsheet-popover-on-ipad-in-swift-5768dfa82094
        if let popoverController = alertController.popoverPresentationController {
            popoverController.sourceView = self.view
            popoverController.sourceRect = CGRect(x: self.view.bounds.midX, y: self.view.bounds.midY, width: 0, height: 0)
        }

        self.present(alertController, animated: true, completion: nil)
    }

    fileprivate func confirmarBorrado() {

        let dialogo = UIAlertController(title: "Borrar aula".localized(),
                                        message: "Esta acción vaciará la cola de espera.".localized(),
                                        preferredStyle: .alert)

        let ok = UIAlertAction(title: "Ok".localized(), style: .destructive, handler: { (action) -> Void in
            log.info("Ok")
            self.borrarAulaReconectar(codigo: self.codigoAula)
        })

        let cancelar = UIAlertAction(title: "Cancelar".localized(), style: .cancel) { (action) -> Void in
            log.info("Cancelar")
        }

        dialogo.addAction(ok)
        dialogo.addAction(cancelar)

        self.present(dialogo, animated: true, completion: nil)
    }

    fileprivate func buscarAula(codigo: String?, pin: String?) {

        if let codigo = codigo, let pin = pin {
            log.debug("Buscando UID del aula: \(codigo):\(pin)")

            // Buscar el aula
            db.collectionGroup("aulas")
                .whereField("codigo", isEqualTo: codigo.uppercased())
                .whereField("pin", isEqualTo: pin)
                .getDocuments() { (querySnapshot, error) in

                    if let error = error {
                        log.error("Error al recuperar datos: \(error.localizedDescription)")
                    } else {

                        // Comprobar que se han recuperado registros
                        if let documents = querySnapshot?.documents {

                            if documents.count > 0 {
                                // Accedemos al primer documento
                                let document = documents.first

                                log.info("Aula encontrada: \(codigo)")

                                self.desconectarListeners()
                                self.invitado = true
                                self.refAula = document?.reference
                                self.conectarListener()
                            } else {
                                log.error("Aula no encontrada")
                                self.dialogoError()
                            }
                        }
                    }
            }
        }
    }

    func dialogoTiempoEspera() {

        let vc = UIViewController()
        vc.preferredContentSize = CGSize(width: 320, height: 216)

        let pickerView = UIPickerView(frame: CGRect(x: 0, y: 0, width: 320, height: 216))
        pickerView.delegate = self
        pickerView.dataSource = self
        vc.view.addSubview(pickerView)

        pickerView.selectRow(tiempos.firstIndex(of: tiempoEspera) ?? 0, inComponent: 0, animated: true)

        let minutosAlert = UIAlertController(title: "Tiempo de espera (minutos)".localized(), message: "", preferredStyle: UIAlertController.Style.alert)
        minutosAlert.setValue(vc, forKey: "contentViewController")

        // Guardar el nuevo tiempo para el aula
        let confirmAction = UIAlertAction(title: "Guardar".localized(),
                                          style: .default) { _ in

            self.tiempoEspera = self.tiempos[pickerView.selectedRow(inComponent: 0)]
            log.info("Establecer tiempo de espera en \(self.tiempoEspera) minutos...")

            self.refAula.updateData([
                "espera": self.tiempoEspera
            ]) { error in
                if let error = error {
                    log.error("Error al actualizar el aula: \(error.localizedDescription)")
                } else {
                    log.info("Aula actualizada")
                }
            }
        }

        // Cancelar
        let cancelAction = UIAlertAction(title: "Cancelar".localized(),
                                         style: .cancel) { (_) in }

        minutosAlert.addAction(confirmAction)
        minutosAlert.addAction(cancelAction)

        self.present(minutosAlert, animated: true)
    }

    let tiempos = [0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60]

    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }

    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return tiempos.count
    }

    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        return String(tiempos[row])
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

            if self.codigoAula != codigoAula {
                self.buscarAula(codigo: codigoAula, pin: PIN)
            } else {
                log.error("No se puede conectar a la propia aula en que estamos")
                self.dialogoError()
            }

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

    // Moverse entre aulas
    fileprivate func aulaAnterior() {
        if !invitado && numAulas > 1 {
            if aulaActual > 0 {
                aulaActual -= 1
            }

            log.debug("Aula anterior")

            self.desconectarListeners()
            self.conectarAula(posicion: self.aulaActual)
        }
    }

    @IBAction func swipeDerecha(_ sender: Any) {
        aulaAnterior()
    }

    fileprivate func aulaSiguiente() {
        if !invitado && numAulas > 1 {
            if aulaActual < numAulas - 1 {
                aulaActual += 1
            }
            log.debug("Aula siguiente")

            self.desconectarListeners()
            self.conectarAula(posicion: self.aulaActual)
        }
    }

    @IBAction func swipeIzquierda(_ sender: Any) {
        aulaSiguiente()
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
        case "aulaSiguiente":
            aulaSiguiente()
        case "aulaAnterior":
            aulaAnterior()
        default:
            break
        }

        // No se si es necesario enviar una respuesta vacía
        replyHandler([String: String]())
    }

}
