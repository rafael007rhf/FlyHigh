package com.ifpr.wearostemplate.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.ifpr.wearostemplate.R
import com.ifpr.wearostemplate.presentation.baseclasses.CorridaStore
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class PerfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_perfil)
        val corridas = CorridaStore(this).listar()
        findViewById<TextView>(R.id.txtResumo).text = getString(R.string.resumo_historico, corridas.size, corridas.sumOf { it.distanciaKm })
        val lista = findViewById<LinearLayout>(R.id.listaCorridas)
        if (corridas.isEmpty()) adicionarLinha(lista, getString(R.string.historico_vazio))
        corridas.take(20).forEach { corrida ->
            val data = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(corrida.dataHora))
            val tempo = String.format(Locale.getDefault(), "%02d:%02d", corrida.tempoSegundos / 60, corrida.tempoSegundos % 60)
            adicionarLinha(lista, getString(R.string.item_corrida, data, corrida.distanciaKm, tempo, corrida.calorias))
        }
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }
    private fun adicionarLinha(lista: LinearLayout, texto: String) {
        lista.addView(TextView(this).apply {
            text = texto; setTextColor(getColor(R.color.flyhigh_white)); textSize = 9f
            setPadding(12, 10, 12, 10); setBackgroundResource(R.drawable.bg_metric_card)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 6 }
        })
    }
}
