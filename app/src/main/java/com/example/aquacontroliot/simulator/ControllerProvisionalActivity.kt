package com.example.aquacontroliot.simulator

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
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
import com.example.aquacontroliot.databinding.ActivityControllerProvisionalBinding
import kotlinx.coroutines.launch
import java.util.Locale

class ControllerProvisionalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControllerProvisionalBinding
    private lateinit var authRepository: AuthRepository
    private var isUpdatingFromState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControllerProvisionalBinding.inflate(layoutInflater)
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
        isUpdatingFromState = true

        binding.switchSimulation.isChecked = telemetry.isSimulationActive

        // SeekBar
        val progressVal = telemetry.distanceCm.toInt().coerceIn(0, 200)
        if (binding.sbDistance.progress != progressVal) {
            binding.sbDistance.progress = progressVal
        }

        // EditText
        if (!binding.etDistanceInput.hasFocus()) {
            binding.etDistanceInput.setText(String.format(Locale.getDefault(), "%.1f", telemetry.distanceCm))
        }

        // Previsualización de Nivel
        binding.tvSimLevelPreview.text = getString(
            R.string.label_sim_level_preview,
            telemetry.waterLevelPercentage,
            telemetry.distanceCm
        )

        // Estado del Actuador Confirmado
        when (telemetry.confirmedPumpState) {
            PumpState.ON -> {
                binding.tvConfirmedPumpState.text = getString(R.string.label_actuator_state) + " " + getString(R.string.pump_state_on)
                binding.tvConfirmedPumpState.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_status_ok)
                )
            }
            PumpState.OFF -> {
                binding.tvConfirmedPumpState.text = getString(R.string.label_actuator_state) + " " + getString(R.string.pump_state_off)
                binding.tvConfirmedPumpState.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_text_secondary)
                )
            }
        }

        // Gestión de Órdenes Pendientes
        when (telemetry.pendingOrder) {
            PumpOrder.NONE -> {
                binding.tvPendingOrderNotice.text = getString(R.string.no_pending_orders)
                binding.tvPendingOrderNotice.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_text_muted)
                )
                binding.btnAcceptOrder.isEnabled = false
                binding.btnRejectOrder.isEnabled = false
            }
            PumpOrder.REQUEST_ON -> {
                binding.tvPendingOrderNotice.text = getString(R.string.order_pending_on)
                binding.tvPendingOrderNotice.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_accent_amber)
                )
                binding.btnAcceptOrder.isEnabled = true
                binding.btnRejectOrder.isEnabled = true
            }
            PumpOrder.REQUEST_OFF -> {
                binding.tvPendingOrderNotice.text = getString(R.string.order_pending_off)
                binding.tvPendingOrderNotice.setTextColor(
                    ContextCompat.getColor(this, R.color.scada_accent_amber)
                )
                binding.btnAcceptOrder.isEnabled = true
                binding.btnRejectOrder.isEnabled = true
            }
        }

        binding.tvLastOrderResultController.text = telemetry.lastOrderResult

        isUpdatingFromState = false
    }

    private fun setupListeners() {
        binding.switchSimulation.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromState) {
                InMemoryPlantRepository.setSimulationActive(isChecked)
            }
        }

        binding.sbDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingFromState) {
                    InMemoryPlantRepository.updateSensorDistance(progress.toDouble())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.etDistanceInput.doAfterTextChanged { text ->
            if (binding.etDistanceInput.hasFocus() && !isUpdatingFromState) {
                val inputVal = text?.toString()?.toDoubleOrNull()
                if (inputVal != null) {
                    InMemoryPlantRepository.updateSensorDistance(inputVal)
                }
            }
        }

        binding.btnAcceptOrder.setOnClickListener {
            InMemoryPlantRepository.acceptPendingOrder()
            Toast.makeText(this, getString(R.string.order_accepted_toast), Toast.LENGTH_SHORT).show()
        }

        binding.btnRejectOrder.setOnClickListener {
            InMemoryPlantRepository.rejectPendingOrder()
            Toast.makeText(this, getString(R.string.order_rejected_toast), Toast.LENGTH_SHORT).show()
        }

        binding.btnChangeRoleController.setOnClickListener {
            val intent = Intent(this, RoleSelectionActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnLogoutController.setOnClickListener {
            authRepository.logout()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
