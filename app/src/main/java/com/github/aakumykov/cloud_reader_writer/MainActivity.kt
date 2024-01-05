package com.github.aakumykov.cloud_reader_writer

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.github.aakumykov.cloud_reader_writer.databinding.ActivityMainBinding
import com.github.aakumykov.cloud_reader_writer.extentions.getStringFromPreferences
import com.github.aakumykov.cloud_reader_writer.extentions.storeStringInPreferences
import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.cloud_writer.LocalCloudWriter
import com.github.aakumykov.cloud_writer.YandexCloudWriter
import com.github.aakumykov.file_lister_navigator_selector.file_selector.FileSelector
import com.github.aakumykov.file_lister_navigator_selector.fs_item.FSItem
import com.github.aakumykov.file_lister_navigator_selector.local_file_selector.LocalFileSelector
import com.github.aakumykov.kotlin_playground.cloud_authenticator.CloudAuthenticator
import com.github.aakumykov.kotlin_playground.cloud_authenticator.YandexAuthenticator
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import com.google.gson.Gson
import com.yandex.authsdk.internal.strategy.LoginType
import okhttp3.OkHttpClient
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructPermissionsRequest
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), CloudAuthenticator.Callbacks, FileSelector.Callback {

    private lateinit var binding: ActivityMainBinding
    private var yandexAuthToken: String? = null
    private lateinit var yandexAuthenticator: CloudAuthenticator
    private lateinit var permissionsRequester: PermissionsRequester
    private val fileSelector: FileSelector by lazy {
        LocalFileSelector.create(this).show(supportFragmentManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getStringFromPreferences(DIR_NAME)?.let { binding.dirNameInput.setText(it) }

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

        binding.dirNameInput.addTextChangedListener { saveDirNameInput() }
        
        prepareButtons()
    }

    private fun prepareButtons() {
        binding.createCloudDirButton.setOnClickListener { createCloudDir() }
        binding.createLocalDirButton1.setOnClickListener { permissionsRequester.launch() }
        binding.checkCloudDirExistsButton.setOnClickListener { checkDirExists(true) }
        binding.checkLocalDirExistsButton.setOnClickListener { checkDirExists(false) }
        binding.selectFileButton.setOnClickListener {
            fileSelector.setCallback(this)
        }
    }

    private fun checkDirExists(isCloud: Boolean) {
        val cloudWriter = if (isCloud) yandexCloudWriter() else localCloudWriter()
        val parentDirName: String = if (isCloud) "/" else localMusicDirPath()

        thread {
            try {
                resetView()
                val exists = cloudWriter.dirExists(parentDirName, dirName())
                showInfo(
                    when (exists) {
                        true -> "Папка существует"
                        false -> "Такой папки нет"
                    }
                )
            } catch (t: Throwable) {
                showError(t)
            } finally {
                hideProgressBar()
            }
        }
    }

    private fun resetView() {
        hideError()
        hideInfo()
        showProgressBar()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveDirNameInput()
    }

    private fun saveDirNameInput() {
        storeStringInPreferences(DIR_NAME, dirName())
    }

    private fun createCloudDir() {
        thread {
            try {
                resetView()
                yandexCloudWriter().createDir("/", dirName())
                showInfo("Папка ${dirName()} создана")
            }
            catch(e: CloudWriter.AlreadyExistsException) {
                showError("Папка уже существует")
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
                resetView()
                localCloudWriter().createDir(localMusicDirPath(), dirName())
                showInfo("Папка ${dirName()} создана")
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

    private fun dirName(): String = binding.dirNameInput.text.toString()

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
        val errorMsg = ExceptionUtils.getErrorMessage(throwable)
        Log.e(TAG, errorMsg, throwable)
        showError(errorMsg)
    }

    private fun showError(errorMsg: String) {
        binding.root.post {
            with(binding.errorView) {
                text = errorMsg
                visibility = View.VISIBLE
            }
        }
    }

    private fun hideError() {
        binding.root.post {
            with(binding.errorView) {
                text = ""
                visibility = View.GONE
            }
        }
    }

    private fun showInfo(text: String) {
        binding.root.post {
            binding.infoView.apply {
                visibility = View.VISIBLE
                setText(text)
            }
        }
    }

    fun hideInfo() {
        binding.root.post {
            with(binding.infoView) {
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

    override fun onFilesSelected(selectedItemsList: List<FSItem>) {
        fileSelector.unsetCallback()

    }
}