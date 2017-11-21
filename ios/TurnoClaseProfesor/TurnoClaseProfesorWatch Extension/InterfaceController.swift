//
//  InterfaceController.swift
//  TurnoClaseProfesorWatch Extension
//
//  Created by Ion Jaureguialzo Sarasola on 21/11/17.
//  Copyright © 2017 Ion Jaureguialzo Sarasola. All rights reserved.
//

import WatchKit
import Foundation


class InterfaceController: WKInterfaceController {

    @IBOutlet var etiquetaAula: WKInterfaceLabel!
    @IBOutlet var etiquetaNumero: WKInterfaceLabel!
    @IBOutlet var etiquetaNombre: WKInterfaceLabel!

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
