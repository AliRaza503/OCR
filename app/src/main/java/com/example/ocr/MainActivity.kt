package com.example.ocr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ocr.ui.utils.UIUtils
import com.example.ocr.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Set status bar color transparent
        UIUtils.setStatusBarTransparent(this, binding.root)
    }
}

