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
//  AppDelegate.swift
//  TurnoClaseProfesor
//
//  Created by widemos on 19/6/15.
//

import UIKit

import XCGLogger

import Firebase
import FirebaseFirestore

// Servicio de logs XCGLogger
let log = XCGLogger.default

// Conexión a Firestore
let db = Firestore.firestore()

// Barra de estado a las 9:41
#if DEBUG
    import SimulatorStatusMagic
#endif

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {

        // Configurar XCGLogger
        log.setup(level: .debug, showThreadName: true, showLevel: true, showFileNames: true, showLineNumbers: true, writeToFile: nil, fileLevel: .debug)

        // Habilitar Firebase
        FirebaseApp.configure()

        // Desactivar el modo offline de Firestore
        let settings = FirestoreSettings()
        settings.isPersistenceEnabled = false
        db.settings = settings

        // Barra de estado a las 9:41
        #if DEBUG
            if UserDefaults.standard.bool(forKey: "FASTLANE_SNAPSHOT") {
                // runtime check that we are in snapshot mode
                SDStatusBarManager.sharedInstance().enableOverrides()
            } else {
                SDStatusBarManager.sharedInstance().disableOverrides()
            }
        #endif

        return true
    }

}
