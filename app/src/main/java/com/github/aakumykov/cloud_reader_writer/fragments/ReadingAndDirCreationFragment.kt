package com.github.aakumykov.cloud_reader_writer.fragments

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.aakumykov.cloud_reader.CloudReader
import com.github.aakumykov.cloud_reader.local_cloud_reader.LocalCloudReader
import com.github.aakumykov.cloud_reader.yandex_cloud_reader.YandexCloudReader
import com.github.aakumykov.cloud_reader_writer.MainActivity
import com.github.aakumykov.cloud_reader_writer.R
import com.github.aakumykov.cloud_reader_writer.cloud_authenticator.CloudAuthenticator
import com.github.aakumykov.cloud_reader_writer.cloud_authenticator.YandexAuthenticator
import com.github.aakumykov.cloud_reader_writer.databinding.FragmentReadingAndDirCreatonBinding
import com.github.aakumykov.cloud_reader_writer.extentions.getStringFromPreferences
import com.github.aakumykov.cloud_reader_writer.extentions.storeStringInPreferences
import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.cloud_writer.LocalCloudWriter
import com.github.aakumykov.cloud_writer.YandexCloudWriter
import com.github.aakumykov.storage_access_helper.StorageAccessHelper
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import com.google.gson.Gson
import com.yandex.authsdk.internal.strategy.LoginType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructPermissionsRequest
import java.io.InputStream
import kotlin.concurrent.thread

