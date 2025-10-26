package ru.hepolise.volumekeytrackcontrol.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.hepolise.volumekeytrackcontrol.repository.BootRepository

class BootViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BootViewModel::class.java)) {
            val repository = BootRepository.getBootRepository(context)
            return BootViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}