package com.example.aquacontroliot.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.aquacontroliot.R
import com.example.aquacontroliot.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        if (authRepository.isFirebaseConfigured()) {
            binding.tvAuthStatusBanner.text = getString(R.string.firebase_active_banner)
            binding.tvAuthStatusBanner.setTextColor(
                ContextCompat.getColor(this, R.color.scada_status_ok)
            )
        } else {
            binding.tvAuthStatusBanner.text = getString(R.string.demo_mode_banner)
            binding.tvAuthStatusBanner.setTextColor(
                ContextCompat.getColor(this, R.color.scada_accent_amber)
            )
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            isValid = false
        }

        if (!isValid) return

        // Mostrar indicador de carga
        setLoadingState(true)

        authRepository.login(email, password) { result ->
            setLoadingState(false)
            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(
                        this,
                        getString(R.string.auth_success_toast),
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this, RoleSelectionActivity::class.java).apply {
                        putExtra(EXTRA_USER_EMAIL, result.email)
                    }
                    startActivity(intent)
                    finish()
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this,
                        getString(R.string.auth_failed_toast, result.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    companion object {
        const val EXTRA_USER_EMAIL = "extra_user_email"
    }
}
