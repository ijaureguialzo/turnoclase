//
//  InterfaceController.swift
//  TurnoClaseProfesorWatch Extension
//
//  Created by Ion Jaureguialzo Sarasola on 21/11/17.
//  Copyright © 2017 Ion Jaureguialzo Sarasola. All rights reserved.
//

import WatchKit
import Foundation

import WatchConnectivity

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

    @IBAction func botonSiguiente() {

        if WCSession.isSupported() {

            session!.sendMessage(["comando": "siguiente"], replyHandler: { (response) -> Void in
                    print("Comando: Siguiente")
                }, errorHandler: { (error) -> Void in
                    print("Error al enviar petición al iPhone \(error)")
                })
        }

    }

    override func awake(withContext context: Any?) {
        super.awake(withContext: context)

        // Configure interface objects here.
        session = WCSession.default

        self.etiquetaAula.setText("...")
        self.etiquetaNumero.setText("...")
        self.etiquetaNombre.setText("")

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
        print("Watch: sesión activa")
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

    }
}
