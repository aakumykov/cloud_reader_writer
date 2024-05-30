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

        /*enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        /*supportFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainerView, BlankFragment(), null)
            .commit()*/

        /*supportFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainerView2, Fragment1(), null)
            .commit()*/

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView2, Fragment1())
            .commit()
    }
}