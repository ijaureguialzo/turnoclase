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
//  BotonGrafico.swift
//  TurnoClase
//
//  Created by widemos on 10/7/15.
//

import UIKit

@IBDesignable class BotonGrafico: UIButton {

    @IBInspectable var colorForma: UIColor = UIColor.clear

    @IBInspectable var forma: Int = 0

    /*
     0: Siguiente
     1: Actualizar
     2: Cancelar
     */

    override func draw(_ rect: CGRect) {

        switch forma {
        case 0:
            dibujarFlechaSiguiente()
        case 1:
            dibujarFlechaActualizar()
        case 2:
            dibujarCruzCancelar()
        default:
            break
        }
    }

    func dibujarFlechaSiguiente() {
        let bezier2Path = UIBezierPath()
        bezier2Path.move(to: CGPoint(x: 43.72, y: 20.4))
        bezier2Path.addLine(to: CGPoint(x: 42.51, y: 21.62))
        bezier2Path.addLine(to: CGPoint(x: 55.71, y: 34.91))
        bezier2Path.addLine(to: CGPoint(x: 12.27, y: 34.91))
        bezier2Path.addLine(to: CGPoint(x: 12.27, y: 36.64))
        bezier2Path.addLine(to: CGPoint(x: 55.71, y: 36.64))
        bezier2Path.addLine(to: CGPoint(x: 42.51, y: 49.93))
        bezier2Path.addLine(to: CGPoint(x: 43.72, y: 51.15))
        bezier2Path.addLine(to: CGPoint(x: 59, y: 35.78))
        bezier2Path.addLine(to: CGPoint(x: 43.72, y: 20.4))
        bezier2Path.close()
        colorForma.setFill()
        bezier2Path.fill()
    }

    func dibujarFlechaActualizar() {
        let bezierPath = UIBezierPath()
        bezierPath.move(to: CGPoint(x: 51.43, y: 36.18))
        bezierPath.addCurve(to: CGPoint(x: 46.93, y: 46.96), controlPoint1: CGPoint(x: 51.37, y: 40.25), controlPoint2: CGPoint(x: 49.78, y: 44.08))
        bezierPath.addCurve(to: CGPoint(x: 25.17, y: 46.96), controlPoint1: CGPoint(x: 40.93, y: 53.03), controlPoint2: CGPoint(x: 31.17, y: 53.03))
        bezierPath.addCurve(to: CGPoint(x: 25.17, y: 24.93), controlPoint1: CGPoint(x: 19.17, y: 40.89), controlPoint2: CGPoint(x: 19.17, y: 31))
        bezierPath.addCurve(to: CGPoint(x: 45.97, y: 24.04), controlPoint1: CGPoint(x: 30.86, y: 19.17), controlPoint2: CGPoint(x: 39.93, y: 18.87))
        bezierPath.addLine(to: CGPoint(x: 40.52, y: 25.46))
        bezierPath.addLine(to: CGPoint(x: 40.8, y: 26.81))
        bezierPath.addLine(to: CGPoint(x: 48.66, y: 24.93))
        bezierPath.addLine(to: CGPoint(x: 48.62, y: 24.89))
        bezierPath.addLine(to: CGPoint(x: 48.65, y: 24.88))
        bezierPath.addLine(to: CGPoint(x: 47.12, y: 17))
        bezierPath.addLine(to: CGPoint(x: 45.8, y: 17.35))
        bezierPath.addLine(to: CGPoint(x: 46.91, y: 23.04))
        bezierPath.addCurve(to: CGPoint(x: 24.2, y: 23.95), controlPoint1: CGPoint(x: 40.34, y: 17.36), controlPoint2: CGPoint(x: 30.42, y: 17.66))
        bezierPath.addCurve(to: CGPoint(x: 24.2, y: 47.94), controlPoint1: CGPoint(x: 17.67, y: 30.57), controlPoint2: CGPoint(x: 17.67, y: 41.32))
        bezierPath.addCurve(to: CGPoint(x: 47.9, y: 47.94), controlPoint1: CGPoint(x: 30.73, y: 54.55), controlPoint2: CGPoint(x: 41.36, y: 54.55))
        bezierPath.addCurve(to: CGPoint(x: 52.8, y: 36.2), controlPoint1: CGPoint(x: 50.99, y: 44.8), controlPoint2: CGPoint(x: 52.74, y: 40.63))
        bezierPath.addLine(to: CGPoint(x: 51.43, y: 36.18))
        bezierPath.close()
        colorForma.setFill()
        bezierPath.fill()
    }

    func dibujarCruzCancelar() {
        let bezierPath = UIBezierPath()
        bezierPath.move(to: CGPoint(x: 51.66, y: 21.28))
        bezierPath.addLine(to: CGPoint(x: 50.69, y: 20.3))
        bezierPath.addLine(to: CGPoint(x: 35.98, y: 35.01))
        bezierPath.addLine(to: CGPoint(x: 21.28, y: 20.3))
        bezierPath.addLine(to: CGPoint(x: 20.3, y: 21.28))
        bezierPath.addLine(to: CGPoint(x: 35.01, y: 35.98))
        bezierPath.addLine(to: CGPoint(x: 20.3, y: 50.69))
        bezierPath.addLine(to: CGPoint(x: 21.28, y: 51.67))
        bezierPath.addLine(to: CGPoint(x: 35.98, y: 36.96))
        bezierPath.addLine(to: CGPoint(x: 50.69, y: 51.67))
        bezierPath.addLine(to: CGPoint(x: 51.66, y: 50.69))
        bezierPath.addLine(to: CGPoint(x: 36.96, y: 35.98))
        bezierPath.addLine(to: CGPoint(x: 51.66, y: 21.28))
        bezierPath.close()
        colorForma.setFill()
        bezierPath.fill()
    }
}
