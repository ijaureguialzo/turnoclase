//
//  OperadorOpcional.swift
//  TurnoClase
//
//  Created by Ion Jaureguialzo Sarasola on 20/7/18.
//  Copyright © 2018 Ion Jaureguialzo Sarasola. All rights reserved.
//

import Foundation

// REF: Opcionales e interpolación de strings: https://oleb.net/blog/2016/12/optionals-string-interpolation/

infix operator ???: NilCoalescingPrecedence

public func ???<T>(optional: T?, defaultValue: @autoclosure () -> String) -> String {
    switch optional {
    case let value?: return String(describing: value)
    case nil: return defaultValue()
    }
}
