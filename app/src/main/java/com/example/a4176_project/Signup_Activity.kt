package com.example.a4176_project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Signup_Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)
        val button = findViewById<Button>(R.id.signup)
        val intent = Intent(this, MainActivity::class.java).apply{}
        button.setOnClickListener {
            startActivity(intent)
        }
    }
}
