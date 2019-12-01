package com.example.a4176_project.ui.notifications

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.a4176_project.R
import java.io.File

class NotificationsFragment : Fragment() {

    val nameStorage = "nameFile"
    val descriptionStorage = "descriptionFile"
    var name: String = ""
    var description: String = ""
    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var imageView: ImageView
    lateinit var username: TextView
    lateinit var descriptionView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_notifications, container, false)
        val file = File(context?.filesDir, nameStorage)
         imageView = root.findViewById(R.id.avator)
        imageView.setOnClickListener {
            dispatchTakePictureIntent()
        }
        username= root.findViewById(R.id.profileNameView)
        descriptionView = root.findViewById(R.id.descriptionTextView)

        val button: Button = root.findViewById(R.id.editProfileButton)

        button.setOnClickListener {
            showDialog()
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
        builder.show();

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
        }
    }

}