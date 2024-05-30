package com.github.aakumykov.cloud_reader_writer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.aakumykov.cloud_reader_writer.fragments.Fragment1

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main2)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerView2, Fragment1())
            .commit()
    }
}