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
//  TurnoClase
//
//  Created by widemos on 15/6/15.
//

import UIKit

import TurnoClaseShared

class ViewController: UIViewController, UITextFieldDelegate {

    @IBOutlet weak var textoAula: UITextField!
    @IBOutlet weak var textoUsuario: UITextField!

    var codigoAula: String!
    var nombreUsuario: String!

    override func viewDidLoad() {
        super.viewDidLoad()
        self.textoAula.delegate = self
        self.textoUsuario.delegate = self

        self.textoAula.nextField = self.textoUsuario

        // REF: Nombre al azar
        let nombre = Nombres.aleatorio()
        textoUsuario.placeholder = nombre

        if #available(iOS 13.0, *) {
            textoAula.textColor = .black
            textoAula.backgroundColor = .white
            // REF: Color del placeholder: https://stackoverflow.com/a/43346157
            textoAula.attributedPlaceholder = NSAttributedString(string: "BE131", attributes: [NSAttributedString.Key.foregroundColor: UIColor(red: 0, green: 0, blue: 0.0980392, alpha: 0.22)])
            textoUsuario.textColor = .black
            textoUsuario.backgroundColor = .white
            textoUsuario.attributedPlaceholder = NSAttributedString(string: nombre, attributes: [NSAttributedString.Key.foregroundColor: UIColor(red: 0, green: 0, blue: 0.0980392, alpha: 0.22)])
        }

        // Cargar los datos anteriores
        // REF: https://betterprogramming.pub/userdefaults-in-swift-4-d1a278a0ec79
        textoAula.text = UserDefaults.standard.string(forKey: "codigoAula")
        textoUsuario.text = UserDefaults.standard.string(forKey: "nombreUsuario")

        // Depuración
        #if DEBUG
            if textoAula.text!.isEmpty {
                log.debug("Generando datos de prueba...")
                textoAula.text = "BE131"
                textoUsuario.text = nombre
            }
        #endif
    }

    override func shouldPerformSegue(withIdentifier identifier: String, sender: Any?) -> Bool {

        // Comprobar si saltamos a la siguiente pantalla o no
        return identifier == "siguientePantalla" && codigoAula.count >= 5 && nombreUsuario.count >= 2

    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {

        // Pasar los datos a la siguiente pantalla
        if segue.identifier == "siguientePantalla" {
            let c = segue.destination as! TurnoViewController

            c.codigoAula = codigoAula
            c.nombreUsuario = nombreUsuario
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {

        // Ocultar el teclado al pulsar en la vista
        view.endEditing(true)
        super.touchesBegan(touches, with: event)
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {

        // Botón de Intro en los campos de texto

        if let nextField = textField.nextField {
            nextField.becomeFirstResponder()
        } else {
            self.view.endEditing(true)
        }

        return true
    }

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {

        // Control de la longitud máxima
        if (range.length + range.location > textField.text!.count) {
            return false
        }

        let newLength = textField.text!.count + string.count - range.length

        switch textField.tag {
        case 10:
            return newLength <= 5
        case 20:
            return newLength <= 15
        default:
            return newLength <= 15
        }
    }

    @IBAction func botonConectar(_ sender: UIButton) {

        efectoBoton(sender)

        codigoAula = textoAula.text!.uppercased()
        nombreUsuario = textoUsuario.text!

        // Guardar el último aula y usuario
        UserDefaults.standard.set(codigoAula, forKey: "codigoAula")
        UserDefaults.standard.set(nombreUsuario, forKey: "nombreUsuario")
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

// Hacer que el botón Siguiente del teclado virtual salte al siguiente campo
// http://stackoverflow.com/questions/27028617/using-next-as-a-return-key

private var kAssociationKeyNextField: UInt8 = 0

extension UITextField {
    var nextField: UITextField? {
        get {
            return objc_getAssociatedObject(self, &kAssociationKeyNextField) as? UITextField
        }
        set(newField) {
            objc_setAssociatedObject(self, &kAssociationKeyNextField, newField, objc_AssociationPolicy.OBJC_ASSOCIATION_RETAIN)
        }
    }
}
