package com.galibu.core

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MyApplication", "Firebase inicializado desde MyApplication")
        } catch (e: Exception) {
            Log.e("MyApplication", "Error inicializando Firebase en MyApplication", e)
        }
    }
}

