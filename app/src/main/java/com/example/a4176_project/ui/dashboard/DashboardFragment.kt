package com.example.a4176_project.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.a4176_project.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private var shaketime = 0
    private lateinit var shakeDetector:ShakeDetector

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProviders.of(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val textView: TextView = root.findViewById(R.id.text_dashboard)
        dashboardViewModel.text.observe(this, Observer {
            textView.text = it
        })
        //If user want to play coke shaker
        val coke = root.findViewById<FloatingActionButton>(R.id.CokeShaker)
        coke.setOnClickListener {
            showDialog()
        }
        return root
    }

    fun showDialog(){
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("Coke shaker")
        val view = layoutInflater.inflate(R.layout.coke_dialog, null)
        val reminder = view.findViewById(R.id.reminder) as TextView
        val picture = view.findViewById(R.id.imageView) as ImageView
        shakeDetector = ShakeDetector(this.context!!)

        builder.setView(view);
        builder.setPositiveButton("Reset") { dialog, p1 ->
            shaketime=0
            picture.setImageResource(R.drawable.coke1)

            shakeDetector.startListening(object:ShakeDetector.ShakeListener {
                override fun onShake(force: Float) {
                    shakecoke(reminder,picture)
                }
            })
        }
        shakeDetector.startListening(object:ShakeDetector.ShakeListener {
            override fun onShake(force: Float) {
                shakecoke(reminder,picture)
            }
        })

        if (!shakeDetector.isSupported()){
            Toast.makeText(
                this.context,
                "Your device does not support the accelerometer function",
                Toast.LENGTH_SHORT
            ).show()
        }

        else{
            shakeDetector.startListening(object:ShakeDetector.ShakeListener {
                override fun onShake(force: Float) {
                    shakecoke(reminder,picture)
                    finished(reminder,picture)
                }
            })
        }
    }

    fun shakecoke(reminder: TextView,picture:ImageView){
        reminder.setText("Shake it as fast as you can!")
        picture.setImageResource(R.drawable.coke2)
        shaketime++
    }

    fun finished(reminder: TextView,picture:ImageView){
        if(shaketime>=5){
            reminder.setText("Booom!")
            picture.setImageResource(R.drawable.coke3)
            var vibrator =
                context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(1000)
            shaketime=0
            shakeDetector.stopListening()
        }
    }
}