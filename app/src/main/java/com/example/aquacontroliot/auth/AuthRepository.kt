package com.example.aquacontroliot.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

sealed class AuthResult {
    data class Success(val email: String, val isDemoMode: Boolean) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(private val context: Context) {

    /**
     * Verifica de manera segura si Firebase se encuentra inicializado en la aplicación
     * (requiere la presencia de google-services.json en la carpeta /app).
     */
    fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Obtiene el correo del operador actual autenticado.
     */
    fun getCurrentUserEmail(): String {
        if (isFirebaseConfigured()) {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user?.email != null) {
                    return user.email!!
                }
            } catch (_: Exception) {
            }
        }
        return "operador.demo@aquacontrol.iot"
    }

    /**
     * Inicia sesión utilizando Firebase Auth si está disponible,
     * o cae de manera segura al modo Demo con indicación visual.
     */
    fun login(email: String, password: String, onResult: (AuthResult) -> Unit) {
        if (isFirebaseConfigured()) {
            try {
                val auth = FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val currentUserEmail = auth.currentUser?.email ?: email
                            onResult(AuthResult.Success(currentUserEmail, isDemoMode = false))
                        } else {
                            val errorMessage = task.exception?.localizedMessage
                                ?: "Error de autenticación con Firebase"
                            onResult(AuthResult.Error(errorMessage))
                        }
                    }
            } catch (_: Exception) {
                // Caída segura a modo Demo si ocurre algún problema con el SDK de Firebase
                onResult(AuthResult.Success(email, isDemoMode = true))
            }
        } else {
            // Modo Demo activo por ausencia de google-services.json
            onResult(AuthResult.Success(email, isDemoMode = true))
        }
    }

    /**
     * Cierra la sesión activa del usuario.
     */
    fun logout() {
        if (isFirebaseConfigured()) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {
            }
        }
    }
}
