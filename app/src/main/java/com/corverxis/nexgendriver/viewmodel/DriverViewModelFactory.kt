package com.corverxis.nexgendriver.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DriverViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val prefs = context.getSharedPreferences("nexgen_prefs", Context.MODE_PRIVATE)
        @Suppress("UNCHECKED_CAST")
        return DriverViewModel(context.applicationContext, prefs) as T
    }
}
