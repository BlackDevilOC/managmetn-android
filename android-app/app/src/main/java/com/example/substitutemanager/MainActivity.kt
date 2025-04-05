package com.example.substitutemanager

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<MaterialButton>(R.id.startButton)
        startButton.setOnClickListener {
            Toast.makeText(
                this,
                "Starting Substitute Teacher Algorithm Test...",
                Toast.LENGTH_LONG
            ).show()
            
            // TODO: Implement the actual algorithm test here
            // This is where we'll add the code to test the substitute teacher algorithm
        }
    }
} 