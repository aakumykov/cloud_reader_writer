package com.github.aakumykov.cloud_reader_writer.extentions

import android.annotation.SuppressLint
import android.app.Activity
import androidx.preference.PreferenceManager

@SuppressLint("ApplySharedPref")
fun Activity.storeStringInPreferences(key: String, value: String?) {
    PreferenceManager.getDefaultSharedPreferences(this).edit()
        .putString(key, value)
        .commit()
}

fun Activity.getStringFromPreferences(key: String): String? {
    return PreferenceManager.getDefaultSharedPreferences(this)
        .getString(key, null)
}