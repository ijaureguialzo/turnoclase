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
//  TurnoClaseProfesor
//
//  Created by widemos on 10/7/15.
//

import UIKit

@IBDesignable class BotonGrafico: UIButton {

    @IBInspectable var colorForma: UIColor = UIColor.black

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
        let bezierPath = UIBezierPath()
        bezierPath.move(to: CGPoint(x: 43.72, y: 20.25))
        bezierPath.addLine(to: CGPoint(x: 42.51, y: 21.47))
        bezierPath.addLine(to: CGPoint(x: 55.71, y: 34.76))
        bezierPath.addLine(to: CGPoint(x: 12.27, y: 34.76))
        bezierPath.addLine(to: CGPoint(x: 12.27, y: 36.49))
        bezierPath.addLine(to: CGPoint(x: 55.71, y: 36.49))
        bezierPath.addLine(to: CGPoint(x: 42.51, y: 49.78))
        bezierPath.addLine(to: CGPoint(x: 43.72, y: 51))
        bezierPath.addLine(to: CGPoint(x: 59, y: 35.63))
        bezierPath.addLine(to: CGPoint(x: 43.72, y: 20.25))
        bezierPath.close()
        bezierPath.miterLimit = 4

        colorForma.setFill()
        bezierPath.fill()
    }

    func dibujarFlechaActualizar() {
        let bezierPath = UIBezierPath()
        bezierPath.move(to: CGPoint(x: 51.63, y: 36.18))
        bezierPath.addCurve(to: CGPoint(x: 47.13, y: 46.96), controlPoint1: CGPoint(x: 51.57, y: 40.25), controlPoint2: CGPoint(x: 49.98, y: 44.08))
        bezierPath.addCurve(to: CGPoint(x: 25.37, y: 46.96), controlPoint1: CGPoint(x: 41.13, y: 53.03), controlPoint2: CGPoint(x: 31.37, y: 53.03))
        bezierPath.addCurve(to: CGPoint(x: 25.37, y: 24.93), controlPoint1: CGPoint(x: 19.37, y: 40.89), controlPoint2: CGPoint(x: 19.37, y: 31))
        bezierPath.addCurve(to: CGPoint(x: 46.17, y: 24.04), controlPoint1: CGPoint(x: 31.06, y: 19.17), controlPoint2: CGPoint(x: 40.13, y: 18.87))
        bezierPath.addLine(to: CGPoint(x: 40.72, y: 25.46))
        bezierPath.addLine(to: CGPoint(x: 41, y: 26.81))
        bezierPath.addLine(to: CGPoint(x: 48.86, y: 24.93))
        bezierPath.addLine(to: CGPoint(x: 48.82, y: 24.89))
        bezierPath.addLine(to: CGPoint(x: 48.85, y: 24.88))
        bezierPath.addLine(to: CGPoint(x: 47.32, y: 17))
        bezierPath.addLine(to: CGPoint(x: 46, y: 17.35))
        bezierPath.addLine(to: CGPoint(x: 47.11, y: 23.04))
        bezierPath.addCurve(to: CGPoint(x: 24.4, y: 23.95), controlPoint1: CGPoint(x: 40.54, y: 17.36), controlPoint2: CGPoint(x: 30.62, y: 17.66))
        bezierPath.addCurve(to: CGPoint(x: 24.4, y: 47.94), controlPoint1: CGPoint(x: 17.87, y: 30.57), controlPoint2: CGPoint(x: 17.87, y: 41.32))
        bezierPath.addCurve(to: CGPoint(x: 48.1, y: 47.94), controlPoint1: CGPoint(x: 30.93, y: 54.55), controlPoint2: CGPoint(x: 41.56, y: 54.55))
        bezierPath.addCurve(to: CGPoint(x: 53, y: 36.2), controlPoint1: CGPoint(x: 51.19, y: 44.8), controlPoint2: CGPoint(x: 52.94, y: 40.63))
        bezierPath.addLine(to: CGPoint(x: 51.63, y: 36.18))
        bezierPath.close()
        bezierPath.miterLimit = 4

        colorForma.setFill()
        bezierPath.fill()
    }

    func dibujarCruzCancelar() {
        let bezierPath = UIBezierPath()
        bezierPath.move(to: CGPoint(x: 52, y: 21.61))
        bezierPath.addLine(to: CGPoint(x: 51.02, y: 20.63))
        bezierPath.addLine(to: CGPoint(x: 36.32, y: 35.34))
        bezierPath.addLine(to: CGPoint(x: 21.61, y: 20.63))
        bezierPath.addLine(to: CGPoint(x: 20.64, y: 21.61))
        bezierPath.addLine(to: CGPoint(x: 35.34, y: 36.32))
        bezierPath.addLine(to: CGPoint(x: 20.64, y: 51.02))
        bezierPath.addLine(to: CGPoint(x: 21.61, y: 52))
        bezierPath.addLine(to: CGPoint(x: 36.32, y: 37.29))
        bezierPath.addLine(to: CGPoint(x: 51.02, y: 52))
        bezierPath.addLine(to: CGPoint(x: 52, y: 51.02))
        bezierPath.addLine(to: CGPoint(x: 37.29, y: 36.32))
        bezierPath.addLine(to: CGPoint(x: 52, y: 21.61))
        bezierPath.close()
        bezierPath.miterLimit = 4

        colorForma.setFill()
        bezierPath.fill()
    }
}
