package com.macan.guestbookkemendagri.helper

import android.app.Application
import android.content.Context

class MyApp: Application() {

    companion object{
        private var instance: MyApp? = null

        fun getContext(): Context {
            return instance!!.applicationContext
        }

    }

    fun getInstance(): MyApp? {
        return instance
    }

    override fun onCreate() {
        instance = this
        super.onCreate()
    }

}