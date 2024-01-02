package com.github.aakumykov.cloud_reader_writer

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.aakumykov.cloud_reader_writer.databinding.ActivityMainBinding
import com.github.aakumykov.cloud_reader_writer.extentions.getStringFromPreferences
import com.github.aakumykov.cloud_reader_writer.extentions.storeStringInPreferences
import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.cloud_writer.LocalCloudWriter
import com.github.aakumykov.cloud_writer.YandexCloudWriter
import com.github.aakumykov.kotlin_playground.cloud_authenticator.CloudAuthenticator
import com.github.aakumykov.kotlin_playground.cloud_authenticator.YandexAuthenticator
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import com.google.gson.Gson
import com.yandex.authsdk.internal.strategy.LoginType
import okhttp3.OkHttpClient
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructPermissionsRequest
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), CloudAuthenticator.Callbacks {

    private lateinit var binding: ActivityMainBinding
    private var yandexAuthToken: String? = null
    private lateinit var yandexAuthenticator: CloudAuthenticator
    private lateinit var permissionsRequester: PermissionsRequester

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getStringFromPreferences(DIR_NAME)?.let { binding.pathInput.setText(it) }

        permissionsRequester = constructPermissionsRequest(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            requiresPermission = ::createLocalDir
        )

        yandexAuthenticator = YandexAuthenticator(this, LoginType.NATIVE, this)
        yandexAuthToken = getStringFromPreferences(YANDEX_AUTH_TOKEN)

        binding.yandexAuthButton.setOnClickListener {
            hideError()
            yandexAuthenticator.startAuth()
        }

        binding.createSimpleCloudDirButton.setOnClickListener { createSimpleCloudDir() }
        binding.createDeepCloudDirButton.setOnClickListener { createDeepCloudDir() }
        binding.createLocalDirButton1.setOnClickListener { permissionsRequester.launch() }
    }

    override fun onDestroy() {
        super.onDestroy()
        storeStringInPreferences(DIR_NAME, dirName())
    }

    private fun createSimpleCloudDir() {
        thread {
            try {
                hideError()
                showProgressBar()
                yandexCloudWriter().createDir("/", dirName())
                showToast("Папка ${dirName()} создана")
            }
            catch (e: CloudWriter.AlreadyExistsException) {
                showError(Exception("Папка уже существует"))
            }
            catch(t: Throwable) {
                showError(t)
            }
            finally {
                hideProgressBar()
            }
        }
    }

    private fun createDeepCloudDir() {
        thread {
            try {
                hideError()
                showProgressBar()
                yandexCloudWriter().createDirWithParents("/", dirName())
                showToast("Папка ${dirName()} создана")
            }
            catch(t: Throwable) {
                showError(t)
            }
            finally {
                hideProgressBar()
            }
        }
    }

    private fun createLocalDir() {
        thread {
            try {
                hideError()
                showProgressBar()
                localCloudWriter().createDirWithParents(localMusicDirPath(), dirName())
                showToast("Папка ${dirName()} создана")
            }
            catch (t: Throwable) {
                showError(t)
            }
            finally {
                hideProgressBar()
            }
        }
    }


    private fun showToast(text: String) {
        binding.root.post { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }


    private fun localCloudWriter(): CloudWriter = LocalCloudWriter("")

    private fun dirName(): String = binding.pathInput.text.toString()

    private fun yandexCloudWriter(): CloudWriter = YandexCloudWriter(OkHttpClient.Builder().build(), Gson(), yandexAuthToken!!)



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

    private fun localMusicDirPath(): String
        = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        const val YANDEX_AUTH_TOKEN = "AUTH_TOKEN"
        const val DIR_NAME = "DIR_NAME"
    }
}