package com.github.aakumykov.cloud_reader_writer

import android.os.Build
import androidx.fragment.app.FragmentActivity
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructPermissionsRequest

class StoragePermissionRequester (
    private val activity: FragmentActivity,
    private val callbacks: Callbacks
) {
    fun startRequestingPermissions() = storagePermissionsRequester.launch()


    private val storagePermissionsRequester: PermissionsRequester by lazy {
        activity.constructPermissionsRequest(
            *permissions(),
            requiresPermission = { callbacks.onStoragePermissionGranted() },
            onPermissionDenied = { callbacks.onStoragePermissionDenied() },
            onNeverAskAgain = { callbacks.onStoragePermissionNeverAskAgain() }
        )
    }

    private fun permissions(): Array<out String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_AUDIO,
                android.Manifest.permission.READ_MEDIA_VIDEO,)
        else
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
    }


    interface Callbacks {
        fun onStoragePermissionGranted()
        fun onStoragePermissionDenied()
        fun onStoragePermissionNeverAskAgain()
    }
}