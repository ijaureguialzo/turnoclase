//
//  TurnoClaseUITests.swift
//  TurnoClaseUITests
//
//  Created by Ion Jaureguialzo Sarasola on 17/11/17.
//  Copyright © 2017 Ion Jaureguialzo Sarasola. All rights reserved.
//

import XCTest

class TurnoClaseUITests: XCTestCase {

    override func setUp() {
        super.setUp()

        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // Fastlane
        let app = XCUIApplication()
        setupSnapshot(app)
        app.launch()

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }

    // REF: Quickstart: https://docs.fastlane.tools/getting-started/ios/screenshots/
    // REF: Poner el 9:41 en el interfaz: https://github.com/shinydevelopment/SimulatorStatusMagic

    func testFastlaneSnapshots() {
        // Use recording to get started writing UI tests.
        // Use XCTAssert and related functions to verify your tests produce the correct results.

        let app = XCUIApplication()

        snapshot("01-PantallaLogin")

        app.buttons["botonConectar"].tap()

        app.buttons["botonActualizar"].tap()
        snapshot("02-Faltan2")

        app.buttons["botonActualizar"].tap()
        snapshot("03-Faltan1")

        app.buttons["botonActualizar"].tap()
        snapshot("04-EsTuTurno")

        app.buttons["botonActualizar"].tap()
        snapshot("05-Terminado")

    }

}
