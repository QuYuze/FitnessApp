package com.example.a4176_project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Login_Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        val text_signup = findViewById<TextView>(R.id.signup)
        val intent = Intent(this, Signup_Activity::class.java).apply{}
        text_signup.setOnClickListener {
            startActivity(intent)
        }
        val login = findViewById<Button>(R.id.Login)
        val intent_home = Intent(this, MainActivity::class.java).apply{}
        login.setOnClickListener {
            startActivity(intent_home)
        }
    }
}
