package com.ifpr.wearostemplate.presentation.baseclasses

data class Corrida(
    var distanciaKm: Double = 0.0,
    var tempoSegundos: Long = 0L,
    var dataHora: Long = 0L
)