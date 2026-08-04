package com.ifpr.wearostemplate.presentation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.wearostemplate.R
import com.ifpr.wearostemplate.presentation.baseclasses.Corrida
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var btnPlayPause: ImageView
    private lateinit var txtCronometro: TextView

    private val handler = Handler(Looper.getMainLooper())

    private var corridaAtiva = false
    private var inicioCorridaMs = 0L

    private val atualizarCronometro = object : Runnable {
        override fun run() {
            if (!corridaAtiva) return

            val tempoDecorridoMs =
                SystemClock.elapsedRealtime() - inicioCorridaMs

            txtCronometro.text = formatarTempo(tempoDecorridoMs)

            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)
        setContentView(R.layout.activity_main)

        btnPlayPause = findViewById(R.id.btnPlayPause)
        txtCronometro = findViewById(R.id.txtCronometro)

        findViewById<Button>(R.id.btnPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        btnPlayPause.setOnClickListener {
            if (corridaAtiva) {
                finalizarCorrida()
            } else {
                iniciarCorrida()
            }
        }
    }

    private fun iniciarCorrida() {
        corridaAtiva = true
        inicioCorridaMs = SystemClock.elapsedRealtime()

        txtCronometro.text = "00:00:00"
        btnPlayPause.setImageResource(R.drawable.ic_stop)

        handler.removeCallbacks(atualizarCronometro)
        handler.post(atualizarCronometro)

        Toast.makeText(
            this,
            "Corrida iniciada",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun finalizarCorrida() {
        val tempoDecorridoMs =
            SystemClock.elapsedRealtime() - inicioCorridaMs

        corridaAtiva = false
        handler.removeCallbacks(atualizarCronometro)

        txtCronometro.text = formatarTempo(tempoDecorridoMs)
        btnPlayPause.setImageResource(R.drawable.ic_play)

        val tempoSegundos = tempoDecorridoMs / 1000L

        salvarCorrida(
            distanciaKm = 0.0,
            tempoSegundos = tempoSegundos
        )
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
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Corrida salva!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(
                    this,
                    "Erro ao salvar: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun formatarTempo(tempoMs: Long): String {
        val totalSegundos = tempoMs / 1000L

        val horas = totalSegundos / 3600L
        val minutos = (totalSegundos % 3600L) / 60L
        val segundos = totalSegundos % 60L

        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            horas,
            minutos,
            segundos
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(atualizarCronometro)
        super.onDestroy()
    }
}