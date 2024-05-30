package com.github.aakumykov.cloud_reader_writer.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.aakumykov.cloud_reader_writer.R
import com.github.aakumykov.cloud_reader_writer.databinding.Fragment1Binding

class Fragment1 : Fragment(R.layout.fragment1) {

    private var _binding: Fragment1Binding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = Fragment1Binding.bind(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun create(): Fragment = Fragment1()
    }
}
