package com.example.aquacontroliot.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repositorio compartido en memoria para la telemetría y control de planta de AquaControl IoT.
 * Permite probar el flujo explícito de órdenes y monitoreo dentro del proceso de la app.
 */
object InMemoryPlantRepository {

    private val _telemetryState = MutableStateFlow(PlantTelemetry())
    val telemetryState: StateFlow<PlantTelemetry> = _telemetryState.asStateFlow()

    fun updateSensorDistance(distanceCm: Double) {
        _telemetryState.update { current ->
            current.copy(
                distanceCm = distanceCm.coerceIn(0.0, 500.0),
                lastUpdatedTimestamp = PlantTelemetry.getCurrentTimestamp()
            )
        }
    }

    fun updateCalibration(emptyCm: Double, fullCm: Double): Boolean {
        if (emptyCm <= fullCm || emptyCm <= 0 || fullCm < 0) {
            return false
        }
        _telemetryState.update { current ->
            current.copy(
                emptyDistanceCm = emptyCm,
                fullDistanceCm = fullCm,
                lastUpdatedTimestamp = PlantTelemetry.getCurrentTimestamp()
            )
        }
        return true
    }

    fun setSimulationActive(active: Boolean) {
        _telemetryState.update { current ->
            current.copy(
                isSimulationActive = active,
                lastUpdatedTimestamp = PlantTelemetry.getCurrentTimestamp()
            )
        }
    }

    fun requestPumpOn() {
        _telemetryState.update { current ->
            current.copy(
                pendingOrder = PumpOrder.REQUEST_ON,
                lastOrderResult = "Solicitud enviada: ENCENDER BOMBA (Pendiente)",
                lastUpdatedTimestamp = PlantTelemetry.getCurrentTimestamp()
            )
        }
    }

    fun requestPumpOff() {
        _telemetryState.update { current ->
            current.copy(
                pendingOrder = PumpOrder.REQUEST_OFF,
                lastOrderResult = "Solicitud enviada: APAGAR BOMBA (Pendiente)",
                lastUpdatedTimestamp = PlantTelemetry.getCurrentTimestamp()
            )
        }
    }

    fun acceptPendingOrder() {
        _telemetryState.update { current ->
            when (current.pendingOrder) {
                PumpOrder.REQUEST_ON -> current.copy(
                    confirmedPumpState = PumpState.ON,
                    pendingOrder = PumpOrder.NONE,
                    lastOrderResult = "ORDEN ACEPTADA: Bomba ENCENDIDA",
                    lastUpdatedTimestamp = PlantTelemetry.getCurrentTimestamp()
                )
                PumpOrder.REQUEST_OFF -> current.copy(
                    confirmedPumpState = PumpState.OFF,
                    pendingOrder = PumpOrder.NONE,
                    lastOrderResult = "ORDEN ACEPTADA: Bomba APAGADA",
                    lastUpdatedTimestamp = PlantTelemetry.getCurrentTimestamp()
                )
                PumpOrder.NONE -> current
            }
        }
    }

    fun rejectPendingOrder() {
        _telemetryState.update { current ->
            if (current.pendingOrder != PumpOrder.NONE) {
                current.copy(
                    pendingOrder = PumpOrder.NONE,
                    lastOrderResult = "ORDEN RECHAZADA por Controlador (Estado sin cambios)",
                    lastUpdatedTimestamp = PlantTelemetry.getCurrentTimestamp()
                )
            } else {
                current
            }
        }
    }
}
