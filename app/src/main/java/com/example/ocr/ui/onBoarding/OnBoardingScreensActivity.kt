package com.example.ocr.ui.onBoarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ocr.ui.utils.UIUtils
import com.example.ocr.databinding.ActivityOnBoardingScreensBinding

class OnBoardingScreensActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnBoardingScreensBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //initialize binding
        binding = ActivityOnBoardingScreensBinding.inflate(layoutInflater)
        //set status bar color to transparent
        setContentView(binding.root)
        UIUtils.setStatusBarTransparent(this, binding.root)
    }

}