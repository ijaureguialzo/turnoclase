//
//  Teclado.swift
//  TurnoClaseProfesor
//
//  Created by Ion Jaureguialzo Sarasola on 07/10/2018.
//  Copyright © 2018 Ion Jaureguialzo Sarasola. All rights reserved.
//

import UIKit

extension UIViewController {

    @objc func textFieldShouldReturn(_ textField: UITextField) -> Bool {

        // Botón de Intro en los campos de texto

        if let nextField = textField.nextField {
            nextField.becomeFirstResponder()
        } else {
            self.view.endEditing(true)
        }

        return true
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
