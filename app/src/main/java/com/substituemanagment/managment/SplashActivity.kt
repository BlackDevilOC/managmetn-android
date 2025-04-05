package com.substituemanagment.managment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Delay for splash screen display (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            // Start MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            
            // Close this activity
            finish()
        }, 1500)
    }
} 