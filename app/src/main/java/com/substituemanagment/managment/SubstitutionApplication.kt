package com.substituemanagment.managment

import android.app.Application
import com.substituemanagment.managment.di.ServiceLocator

class SubstitutionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
} 