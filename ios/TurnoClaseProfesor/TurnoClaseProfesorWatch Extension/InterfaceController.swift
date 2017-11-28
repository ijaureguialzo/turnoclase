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

            session = WCSession.default

            session!.sendMessage(["siguiente": "BE131"], replyHandler: { (response) -> Void in
                print("Enviando petición al iPhone")
            }, errorHandler: { (error) -> Void in
                print("Error al enviar petición al iPhone")
                print(error)
            })

        }

    }

    override func awake(withContext context: Any?) {
        super.awake(withContext: context)

        // Configure interface objects here.
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

}
