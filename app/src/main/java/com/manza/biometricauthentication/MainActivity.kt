package com.manza.biometricauthentication

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import com.manza.biometricauthentication.databinding.ActivityMainBinding
import com.manza.biometricauthentication.util.BiometricAuthenticateUtil

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authenticators =
        BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    private lateinit var enrollBiometricRequestLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        init()

    }

    private fun init() {
        binding.btnBiometricSave.setOnClickListener {
            startAuthenticate()
        }
        //注册生物识别callback
        enrollBiometricRequestLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    startAuthenticate()
                } else {
                    Toast.makeText(this, "没有注册生物识别", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * 开始生物识别认证
     */
    private fun startAuthenticate() {
        BiometricAuthenticateUtil.authenticateOrEnroll(
            this,
            BIOMETRIC_STRONG,
            onEnroll = {
                // 提示用户创建应用程序接受的凭证。
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    enrollBiometricRequestLauncher.launch(
                        Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                            putExtra(
                                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                authenticators
                            )
                        }
                    )
                } else {
                    Toast.makeText(this, "当前设备不支持录入生物识别", Toast.LENGTH_LONG).show()
                }
            }, onAuthenticationSucceeded = {
                Toast.makeText(this, "成功", Toast.LENGTH_LONG).show()
            }
        )
    }

}