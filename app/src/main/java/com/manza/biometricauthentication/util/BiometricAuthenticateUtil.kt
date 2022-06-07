package com.manza.biometricauthentication.util

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher


/**
 *  author : yongfeng.li
 *  date : 6/6/2022 16:18
 *  description :生物识别工具类
 */
object BiometricAuthenticateUtil {
    const val tag: String = "BiometricUtil"
    const val biometricEncryptionDecryptionKey = "biometric_encryption_decryption_key"

    /**
     * 检测是否支持该类型的生物识别
     * @param  {@link Authenticators}
     * 认证类型：BiometricManager.Authenticators.BIOMETRIC_STRONG
     *         BiometricManager.Authenticators.BIOMETRIC_WEAK
     *         BiometricManager.Authenticators.DEVICE_CREDENTIAL
     */
    fun checkIsBiometricAvailable(
        context: Context?,
        authenticators: Int,
    ): Boolean {
        if (context == null) return false
        val canAuthenticate = BiometricManager.from(context).canAuthenticate(authenticators)
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS || canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    //判断认证或录入
    fun authenticateOrEnroll(
        context: Context?,
        authenticators: Int,
        showAuthenticatePrompt: () -> Unit,
        enroll: () -> Unit
    ) {
        context?.let {
            val canAuthenticate = BiometricManager.from(it).canAuthenticate(authenticators)
            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                showAuthenticatePrompt()
            } else if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                enroll()
            }
        }
    }

    /**
     * 认证或登记
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun authenticate(
        context: Context?,
        authenticators: Int,
        cipher: Cipher? = null,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit,
        onAuthenticationError: (
            errorCode: Int,
            errString: CharSequence
        ) -> Unit = { _: Int, _: CharSequence -> },
        onAuthenticationFailed: () -> Unit = {},
    ) {
        context?.let { it ->
            when (val canAuthenticate = BiometricManager.from(it).canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    //可以进行生物识别认证
                    authenticateInternal(
                        context,
                        cipher,
                        onAuthenticationSucceeded,
                        onAuthenticationError,
                        onAuthenticationFailed,
                    )
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.d(tag, "BIOMETRIC_ERROR_NONE_ENROLLED")
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.d(tag, "BIOMETRIC_ERROR_HW_UNAVAILABLE")
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.d(tag, "BIOMETRIC_ERROR_NO_HARDWARE")
                }
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    Log.d(tag, "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED")
                }
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                    Log.d(tag, "BIOMETRIC_ERROR_UNSUPPORTED")
                }
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                    Log.d(tag, "BIOMETRIC_STATUS_UNKNOWN")
                }
                else -> {
                    Log.d(tag, "else bloc and result ===>$canAuthenticate")
                }
            }
        }

    }

    /**
     * 认证或登记
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun authenticate(
        fragment: Fragment?,
        authenticators: Int,
        cipher: Cipher? = null,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit,
        onAuthenticationError: (
            errorCode: Int,
            errString: CharSequence
        ) -> Unit = { _: Int, _: CharSequence -> },
        onAuthenticationFailed: () -> Unit = {},
    ) {
        if (fragment == null) return
        authenticate(
            fragment.activity,
            authenticators,
            cipher,
            onAuthenticationSucceeded,
            onAuthenticationError,
            onAuthenticationFailed
        )
    }

    //认证
    @RequiresApi(Build.VERSION_CODES.M)
    private fun authenticateInternal(
        context: Context?,
        cipher: Cipher? = null,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit,
        onAuthenticationError: (
            errorCode: Int,
            errString: CharSequence
        ) -> Unit = { _: Int, _: CharSequence -> },
        onAuthenticationFailed: () -> Unit = {},
        biometricPrompt: BiometricPrompt? = null
    ) {
        context?.let {
            if (it is FragmentActivity) {
                var biometricPromptTemp: BiometricPrompt? = null
                if (biometricPrompt == null) {
                    biometricPromptTemp = biometricPrompt
                }
                if (biometricPromptTemp == null) {
                    biometricPromptTemp = BiometricPrompt(
                        it,
                        ContextCompat.getMainExecutor(it),
                        authenticationCallback(
                            onAuthenticationError,
                            onAuthenticationSucceeded,
                            onAuthenticationFailed
                        )
                    )
                }
                biometricPromptTemp.let { biometricPrompt ->
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("标题")
                        .setSubtitle("子标题")
                        .setNegativeButtonText("取消")
                        .build().also { promptInfo ->
                            if (cipher == null) {
                                biometricPrompt.authenticate(promptInfo)
                            } else {
                                biometricPrompt.authenticate(
                                    promptInfo,
                                    BiometricPrompt.CryptoObject(
                                        cipher
                                    )
                                )
                            }
                        }
                }
            }
        }
    }

    //识别回调
    private fun authenticationCallback(
        onAuthenticationError: (errorCode: Int, errString: CharSequence) -> Unit,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit,
        onAuthenticationFailed: () -> Unit
    ) = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(
            errorCode: Int,
            errString: CharSequence
        ) {
            super.onAuthenticationError(errorCode, errString)
            Log.d(tag, "errorCode===>>>$errorCode   errString===>>>$errString")
            onAuthenticationError(errorCode, errString)
        }

        override fun onAuthenticationSucceeded(
            result: BiometricPrompt.AuthenticationResult
        ) {
            super.onAuthenticationSucceeded(result)
            onAuthenticationSucceeded(result)
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onAuthenticationFailed()
        }
    }
}