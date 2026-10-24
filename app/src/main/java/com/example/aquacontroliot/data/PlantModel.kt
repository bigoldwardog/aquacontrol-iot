package com.example.aquacontroliot.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PumpState {
    OFF,
    ON
}

enum class PumpOrder {
    NONE,
    REQUEST_ON,
    REQUEST_OFF
}

data class PlantTelemetry(
    val distanceCm: Double = 110.0,
    val emptyDistanceCm: Double = 200.0,
    val fullDistanceCm: Double = 20.0,
    val isSimulationActive: Boolean = true,
    val confirmedPumpState: PumpState = PumpState.OFF,
    val pendingOrder: PumpOrder = PumpOrder.NONE,
    val lastOrderResult: String = "Sin órdenes procesadas",
    val lastUpdatedTimestamp: String = getCurrentTimestamp()
) {
    /**
     * Valida que la calibración del tanque sea físicamente coherente.
     */
    val isCalibrationValid: Boolean
        get() = emptyDistanceCm > fullDistanceCm && emptyDistanceCm > 0 && fullDistanceCm >= 0

    /**
     * Calcula el porcentaje del nivel de agua (0% a 100%).
     * nivel (%) = (distancia_vacio_cm - distancia_medida_cm) / (distancia_vacio_cm - distancia_lleno_cm) * 100
     */
    val waterLevelPercentage: Double
        get() {
            if (!isCalibrationValid) return 0.0
            val range = emptyDistanceCm - fullDistanceCm
            val level = ((emptyDistanceCm - distanceCm) / range) * 100.0
            return level.coerceIn(0.0, 100.0)
        }

    companion object {
        fun getCurrentTimestamp(): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
