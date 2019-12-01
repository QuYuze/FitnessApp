package com.example.a4176_project.ui.notifications

import android.content.ClipDescription
import android.content.Context
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.a4176_project.R
import kotlinx.android.synthetic.main.login.*
import java.io.File

class NotificationsFragment : Fragment() {

    val nameStorage = "nameFile"
    val descriptionStorage = "descriptionFile"
    var name: String = ""
    var description: String = ""

    lateinit var username: TextView
    lateinit var descriptionView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_notifications, container, false)
        val imageView: ImageView = root.findViewById(R.id.avator)
        val file = File(context?.filesDir, nameStorage)

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

}