class ReadingAndDirCreationFragment :
    Fragment(R.layout.fragment_reading_and_dir_creaton),
    CloudAuthenticator.Callbacks
{
    private var _binding: FragmentReadingAndDirCreatonBinding? = null
    private val binding get() = _binding!!

    private var yandexAuthToken: String? = null
    private lateinit var yandexAuthenticator: CloudAuthenticator
    private lateinit var permissionsRequester: PermissionsRequester
    private lateinit var storageAccessHelper: StorageAccessHelper

    private val fullSourceFileName: String get() {
        return if (sourceIsLocal) filePathInLocalDownloads(inputFileName)
        else inputFileName
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentReadingAndDirCreatonBinding.bind(view)

        storageAccessHelper = StorageAccessHelper.create(this).apply {
            prepareForReadAccess()
            prepareForWriteAccess()
            prepareForFullAccess()
        }

        restoreInputFields()

        permissionsRequester = constructPermissionsRequest(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            requiresPermission = ::createLocalDirReal
        )

        yandexAuthenticator = YandexAuthenticator(requireActivity(), LoginType.NATIVE, this)
        yandexAuthToken = getStringFromPreferences(YANDEX_AUTH_TOKEN)
        displayYandexAuthStatus()

        prepareButtons()
        prepareInputFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        storeInputFields()

        _binding = null
    }


    private fun prepareInputFields() {
        binding.fileNameInput.addTextChangedListener { storeStringInPreferences(FILE_NAME, inputFileName) }
        binding.dirNameInput.addTextChangedListener { storeStringInPreferences(DIR_NAME, inputDirName) }
    }

    private fun storeInputFields() {

        storeStringInPreferences(FILE_NAME, inputFileName)
        storeStringInPreferences(DIR_NAME, inputDirName)

        storeStringInPreferences(SOURCE_STORAGE_TYPE, when(binding.sourceTypeToggleButton.checkedButtonId) {
            R.id.sourceTypeLocal -> StorageType.LOCAL.name
            R.id.sourceTypeYandexDisk -> StorageType.YANDEX_DISK.name
            else -> ""
        })

        storeStringInPreferences(TARGET_STORAGE_TYPE, when(binding.targetTypeToggleButton.checkedButtonId) {
            R.id.targetTypeLocal -> StorageType.LOCAL.name
            R.id.targetTypeYandexDisk -> StorageType.YANDEX_DISK.name
            else -> ""
        })
    }

    private fun restoreInputFields() {

        getStringFromPreferences(FILE_NAME)?.let { binding.fileNameInput.setText(it) }
        getStringFromPreferences(DIR_NAME)?.let { binding.dirNameInput.setText(it) }

        when(getStringFromPreferences(SOURCE_STORAGE_TYPE)) {
            StorageType.LOCAL.toString() -> binding.sourceTypeLocal.isSelected = true
            StorageType.YANDEX_DISK.toString() -> binding.sourceTypeYandexDisk.isSelected = true
            else -> {}
        }

        when(getStringFromPreferences(TARGET_STORAGE_TYPE)) {
            StorageType.LOCAL.toString() -> binding.targetTypeLocal.isSelected = true
            StorageType.YANDEX_DISK.toString() -> binding.targetTypeYandexDisk.isSelected = true
            else -> {}
        }
    }

    private fun prepareButtons() {

        /*binding.requestReadAccess.setOnClickListener {
            storageAccessHelper.requestReadAccess { isGranted ->
                showToast("Доступ на чтение: ${if(isGranted) "получен" else "не получен"}")
            }
        }

        binding.requestWriteAccess.setOnClickListener {
            storageAccessHelper.requestReadAccess { isGranted ->
                showToast("Доступ на запись: ${if(isGranted) "получен" else "не получен"}")
            }
        }

        binding.requestFullAccess.setOnClickListener {
            storageAccessHelper.requestReadAccess { isGranted ->
                showToast("Полный доступ: ${if(isGranted) "получен" else "не получен"}")
            }
        }*/


        binding.yandexAuthButton.setOnClickListener {
            if (null == yandexAuthToken)
                yandexAuthenticator.startAuth()
            else {
                yandexAuthToken = null
                storeStringInPreferences(YANDEX_AUTH_TOKEN, null)
                displayYandexAuthStatus()
            }
        }

        binding.checkFileExistsButton.setOnClickListener { checkFileExists() }
        binding.getDownloadLinkButton.setOnClickListener { onGetDownloadLinkClicked() }
        binding.writeToFileButton.setOnClickListener { onWriteToFileButtonClicked() }

        binding.getInputStreamButton.setOnClickListener {
            if (sourceCloudReader is LocalCloudReader)
                storageAccessHelper.requestReadAccess { getInputStreamOfFile() }
            else
                getInputStreamOfFile()
        }

        binding.createDirButton.setOnClickListener { createDir() }
        binding.checkDirExistsButton.setOnClickListener { checkDirExists() }

        //        binding.selectFileButton.setOnClickListener { pickFile() }
//        binding.uploadFileButton.setOnClickListener { uploadFile() }
//        binding.checkUploadedFileButton.setOnClickListener { checkUploadedFile() }
//        binding.deleteDirButton.setOnClickListener { deleteDirectory() }
    }

    private fun onWriteToFileButtonClicked() {

        if (targetIsLocal) {
            storageAccessHelper.requestFullAccess { isGranted ->
                if (isGranted)
                    writeStreamToFile()
                else
                    showToast("Нет досутпа к хранилищу")
            }
        }
        else {
            writeStreamToFile()
        }
    }

    private fun writeStreamToFile() {

        val sourceFilePath = if (sourceIsLocal) filePathInLocalDownloads(inputFileName) else inputFileName

        val targetFilePath = if (targetIsLocal) filePathInLocalDownloads("target_$inputFileName") else "target_$inputFileName"

        showInfo("$sourceFilePath --> $targetFilePath")

        showProgressBar()

        lifecycleScope.launch (Dispatchers.IO) {
            try {
                sourceCloudReader.getFileInputStream(sourceFilePath).getOrThrow().use { inputStream ->

                    val countingInputStream = CountingInputStream(inputStream, bufferSize = 1024 * 1024) { bytesRead ->
                        Log.d(TAG, "Прочитано байт: $bytesRead")
                    }

                    cloudWriter.putFile(
                        countingInputStream,
                        targetFilePath,
                        true
                    )
                }
            }
            catch (e: Exception) {
                showError(e)
            }
            finally {
                hideProgressBar()
            }
        }
    }


    private fun checkFileExists() {

        beginAndBusy()

        val fullFileName = if (sourceIsLocal) filePathInLocalDownloads(inputFileName) else inputFileName

        lifecycleScope.launch(Dispatchers.IO) {
            sourceCloudReader.fileExists(fullFileName).also { result ->

                withContext(Dispatchers.Main) {
                    hideProgressBar()

                    if (result.isSuccess) {
                        result.getOrNull()?.also { isExists ->
                            val isExistsWord = if (isExists) "существует" else "Не существует"
                            showInfo("Файл ${inputFileName} $isExistsWord")
                        } ?: showError("Результат null :-(")
                    } else {
                        showError(result.exceptionOrNull())
                    }
                }
            }
        }
    }

    private fun onGetDownloadLinkClicked() {

        if (!sourceIsLocal && null == yandexAuthToken) {
            showToast("Авторизуйтесь в Яндекс")
            return
        }

        beginAndBusy()

        lifecycleScope.launch(Dispatchers.IO) {

            sourceCloudReader.getDownloadLink(fullSourceFileName).also { result ->

                withContext(Dispatchers.Main) {

                    hideProgressBar()

                    if (result.isSuccess) {
                        result.getOrNull()?.also { url ->
                            showInfo(url)
                        } ?: showError("Результат null :-(")
                    } else {
                        showError(result.exceptionOrNull())
                    }
                }
            }
        }
    }

    private fun getInputStreamOfFile() {
        if (sourceIsLocal) {
            storageAccessHelper.requestReadAccess { getInputStreamOfFileReal() }
        } else {
            getInputStreamOfFileReal()
        }
    }

    private fun getInputStreamOfFileReal() {

        beginAndBusy()

        lifecycleScope.launch(Dispatchers.IO) {
            sourceCloudReader.getFileInputStream(fullSourceFileName).also { result ->
                withContext(Dispatchers.Main) { hideProgressBar() }

                try {
                    result.getOrThrow().use { inputStream: InputStream ->
                        val firstByte = inputStream.read().toByte()
                        withContext(Dispatchers.Main) { showInfo("Первый байт во входном потоке: $firstByte") }
                    }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) { showError(t) }
                }
            }
        }
    }


    private fun beginAndBusy() {
        hideInfo()
        hideError()
        showProgressBar()
    }


    /*private fun deleteDirectory() {

        val basePath = "/"
        val fileName = "dir1"

        lifecycleScope.launch {

            hideInfo()
            hideError()
            showProgressBar()

            try {
                withContext(Dispatchers.IO) {
                    cloudWriter().deleteFile(basePath, fileName)
                }
                showInfo("Папка '$fileName' удалена.")
            }
            catch (t: Throwable) {
                showError(t)
                Log.e(TAG, ExceptionUtils.getErrorMessage(t), t);
            }
            finally {
                hideProgressBar()
            }
        }
    }*/

    private fun createDir() {
        if (sourceIsLocal)
            createLocalDir()
        else
            createCloudDir()
    }

    private fun checkDirExists() {

        beginAndBusy()

        val fullDirPath = if (sourceIsLocal) dirPathInLocalDownloads(inputDirName) else inputDirName

        lifecycleScope.launch(Dispatchers.IO){
            sourceCloudReader.fileExists(fullDirPath)
                .onSuccess { isExists ->
                    if (isExists) showInfo("${inputDirName} существует")
                    else showInfo("${inputDirName} НЕ существует") }
                .onFailure {
                    showError(it)
                }
                .also {
                    hideProgressBar()
                }
        }
    }

    /*private fun checkUploadedFile() {

        if (null == selectedFile) {
            showError("Выберите файл")
            return
        }

        thread {
            resetView()
            showProgressBar()
            try {
                val exists = cloudWriter().fileExists(targetDir(), selectedFile!!.name)
                val isExistsWord = if (exists) "существует" else "не существует"
                showInfo("Файл '${selectedFile!!.name}' $isExistsWord")
            }
            catch (t: Throwable) {
                showError(t)
            }
            finally {
                hideProgressBar()
            }
        }
    }*/

    private fun targetDir(): String =
        if (sourceIsLocal) localMusicDirPath() else CANONICAL_ROOT_PATH


    /*private fun pickFile() {
        with(fileSelector) {
            setCallback(this@MainActivity)
            show(supportFragmentManager)
        }
    }*/


    /*private fun uploadFile() {
        thread {
            resetView()
            showProgressBar()
            try {
                selectedFile?.also {
                    cloudWriter().putFile(
                        File(it.absolutePath),
                        "/",
                        isOverwrite()
                    )
                    showInfo("Файл загружен")
                }
            }
            catch (e: CloudWriter.AlreadyExistsException) {
                showError("Файл ужо существует")
            }
            catch (t: Throwable) {
                showError(t)
            }
            finally {
                hideProgressBar()
            }
        }
    }*/

