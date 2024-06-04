package com.github.aakumykov.cloud_reader_writer.extentions

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

@SuppressLint("ApplySharedPref")
fun Fragment.storeStringInPreferences(key: String, value: String?) {
    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
        .putString(key, value)
        .commit()
}

fun Fragment.getStringFromPreferences(key: String): String? {
    return PreferenceManager.getDefaultSharedPreferences(requireContext())
        .getString(key, null)
}


@SuppressLint("ApplySharedPref")
fun Fragment.storeBooleanInPreferences(key: String, value:Boolean) {
    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
        .putBoolean(key, value)
        .commit()
}

fun Fragment.getBooleanFromPreferences(key: String, default: Boolean): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(requireContext())
        .getBoolean(key, default)
}