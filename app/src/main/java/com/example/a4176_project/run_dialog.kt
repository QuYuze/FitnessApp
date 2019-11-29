package com.example.a4176_project

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment

class  run_dialog: AppCompatDialogFragment()
{
    private var distance:Int = 0
    private lateinit var checkBox:CheckBox

    lateinit var customView: View;
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?
    {
        return customView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate your view here
        customView = layoutInflater.inflate(R.layout.run_dialog,null)
        // Create Alert Dialog with your custom view
        return AlertDialog.Builder(context!!)
            .setTitle(R.string.setup)
            .setView(customView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

    }

} 