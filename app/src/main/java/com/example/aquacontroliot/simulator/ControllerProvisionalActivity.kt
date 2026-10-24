package com.example.aquacontroliot.simulator

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aquacontroliot.auth.AuthRepository
import com.example.aquacontroliot.auth.LoginActivity
import com.example.aquacontroliot.auth.RoleSelectionActivity
import com.example.aquacontroliot.databinding.ActivityControllerProvisionalBinding

class ControllerProvisionalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControllerProvisionalBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControllerProvisionalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        setupListeners()
    }

    private fun setupListeners() {
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
