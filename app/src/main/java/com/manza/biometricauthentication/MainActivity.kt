package com.manza.biometricauthentication

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import com.manza.biometricauthentication.databinding.ActivityMainBinding
import com.manza.biometricauthentication.util.BiometricAuthenticateUtil
import com.manza.biometricauthentication.util.CryptographyManager
import javax.crypto.Cipher


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authenticators =
        BIOMETRIC_STRONG
    private lateinit var enrollBiometricRequestLauncher: ActivityResultLauncher<Intent>
    private lateinit var cryptographyManager: CryptographyManager
    private val SHARED_PREFS_FILENAME = "biometric_prefs"
    private val CIPHERTEXT_WRAPPER = "ciphertext_wrapper"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        init()

    }

    private fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cryptographyManager = CryptographyManager()
            //注册生物识别callback
            enrollBiometricRequestLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == RESULT_OK) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            startAuthenticate()
                        }
                    }
                }

            binding.btnBiometricEncrypt.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    startAuthenticate()
                }
            }
            binding.btnBiometricDecrypt.setOnClickListener {
                showPromptForDecryption()

            }
            binding.btnClear.setOnClickListener {
                binding.textView.text = "暂无数据"
            }
        }
    }

    /**
     * 开始生物识别认证
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun startAuthenticate() {
        val target = binding.editTextTextPersonName.text.toString().trim()
        if (target.isEmpty()) {
            Toast.makeText(this, "请输入待加密数据", Toast.LENGTH_LONG).show()
            return
        }

        showPromptForEncryption()
    }

    /**
     * 加密存
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPromptForEncryption() {
        if (checkAbility()) return
        BiometricAuthenticateUtil.authenticateOrEnroll(
            this,
            authenticators,
            showAuthenticatePrompt = {
                showBiometricPrompt(
                    cipher = cryptographyManager.getInitializedCipherForEncryption(
                        BiometricAuthenticateUtil.biometricEncryptionDecryptionKey
                    )
                ) { authenticationResult ->
                    authenticationResult.cryptoObject?.cipher?.let { cipher ->
                        cryptographyManager.encryptData(
                            binding.editTextTextPersonName.text.toString().trim(),
                            cipher
                        ).also {
                            cryptographyManager.persistCiphertextWrapperToSharedPrefs(
                                it,
                                applicationContext,
                                SHARED_PREFS_FILENAME,
                                MODE_PRIVATE,
                                CIPHERTEXT_WRAPPER
                            )
                        }
                    }
                }

            },
            ::enroll
        )
    }

    private fun checkAbility(): Boolean {
        if (!BiometricAuthenticateUtil.checkIsBiometricAvailable(this, authenticators)) {
            Toast.makeText(this, "当前设备不支持生物识别", Toast.LENGTH_LONG).show()
            return true
        }
        return false
    }

    /**
     * 解密取
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPromptForDecryption() {
        if (checkAbility()) return
        BiometricAuthenticateUtil.authenticateOrEnroll(
            this,
            authenticators,
            showAuthenticatePrompt = {
                cryptographyManager.getCiphertextWrapperFromSharedPrefs(
                    applicationContext,
                    SHARED_PREFS_FILENAME,
                    Context.MODE_PRIVATE,
                    CIPHERTEXT_WRAPPER
                )?.let { ciphertextWrapper ->
                    showBiometricPrompt(
                        cipher = cryptographyManager.getInitializedCipherForDecryption(
                            BiometricAuthenticateUtil.biometricEncryptionDecryptionKey,
                            ciphertextWrapper.initializationVector
                        )
                    ) { authenticationResult ->
                        authenticationResult.cryptoObject?.cipher?.let { cipher ->
                            cryptographyManager.decryptData(ciphertextWrapper.ciphertext, cipher)
                                .let { decryptedResult ->
                                    binding.textView.text = decryptedResult
                                }
                        }
                    }

                }

            },
            ::enroll
        )

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showBiometricPrompt(
        cipher: Cipher,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        BiometricAuthenticateUtil.authenticate(
            this,
            authenticators,
            cipher = cipher,
            onAuthenticationSucceeded
        )
    }

    private fun enroll() {
        AlertDialog.Builder(this).apply {
            setTitle("请先录入指纹信息")
            setPositiveButton("确定") { dialog, _ ->
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
                    enrollBiometricRequestLauncher.launch(
                        Intent(Settings.ACTION_SETTINGS)
                    )
                }
                dialog.dismiss()
            }
            setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
        }.create().show()
    }
}