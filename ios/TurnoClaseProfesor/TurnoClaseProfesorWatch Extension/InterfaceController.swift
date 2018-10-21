//
//  InterfaceController.swift
//  TurnoClaseProfesorWatch Extension
//
//  Created by Ion Jaureguialzo Sarasola on 21/11/17.
//  Copyright © 2017 Ion Jaureguialzo Sarasola. All rights reserved.
//

import WatchKit
import WatchConnectivity

import XCGLogger

// Servicio de logs XCGLogger
let log = XCGLogger.default

import Localize_Swift

class InterfaceController: WKInterfaceController {

    var session: WCSession? {
        didSet {
            if let session = session {
                session.delegate = self
                session.activate()
            }
        }
    }

    @IBOutlet var etiquetaAula: WKInterfaceLabel!
    @IBOutlet var etiquetaNumero: WKInterfaceLabel!
    @IBOutlet var etiquetaNombre: WKInterfaceLabel!
    @IBOutlet var etiquetaBotonSiguiente: WKInterfaceButton!

    @IBAction func botonSiguiente() {

        if(!demo) {
            if WCSession.isSupported() {
                session!.sendMessage(["comando": "siguiente"], replyHandler: { (response) -> Void in
                        log.debug("Comando: Siguiente")
                    }, errorHandler: { (error) -> Void in
                        log.error("Error al enviar petición al iPhone \(error)")
                    })
            }
        } else {
            if n >= 0 {
                actualizarPantalla(numero: n, nombre: nombreAleatorio())
            } else {
                actualizarPantalla(numero: 0, nombre: "")
            }

            n -= 1
        }
    }

    var watch: WKInterfaceDevice!

    var demo = false
    var n = 2

    var localizado = true // Por defecto a true; parche para corregir el problema de la localización del watch al hacer capturas

    fileprivate func actualizarPantalla(numero: Int, nombre: String) {
        self.etiquetaNumero.setText(String(numero))
        self.etiquetaNombre.setText(nombre)
    }

    override func awake(withContext context: Any?) {
        super.awake(withContext: context)

        // Configurar XCGLogger
        log.setup(level: .debug, showThreadName: true, showLevel: true, showFileNames: true, showLineNumbers: true, writeToFile: nil, fileLevel: .debug)

        if localizado {
            self.etiquetaBotonSiguiente.setTitle("Siguiente".localized())
        } else {
            self.etiquetaBotonSiguiente.setTitle("Boton_Siguiente")
        }
        actualizarPantalla(numero: 0, nombre: "")

        if !demo {
            self.etiquetaAula.setText("...")
        } else {
            self.etiquetaAula.setText("BE131")
        }

        // Configure interface objects here.
        session = WCSession.default

        // Acceso al dispositivo
        watch = WKInterfaceDevice.current()

    }

    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }

    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }

}

extension InterfaceController: WCSessionDelegate {

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        log.debug("Watch: sesión activa")

        // Actualizar la pantalla al conectar por primera vez
        if(!demo) {
            session.sendMessage(["comando": "actualizar"], replyHandler: { (response) -> Void in
                    log.debug("Comando: Actualizar")
                }, errorHandler: { (error) -> Void in
                    log.error("Error al enviar petición al iPhone \(error)")
                })
        }
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {

        if let codigoAula = message["codigoAula"] as? String {
            self.etiquetaAula.setText(codigoAula)
        }
        if let enCola = message["enCola"] as? String {
            self.etiquetaNumero.setText(enCola)
        }
        if let mensaje = message["mensaje"] as? String {
            self.etiquetaNombre.setText(mensaje)
        }

        // Feedback tactil
        watch.play(.click)
    }

}