//    private fun isOverwrite(): Boolean = binding.overwriteSwitch.isChecked

//    private fun cloudWriter(): CloudWriter = if (isLocalChecked) localCloudWriter() else yandexCloudWriter()

    private val sourceCloudReader get(): CloudReader = if (sourceIsLocal) localCloudReader else yandexCloudReader

    private val cloudWriter get(): CloudWriter = if (targetIsLocal) localCloudWriter else yandexCloudWriter

    private val sourceIsLocal get(): Boolean = binding.sourceTypeToggleButton.checkedButtonId == R.id.sourceTypeLocal

    private val targetIsLocal get(): Boolean = binding.targetTypeToggleButton.checkedButtonId == R.id.targetTypeLocal


    /*private fun checkDirExists(isLocal: Boolean) {

        val parentDirName: String = if (isLocal) localMusicDirPath() else "/"

        thread {
            try {
                resetView()
                showProgressBar()
                val exists = cloudWriter().fileExists(parentDirName, path)
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
    }*/

    private fun resetView() {
        hideError()
        hideInfo()
    }

    private fun createCloudDir() {
        thread {
            try {
                resetView()
                showProgressBar()
                yandexCloudWriter.createDir("/", inputDirName)
                showInfo("Папка ${inputDirName} создана")
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
        permissionsRequester.launch()
    }

    private fun createLocalDirReal() {
        thread {
            try {
                resetView()
                showProgressBar()
                localCloudWriter.createDir(localDownloadsDirPath(), inputDirName)
                showInfo("Папка \"${inputDirName}\" создана")
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
        binding.root.post { Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show() }
    }


    private val localCloudWriter get(): CloudWriter = LocalCloudWriter("")

    private val localCloudReader get(): CloudReader = LocalCloudReader()

    private val yandexCloudReader
        get(): CloudReader = YandexCloudReader(
            yandexAuthToken!!,
            okHttpClient,
            gson
        )

    private val inputFileName
        get(): String = binding.fileNameInput.text.toString()

    private val inputDirName
        get(): String = binding.dirNameInput.text.toString()

    private fun filePathInLocalDownloads(name: String): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + name
    }

    private fun dirPathInLocalDownloads(name: String): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + name
    }

    private val yandexCloudWriter get(): CloudWriter = YandexCloudWriter(okHttpClient, gson, yandexAuthToken!!)

    private val okHttpClient get() = OkHttpClient.Builder().build()

    private val gson get() = Gson()


    override fun onCloudAuthSuccess(authToken: String) {
        yandexAuthToken = authToken
        storeStringInPreferences(YANDEX_AUTH_TOKEN, authToken)
        displayYandexAuthStatus()
    }

    private fun displayYandexAuthStatus() {
        if (null == yandexAuthToken) {
            with(binding.yandexAuthButton){
                setText(R.string.login_to_yandex)
                setIconResource(R.drawable.ic_logged_out)
            }
        } else {
            with(binding.yandexAuthButton){
                setText(R.string.logout_from_yandex)
                setIconResource(R.drawable.ic_logged_in)
            }
        }
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

    private fun showError(throwable: Throwable?) {
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

    /*override fun onFilesSelected(selectedItemsList: List<FSItem>) {
        fileSelector.unsetCallback()
        selectedFile = selectedItemsList[0]
        showInfo("Выбран файл '${selectedFile?.name}'")
    }*/

    private fun localDownloadsDirPath(): String =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath


    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        const val SOURCE_STORAGE_TYPE = "SOURCE_STORAGE_TYPE"
        const val TARGET_STORAGE_TYPE = "TARGET_STORAGE_TYPE"
        const val YANDEX_AUTH_TOKEN = "AUTH_TOKEN"
        const val FILE_NAME = "FILE_NAME"
        const val DIR_NAME = "DIR_NAME"
        const val CANONICAL_ROOT_PATH = "/"

        fun create(): Fragment = ReadingAndDirCreationFragment()
    }
}