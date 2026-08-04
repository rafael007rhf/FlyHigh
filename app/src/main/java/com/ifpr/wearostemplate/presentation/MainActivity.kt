/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.ifpr.wearostemplate.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.wearostemplate.R
import com.ifpr.wearostemplate.presentation.baseclasses.Corrida


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnPerfil).setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            startActivity(intent)
        }

        val btnPlayPause = findViewById<ImageView>(R.id.btnPlayPause)

        btnPlayPause.setOnClickListener {
            val distanciaKm = 2.5
            val tempoSegundos = 900L

            salvarCorrida(distanciaKm, tempoSegundos)

            Toast.makeText(
                this,
                "Corrida salva!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun salvarCorrida(
        distanciaKm: Double,
        tempoSegundos: Long
    ) {
        val corrida = Corrida(
            distanciaKm = distanciaKm,
            tempoSegundos = tempoSegundos,
            dataHora = System.currentTimeMillis()
        )

        FirebaseDatabase
            .getInstance()
            .getReference("corridas")
            .push()
            .setValue(corrida)
            .addOnFailureListener { erro ->
                Toast.makeText(
                    this,
                    "Erro ao salvar: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


}

