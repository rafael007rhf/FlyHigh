package com.ifpr.wearostemplate.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.wearostemplate.R
import com.ifpr.wearostemplate.presentation.baseclasses.Corrida
import com.ifpr.wearostemplate.presentation.baseclasses.CorridaStore
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), LocationListener, SensorEventListener {
    private lateinit var btnPlayPause: ImageView
    private lateinit var txtCronometro: TextView
    private lateinit var txtDistancia: TextView
    private lateinit var txtRitmo: TextView
    private lateinit var txtCalorias: TextView
    private lateinit var txtGps: TextView
    private lateinit var txtBatimentos: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private var heartSensor: Sensor? = null
    private var corridaAtiva = false
    private var inicioTrechoMs = 0L
    private var tempoAcumuladoMs = 0L
    private var distanciaMetros = 0f
    private var ultimaLocalizacao: Location? = null
    private var batimentos = 0

    private val permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (temPermissaoLocalizacao() && !corridaAtiva && tempoAcumuladoMs == 0L) iniciarCorrida()
        else iniciarSensoresSePermitido()
    }
    private val atualizarTela = object : Runnable {
        override fun run() {
            if (!corridaAtiva) return
            renderizarMetricas(tempoTotalMs())
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContentView(R.layout.activity_main)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        txtCronometro = findViewById(R.id.txtCronometro)
        txtDistancia = findViewById(R.id.txtDistancia)
        txtRitmo = findViewById(R.id.txtRitmo)
        txtCalorias = findViewById(R.id.txtCalorias)
        txtGps = findViewById(R.id.txtGps)
        txtBatimentos = findViewById(R.id.txtBatimentos)
        findViewById<Button>(R.id.btnPerfil).setOnClickListener { startActivity(Intent(this, PerfilActivity::class.java)) }
        btnPlayPause.setOnClickListener {
            when {
                !temPermissaoLocalizacao() -> pedirPermissoes()
                corridaAtiva -> pausarCorrida()
                tempoAcumuladoMs > 0L -> retomarCorrida()
                else -> iniciarCorrida()
            }
        }
        btnPlayPause.setOnLongClickListener {
            if (corridaAtiva || tempoAcumuladoMs > 0L) finalizarCorrida()
            true
        }
        renderizarMetricas(0L)
    }

    private fun pedirPermissoes() {
        val permissoes = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= 29) permissoes += Manifest.permission.ACTIVITY_RECOGNITION
        permissoes += Manifest.permission.BODY_SENSORS
        permissionsLauncher.launch(permissoes.toTypedArray())
    }

    private fun iniciarCorrida() {
        distanciaMetros = 0f; tempoAcumuladoMs = 0L; batimentos = 0; ultimaLocalizacao = null
        retomarCorrida()
        Toast.makeText(this, R.string.corrida_iniciada, Toast.LENGTH_SHORT).show()
    }

    private fun retomarCorrida() {
        corridaAtiva = true
        inicioTrechoMs = SystemClock.elapsedRealtime()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        btnPlayPause.setImageResource(R.drawable.ic_stop)
        btnPlayPause.contentDescription = getString(R.string.cd_pause)
        iniciarSensoresSePermitido()
        handler.removeCallbacks(atualizarTela); handler.post(atualizarTela)
    }

    private fun pausarCorrida() {
        tempoAcumuladoMs = tempoTotalMs(); corridaAtiva = false
        handler.removeCallbacks(atualizarTela); pararSensores()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        btnPlayPause.setImageResource(R.drawable.ic_play)
        btnPlayPause.contentDescription = getString(R.string.cd_resume)
        Toast.makeText(this, R.string.corrida_pausada, Toast.LENGTH_SHORT).show()
    }

    private fun finalizarCorrida() {
        if (corridaAtiva) tempoAcumuladoMs = tempoTotalMs()
        corridaAtiva = false; handler.removeCallbacks(atualizarTela); pararSensores()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        renderizarMetricas(tempoAcumuladoMs)
        val corrida = Corrida(distanciaMetros / 1000.0, tempoAcumuladoMs / 1000L, System.currentTimeMillis(), calcularCalorias(), batimentos)
        CorridaStore(this).salvar(corrida)
        FirebaseDatabase.getInstance().getReference("corridas").push().setValue(corrida)
        Toast.makeText(this, R.string.corrida_salva, Toast.LENGTH_SHORT).show()
        tempoAcumuladoMs = 0L; distanciaMetros = 0f; ultimaLocalizacao = null
        btnPlayPause.setImageResource(R.drawable.ic_play)
        btnPlayPause.contentDescription = getString(R.string.cd_play)
    }

    private fun iniciarSensoresSePermitido() {
        if (!corridaAtiva) return
        if (temPermissaoLocalizacao()) try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, this)
            txtGps.text = getString(R.string.gps_buscando)
        } catch (_: SecurityException) { txtGps.text = getString(R.string.gps_indisponivel) }
        catch (_: IllegalArgumentException) { txtGps.text = getString(R.string.gps_indisponivel) }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED)
            heartSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    private fun pararSensores() { locationManager.removeUpdates(this); sensorManager.unregisterListener(this) }

    override fun onLocationChanged(location: Location) {
        txtGps.text = getString(R.string.gps_ok)
        ultimaLocalizacao?.let { anterior ->
            val trecho = anterior.distanceTo(location)
            if (location.accuracy <= 30f && trecho in 1f..100f) distanciaMetros += trecho
        }
        ultimaLocalizacao = location
        renderizarMetricas(tempoTotalMs())
    }
    override fun onProviderDisabled(provider: String) { txtGps.text = getString(R.string.gps_desligado) }
    override fun onSensorChanged(event: SensorEvent) {
        val valor = event.values.firstOrNull()?.roundToInt() ?: return
        if (valor in 30..240) { batimentos = valor; txtBatimentos.text = getString(R.string.valor_bpm, valor) }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun renderizarMetricas(tempoMs: Long) {
        txtCronometro.text = formatarTempo(tempoMs)
        txtDistancia.text = getString(R.string.valor_km, distanciaMetros / 1000f)
        val ritmo = if (distanciaMetros >= 50f) (tempoMs / 60000.0) / (distanciaMetros / 1000.0) else 0.0
        txtRitmo.text = if (ritmo > 0) formatarRitmo(ritmo) else getString(R.string.valor_ritmo_vazio)
        txtCalorias.text = getString(R.string.valor_kcal, calcularCalorias())
    }
    private fun tempoTotalMs() = tempoAcumuladoMs + if (corridaAtiva) SystemClock.elapsedRealtime() - inicioTrechoMs else 0L
    private fun calcularCalorias() = (distanciaMetros / 1000f * 70f).roundToInt()
    private fun temPermissaoLocalizacao() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private fun formatarRitmo(minutos: Double): String { val s = (minutos * 60).roundToInt(); return getString(R.string.valor_ritmo, s / 60, s % 60) }
    private fun formatarTempo(ms: Long): String { val s = ms / 1000L; return String.format(Locale.getDefault(), "%02d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60) }
    override fun onDestroy() { handler.removeCallbacks(atualizarTela); pararSensores(); super.onDestroy() }
}
