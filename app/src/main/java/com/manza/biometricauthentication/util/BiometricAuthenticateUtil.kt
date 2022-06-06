package com.manza.biometricauthentication.util

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity


/**
 *  author : yongfeng.li
 *  date : 6/6/2022 16:18
 *  description :生物识别工具类
 */
object BiometricAuthenticateUtil {
    val tag: String = "BiometricUtil"

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

    fun authenticateOrEnroll(
        context: Context?,
        authenticators: Int,
        onEnroll: () -> Unit,
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
                    authenticate(
                        context,
                        onAuthenticationSucceeded,
                        onAuthenticationError,
                        onAuthenticationFailed,
                    )
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    //没有录入生物特征
                    onEnroll()
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

    //显示弹窗
    private fun authenticate(
        context: Context?,
        onAuthenticationSucceeded: (result: BiometricPrompt.AuthenticationResult) -> Unit,
        onAuthenticationError: (
            errorCode: Int,
            errString: CharSequence
        ) -> Unit = { _: Int, _: CharSequence -> },
        onAuthenticationFailed: () -> Unit = {},
    ) {
        context?.let {
            if (it is FragmentActivity) {
                BiometricPrompt(
                    it,
                    ContextCompat.getMainExecutor(it),
                    object : BiometricPrompt.AuthenticationCallback() {
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
                    }).let {
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("title")
                        .setSubtitle("subtitle")
                        .setNegativeButtonText("setNegativeButtonText")
                        .build().run {
                            it.authenticate(this)
                        }
                }
            }
        }
    }
}