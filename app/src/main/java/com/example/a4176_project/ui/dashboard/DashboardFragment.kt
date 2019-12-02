package com.example.a4176_project.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.example.a4176_project.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private var shaketime :Float = 0F
    private lateinit var shakeDetector:ShakeDetector
    private val QRcodeWidth = 500

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
        val coke = root.findViewById<Button>(R.id.CokeShaker)
        coke.setOnClickListener {
            showDialog()
        }

        val QRcode:Button = root.findViewById<Button>(R.id.QRcode)
        QRcode.setOnClickListener(){
            showDialog2()
        }
        return root
    }

    fun showDialog(){
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("Coke shaker")
        val view = layoutInflater.inflate(R.layout.coke_dialog, null)
        val reminder = view.findViewById(R.id.reminder) as TextView
        val picture = view.findViewById(R.id.imageView) as ImageView
        picture.setImageResource(R.drawable.coke1)
        shakeDetector = ShakeDetector(this.context!!)

        builder.setView(view);
        builder.setPositiveButton("Reset") { dialog, p1 ->
            shaketime=0F
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
        builder.show()
    }
    fun showDialog2(){
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("QRcode")
        val view = layoutInflater.inflate(R.layout.qrcode_dialog, null)
        val picture = view.findViewById(R.id.QRcode) as ImageView
        val qrcode = TextToImageEncode("YahuWang")

        builder.setView(view)
            Glide
                .with(this)
                .load(qrcode)
                .centerCrop()
                .into(picture)

        builder.setNeutralButton("Share"){ dialog, p1 ->
            val file = File("${context!!.cacheDir}/drawing.png")
            qrcode!!.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
            val contentUri = FileProvider.getUriForFile(context!!, context!!.packageName + ".provider", file)

            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM,contentUri)
                type = "image/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(shareIntent, "Share to..."))

        }

        builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
            dialog.cancel()
        }
        builder.show()
    }

    fun shakecoke(reminder: TextView,picture:ImageView){
        shaketime++
        reminder.setText("Shake it as fast as you can! \n"+shaketime/1000*100.toFloat()+"% complete!")
        picture.setImageResource(R.drawable.coke2)
    }

    fun finished(reminder: TextView,picture:ImageView){
        if(shaketime>=990){
            reminder.setText("Booom!")
            picture.setImageResource(R.drawable.coke3)
            var vibrator =
                context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(1000)
            shaketime=0F
            shakeDetector.stopListening()
        }
    }

    //Generate QR code
    private fun TextToImageEncode(Value: String): Bitmap? {
        val bitMatrix: BitMatrix
        try {
            bitMatrix = MultiFormatWriter().encode(
                Value,
                BarcodeFormat.QR_CODE,
                QRcodeWidth, QRcodeWidth, null
            )
        } catch (Illegalargumentexception: IllegalArgumentException) {
            return null
        }
        val MatrixWidth = bitMatrix.getWidth()
        val MatrixHeight = bitMatrix.getHeight()
        val pixels = IntArray(MatrixWidth * MatrixHeight)

        for (y in 0 until MatrixHeight) {
            val offset = y * MatrixWidth
            for (x in 0 until MatrixWidth) {
                pixels[offset + x] =
                    if (bitMatrix.get(x, y))
                    resources.getColor(R.color.black)
               else
                    resources.getColor(R.color.white)
            }
        }
        val bitmap = Bitmap.createBitmap(MatrixWidth, MatrixHeight, Bitmap.Config.ARGB_4444)
        bitmap.setPixels(pixels, 0, 500, 0, 0, MatrixWidth, MatrixHeight)
        return bitmap
    }

}