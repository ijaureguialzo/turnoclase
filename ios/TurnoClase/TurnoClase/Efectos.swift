//
//  Efectos.swift
//  TurnoClase
//
//  Created by Ion Jaureguialzo Sarasola on 07/10/2018.
//  Copyright © 2018 Ion Jaureguialzo Sarasola. All rights reserved.
//

import UIKit

extension UIViewController {

    func feedbackTactil(alerta: Bool = false) {

        // REF: Feedback tactil: https://www.hackingwithswift.com/example-code/uikit/how-to-generate-haptic-feedback-with-uifeedbackgenerator
        if #available(iOS 10, *) {
            if !alerta {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
            } else {
                UINotificationFeedbackGenerator().notificationOccurred(.warning)
            }
        }
    }

}
