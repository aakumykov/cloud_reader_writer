package com.github.aakumykov.cloud_reader_writer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.aakumykov.cloud_reader_writer.databinding.ActivityMainBinding
import com.github.aakumykov.cloud_reader_writer.fragments.ReadingAndDirCreationFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerView, ReadingAndDirCreationFragment.create())
            .commit()
    }
}