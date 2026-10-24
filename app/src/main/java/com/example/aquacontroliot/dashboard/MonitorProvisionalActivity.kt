package com.example.aquacontroliot.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.aquacontroliot.R
import com.example.aquacontroliot.auth.AuthRepository
import com.example.aquacontroliot.auth.LoginActivity
import com.example.aquacontroliot.auth.RoleSelectionActivity
import com.example.aquacontroliot.data.InMemoryPlantRepository
import com.example.aquacontroliot.data.PlantTelemetry
import com.example.aquacontroliot.data.PumpOrder
import com.example.aquacontroliot.data.PumpState
import com.example.aquacontroliot.databinding.ActivityMonitorProvisionalBinding
import kotlinx.coroutines.launch
import java.util.Locale

class MonitorProvisionalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitorProvisionalBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitorProvisionalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        setupListeners()
        observeTelemetry()
    }

    private fun observeTelemetry() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                InMemoryPlantRepository.telemetryState.collect { telemetry ->
                    updateUI(telemetry)
                }
            }
        }
    }

    private fun updateUI(telemetry: PlantTelemetry) {
        // Nivel de Agua y Tanque
        val levelPct = telemetry.waterLevelPercentage
        binding.tvWaterLevelPercentage.text = String.format(Locale.getDefault(), "%.1f %%", levelPct)

        // Actualización dinámica de la barra de llenado de agua
        binding.frameTankOuter.post {
            val totalHeight = binding.frameTankOuter.height - binding.frameTankOuter.paddingTop - binding.frameTankOuter.paddingBottom
            val fillHeight = (totalHeight * (levelPct / 100.0)).toInt().coerceIn(0, totalHeight)
            val params = binding.viewWaterFill.layoutParams
            params.height = fillHeight
            binding.viewWaterFill.layoutParams = params
        }

        // Lecturas ultrasónicas y calibración
        binding.tvSensorDistance.text = getString(R.string.label_sensor_distance, telemetry.distanceCm)
        binding.tvCalibrationInfo.text = getString(
            R.string.label_calibration_info,
            telemetry.emptyDistanceCm,
            telemetry.fullDistanceCm
        )
        binding.tvLastUpdated.text = getString(R.string.label_last_update, telemetry.lastUpdatedTimestamp)

        // Campos de calibración (pre-rellenados si el usuario no los está editando)
        if (!binding.etEmptyDistance.hasFocus()) {
            binding.etEmptyDistance.setText(telemetry.emptyDistanceCm.toString())
        }
        if (!binding.etFullDistance.hasFocus()) {
            binding.etFullDistance.setText(telemetry.fullDistanceCm.toString())
        }

        // Estado Confirmado de la Bomba
        when (telemetry.confirmedPumpState) {
            PumpState.ON -> {
                binding.imgPumpStatusLed.setColorFilter(
                    ContextCompat.getColor(this, R.color.scada_status_ok)
                )
                binding.tvPumpStatusState.text = getString(R.string.pump_state_on)
                binding.tvPumpStatusState.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_status_ok)
                )
            }
            PumpState.OFF -> {
                binding.imgPumpStatusLed.setColorFilter(
                    ContextCompat.getColor(this, R.color.scada_text_muted)
                )
                binding.tvPumpStatusState.text = getString(R.string.pump_state_off)
                binding.tvPumpStatusState.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_text_primary)
                )
            }
        }

        // Estado de Solicitudes Pendientes
        when (telemetry.pendingOrder) {
            PumpOrder.NONE -> {
                binding.tvPendingOrderStatus.text = getString(R.string.order_none)
                binding.tvPendingOrderStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_text_muted)
                )
            }
            PumpOrder.REQUEST_ON -> {
                binding.tvPendingOrderStatus.text = getString(R.string.order_pending_on)
                binding.tvPendingOrderStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_accent_amber)
                )
            }
            PumpOrder.REQUEST_OFF -> {
                binding.tvPendingOrderStatus.text = getString(R.string.order_pending_off)
                binding.tvPendingOrderStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_accent_amber)
                )
            }
        }

        binding.tvLastOrderResult.text = telemetry.lastOrderResult
    }

    private fun setupListeners() {
        binding.btnRequestPumpOn.setOnClickListener {
            InMemoryPlantRepository.requestPumpOn()
        }

        binding.btnRequestPumpOff.setOnClickListener {
            InMemoryPlantRepository.requestPumpOff()
        }

        binding.btnSaveCalibration.setOnClickListener {
            val emptyText = binding.etEmptyDistance.text?.toString()?.trim().orEmpty()
            val fullText = binding.etFullDistance.text?.toString()?.trim().orEmpty()

            val emptyVal = emptyText.toDoubleOrNull()
            val fullVal = fullText.toDoubleOrNull()

            if (emptyVal == null || fullVal == null) {
                Toast.makeText(this, getString(R.string.error_invalid_calibration), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val success = InMemoryPlantRepository.updateCalibration(emptyVal, fullVal)
            if (success) {
                binding.etEmptyDistance.clearFocus()
                binding.etFullDistance.clearFocus()
                Toast.makeText(this, getString(R.string.calibration_updated_toast), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.error_invalid_calibration), Toast.LENGTH_LONG).show()
            }
        }

        binding.btnChangeRoleMonitor.setOnClickListener {
            val intent = Intent(this, RoleSelectionActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnLogoutMonitor.setOnClickListener {
            authRepository.logout()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
