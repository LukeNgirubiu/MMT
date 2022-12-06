package com.example.mmt.Utils

import android.app.AlertDialog
import android.content.Context

class Dialogs(val context: Context) {
    fun waitingDialog(title:String,message:String):AlertDialog{
        val alertBuild= AlertDialog.Builder(this.context)
        alertBuild.setTitle(title)
        alertBuild.setMessage(message)
        val alert=alertBuild.create()
        alert.show()
        return alert
    }

}
/*
 *
 *
 * */