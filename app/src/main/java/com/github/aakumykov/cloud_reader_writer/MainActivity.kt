package com.github.aakumykov.cloud_reader_writer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.aakumykov.cloud_reader_writer.databinding.ActivityMain2Binding
import com.github.aakumykov.cloud_reader_writer.fragments.Fragment1

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerView2, Fragment1())
            .commit()
    }
}