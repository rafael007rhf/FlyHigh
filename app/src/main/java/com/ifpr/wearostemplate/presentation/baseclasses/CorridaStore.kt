package com.ifpr.wearostemplate.presentation.baseclasses

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CorridaStore(context: Context) {
    private val prefs = context.getSharedPreferences("flyhigh_corridas", Context.MODE_PRIVATE)
    fun salvar(corrida: Corrida) {
        val array = JSONArray(prefs.getString(CHAVE, "[]"))
        array.put(JSONObject().apply {
            put("distanciaKm", corrida.distanciaKm); put("tempoSegundos", corrida.tempoSegundos)
            put("dataHora", corrida.dataHora); put("calorias", corrida.calorias)
            put("batimentosMedios", corrida.batimentosMedios)
        })
        prefs.edit().putString(CHAVE, array.toString()).apply()
    }
    fun listar(): List<Corrida> {
        val array = JSONArray(prefs.getString(CHAVE, "[]"))
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            Corrida(item.optDouble("distanciaKm"), item.optLong("tempoSegundos"), item.optLong("dataHora"), item.optInt("calorias"), item.optInt("batimentosMedios"))
        }.reversed()
    }
    companion object { private const val CHAVE = "historico" }
}
