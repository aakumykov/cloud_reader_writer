package com.github.aakumykov.cloud_reader_writer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.github.aakumykov.cloud_reader_writer.databinding.ActivityMainBinding
import com.github.aakumykov.cloud_reader_writer.extentions.getStringFromPreferences
import com.github.aakumykov.cloud_reader_writer.extentions.storeStringInPreferences
import com.github.aakumykov.cloud_writer.YandexCloudWriter
import com.github.aakumykov.kotlin_playground.cloud_authenticator.CloudAuthenticator
import com.github.aakumykov.kotlin_playground.cloud_authenticator.YandexAuthenticator
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import com.google.gson.Gson
import com.yandex.authsdk.internal.strategy.LoginType
import okhttp3.OkHttpClient
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), CloudAuthenticator.Callbacks{

    private lateinit var binding: ActivityMainBinding
    private var yandexAuthToken: String? = null
    private lateinit var yandexAuthenticator: CloudAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        yandexAuthenticator = YandexAuthenticator(this, LoginType.NATIVE, this)
        yandexAuthToken = getStringFromPreferences(YANDEX_AUTH_TOKEN)

        binding.yandexAuthButton.setOnClickListener {
            hideError()
            yandexAuthenticator.startAuth()
        }

        binding.createDirButton1.setOnClickListener { createDir1() }

        binding.createDirButton2.setOnClickListener { createDir2() }
    }


    private fun createDir1() {
        createDirReal(binding.absolutePathInput.text.toString())
    }

    private fun createDir2() {
        createDirReal(binding.relativePathInput.text.toString())
    }

    private fun createDirReal(dirName: String) {
        thread {
            try {
                hideError()
                showProgressBar()

                YandexCloudWriter(OkHttpClient.Builder().build(), Gson(), yandexAuthToken!!)
                    .createDirWithParents(dirName)
            }
            catch (t: Throwable) {
                showError(t)
            }
            finally {
                hideProgressBar()
            }
        }
    }

    override fun onCloudAuthSuccess(authToken: String) {
        yandexAuthToken = authToken
        storeStringInPreferences(YANDEX_AUTH_TOKEN, authToken)
    }

    override fun onCloudAuthFailed(throwable: Throwable) {
        showError(throwable)
    }

    private fun showProgressBar() {
        binding.root.post { binding.progressBar.visibility = View.VISIBLE }
    }

    private fun hideProgressBar() {
        binding.root.post { binding.progressBar.visibility = View.GONE }
    }

    private fun showError(throwable: Throwable) {
        binding.root.post {
            val errorMsg = ExceptionUtils.getErrorMessage(throwable)
            with(binding.errorView) {
                text = errorMsg
                visibility = View.VISIBLE
            }
            Log.e(TAG, errorMsg, throwable)
        }
    }

    fun hideError() {
        binding.root.post {
            with(binding.errorView) {
                text = ""
                visibility = View.GONE
            }
        }
    }

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        const val YANDEX_AUTH_TOKEN = "AUTH_TOKEN"
    }
}