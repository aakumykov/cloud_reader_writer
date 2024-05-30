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