//
//  TurnoClase
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

package com.jaureguialzo.turnoclase

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import kotlinx.android.synthetic.main.activity_acerca_de.*

class AcercaDe : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acerca_de)

        // Cargar HTML desde un @string
        etiquetaASL.text = fromHtml(getString(R.string.texto_asl))
        etiquetaASL.movementMethod = LinkMovementMethod.getInstance()

        // Cargar HTML desde un @string y activar los enlaces
        etiquetaLicenciaImagenes.text = fromHtml(getString(R.string.texto_licencia_imagenes))
        etiquetaLicenciaImagenes.movementMethod = LinkMovementMethod.getInstance()

        // Cargar HTML desde un @string
        etiquetaASL.text = fromHtml(getString(R.string.texto_licencia_victor))
        etiquetaASL.movementMethod = LinkMovementMethod.getInstance()
    }

    // REF: Método obsoleto en Android N: https://stackoverflow.com/a/37905107/5136913
    @Suppress("DEPRECATION")
    private fun fromHtml(html: String): Spanned {
        val result: Spanned
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            result = Html.fromHtml(html)
        }
        return result
    }

}
