package com.github.aakumykov.cloud_reader_writer.cloud_authenticator

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthSdkContract
import com.yandex.authsdk.YandexAuthToken
import com.yandex.authsdk.internal.strategy.LoginType

class YandexAuthenticator (
    componentActivity: ComponentActivity,
    private val loginType: LoginType,
    private val cloudAuthenticatorCallbacks: CloudAuthenticator.Callbacks
) : CloudAuthenticator {

    private val activityResultLauncher: ActivityResultLauncher<YandexAuthLoginOptions>

    init {
        val yandexAuthOptions = YandexAuthOptions(componentActivity, true)
        val yandexAuthSdkContract = YandexAuthSdkContract(yandexAuthOptions)

        activityResultLauncher = componentActivity.registerForActivityResult(yandexAuthSdkContract) { result ->

            try {
                val yandexAuthToken: YandexAuthToken? = result.getOrThrow()
                cloudAuthenticatorCallbacks.onCloudAuthSuccess(yandexAuthToken!!.value)
            }
            catch (t: Throwable) {
                cloudAuthenticatorCallbacks.onCloudAuthFailed(t)
            }
        }
    }

    override fun startAuth() {
        activityResultLauncher.launch(YandexAuthLoginOptions(loginType))
    }

}