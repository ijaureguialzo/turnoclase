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

#if DEBUG
    import FirebaseAppCheck
#endif

// Servicio de logs XCGLogger
let log = XCGLogger.default

// Conexión a Firestore
let db = Firestore.firestore()

// Detectar el estado de la conexión de red
import Reachability

let reachability = try! Reachability()

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {

        // Tamaño de la ventana
        if #available(iOS 13.0, *) {
            UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.forEach { windowScene in
                windowScene.sizeRestrictions?.minimumSize = CGSize(width: 680, height: 680)
                windowScene.sizeRestrictions?.maximumSize = CGSize(width: 680, height: 680)
            }
        }

        // Configurar XCGLogger
        log.setup(level: .debug, showThreadName: true, showLevel: true, showFileNames: true, showLineNumbers: true, writeToFile: nil, fileLevel: .debug)

        // Debug de Firebase App Check
        #if DEBUG
            let providerFactory = AppCheckDebugProviderFactory()
            AppCheck.setAppCheckProviderFactory(providerFactory)
        #endif
        
        // Habilitar Firebase
        FirebaseApp.configure()

        // REF: Aviso de "missing Push Entitlement" al enviar a la App Store por incluir Firebase: https://stackoverflow.com/a/46802075/5136913

        // Opciones de Firestore
        let settings = FirestoreSettings()

        // Use memory-only cache
        settings.cacheSettings = MemoryCacheSettings(garbageCollectorSettings: MemoryLRUGCSettings())

        db.settings = settings

        // Valor por defecto para el sonido
        UserDefaults.standard.register(defaults: [
            "QUEUE_NOT_EMPTY_SOUND": true
            ])

        return true
    }

}


