package com.example.ocr.UI.SplashScreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.ocr.MainActivity
import com.example.ocr.R
import com.example.ocr.UI.OnBoarding.OnBoardingScreensActivity

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        Handler().postDelayed({
            val intent = when (onBoardingFinished()) {
                true -> Intent(this@SplashScreenActivity, MainActivity::class.java)
                else -> Intent(this@SplashScreenActivity, OnBoardingScreensActivity::class.java)
            }
            finish()
            startActivity(intent)
        }, 3000)
    }

    private fun onBoardingFinished(): Boolean {
        val sharedPreferences = getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("Finished", false)
    }
}