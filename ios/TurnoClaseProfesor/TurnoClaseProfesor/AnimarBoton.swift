//
//  AnimarBoton.swift
//  TurnoClaseProfesor
//
//  Created by Ion Jaureguialzo Sarasola on 25/11/17.
//  Copyright © 2017 Ion Jaureguialzo Sarasola. All rights reserved.
//

import UIKit

extension ViewController {

    @IBAction func fadeOut(_ sender: UIButton) {

        // Difuminar
        UIView.animate(withDuration: 0.1,
                       delay: 0,
                       options: UIViewAnimationOptions.curveLinear.intersection(.allowUserInteraction).intersection(.beginFromCurrentState),
                       animations: {
                           sender.alpha = 0.15
                       }, completion: nil)
    }

    @IBAction func fadeIn(_ sender: UIButton) {

        // Restaurar
        UIView.animate(withDuration: 0.3,
                       delay: 0,
                       options: UIViewAnimationOptions.curveLinear.intersection(.allowUserInteraction).intersection(.beginFromCurrentState),
                       animations: {
                           sender.alpha = 1
                       }, completion: nil)
    }

}
