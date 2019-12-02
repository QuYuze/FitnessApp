package com.example.a4176_project.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.a4176_project.Login_Activity
import com.example.a4176_project.R
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class ProfileFragment : Fragment() {

    val nameStorage = "nameFile"
    val descriptionStorage = "descriptionFile"
    var name: String = ""
    var description: String = ""
    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var imageView: ImageView
    lateinit var username: TextView
    lateinit var descriptionView: TextView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)
        val file = File(context?.filesDir, nameStorage)
         imageView = root.findViewById(R.id.avator)

        //Read saved image and other user info

        sharedPreferences = activity!!.getSharedPreferences("profile", Context.MODE_PRIVATE)
        val url = sharedPreferences.getString("img-URI",null)
        val uname = sharedPreferences.getString("Name",null)
        val udesc = sharedPreferences.getString("Description",null)
        if(url != null)
        {
            Glide
                .with(this)
                .load(url)
                .centerCrop()
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)
        }
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE)

        imageView.setOnClickListener {
            dispatchTakePictureIntent()
        }
        username= root.findViewById(R.id.profileNameView)
        descriptionView = root.findViewById(R.id.descriptionTextView)

        val button: Button = root.findViewById(R.id.editProfileButton)

        button.setOnClickListener {
            showDialog()
        }
        if(uname!=null&&udesc!=null)
        {
            username.text = uname
            descriptionView.text = udesc
        }
        val text_login:TextView = root.findViewById(R.id.login)
        val intent = Intent(context!!, Login_Activity::class.java).apply{

        }
        text_login.setOnClickListener {
            startActivity(intent)
        }
        return root
    }

    fun showDialog() {
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("Set Profile")
        val view = layoutInflater.inflate(R.layout.run_second_diaglog, null)
        val editName: EditText = view.findViewById(R.id.editName)
        val editDescription: EditText = view.findViewById(R.id.editDescription)

        builder.setView(view)

        builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
            name = editName.text.toString()
            description = editDescription.text.toString()
            with (sharedPreferences.edit()){
                putString("Name",name)
                putString("Description",description)
                apply()
            }
            context?.openFileOutput(nameStorage, Context.MODE_PRIVATE).use {
                it?.write(name.toByteArray())
            }

            context?.openFileOutput(descriptionStorage, Context.MODE_PRIVATE).use {
                it?.write(description.toByteArray())
            }

            username.text =
                context?.openFileInput(nameStorage)?.bufferedReader()?.useLines { lines ->

                    lines.fold("") { some, text ->
                        "$some\n$text"
                    }
                }

            descriptionView.text =
                context?.openFileInput(descriptionStorage)?.bufferedReader()?.useLines { lines ->

                    lines.fold("") { some, text ->
                        "$some\n$text"
                    }
                }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, p1 ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(activity!!.packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data!!.extras!!.get("data") as Bitmap
            //var img = Bitmap.createScaledBitmap(imageBitmap,150,150,false)
            Glide
                .with(this)
                .load(imageBitmap)
                .centerCrop()
                .apply(RequestOptions.circleCropTransform())//found this usage here https://stackoverflow.com/questions/25278821/how-to-round-an-image-with-glide-library
                .into(imageView)
            // Get the context wrapper instance
            val wrapper = ContextWrapper(context)
            // Initializing a new file
            // The bellow line return a directory in internal storage
            var file = wrapper.getDir("images", Context.MODE_PRIVATE)
            //save the image. Reference
            //https://android--code.blogspot.com/2018/04/android-kotlin-save-image-to-internal.html
            file = File(file, "${UUID.randomUUID()}.jpg")
            try {
                val stream: OutputStream = FileOutputStream(file)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()
            } catch (e: IOException){ // Catch the exception
                e.printStackTrace()
            }
            with (sharedPreferences.edit()){
               putString("img-URI",file.absolutePath)
                apply()
            }
        }
    }
}
