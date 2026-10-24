package com.example.aquacontroliot.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aquacontroliot.R
import com.example.aquacontroliot.dashboard.MonitorProvisionalActivity
import com.example.aquacontroliot.databinding.ActivityRoleSelectionBinding
import com.example.aquacontroliot.simulator.ControllerProvisionalActivity

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val userEmail = intent.getStringExtra(LoginActivity.EXTRA_USER_EMAIL)
            ?: authRepository.getCurrentUserEmail()

        binding.tvUserEmail.text = getString(R.string.role_logged_as, userEmail)
    }

    private fun setupListeners() {
        binding.btnSelectMonitor.setOnClickListener {
            val intent = Intent(this, MonitorProvisionalActivity::class.java)
            startActivity(intent)
        }

        binding.btnSelectController.setOnClickListener {
            val intent = Intent(this, ControllerProvisionalActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            authRepository.logout()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
