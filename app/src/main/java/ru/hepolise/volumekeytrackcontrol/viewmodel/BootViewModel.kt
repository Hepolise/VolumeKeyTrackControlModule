package ru.hepolise.volumekeytrackcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.hepolise.volumekeytrackcontrol.repository.BootRepository
import ru.hepolise.volumekeytrackcontrol.util.LSPosedLogger
import ru.hepolise.volumekeytrackcontrol.util.StatusSysPropsHelper

class BootViewModel(
    private val bootRepository: BootRepository
) : ViewModel() {

    private val _isBootCompleted = MutableStateFlow(false)
    val isBootCompleted = _isBootCompleted.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var observationJob: Job? = null

    init {
        LSPosedLogger.log("Init view model")
        observationJob = viewModelScope.launch {
            bootRepository.observeBootCompleted().collect { completed ->
                _isBootCompleted.value = completed
                _isLoading.value = false
                observationJob?.cancel()
            }
        }

        viewModelScope.launch {
            if (!bootRepository.isBootCompleted()) {
                checkBootStatus()
            }
        }
    }


    private suspend fun checkBootStatus() {
        val maxAttempts = 60
        var attempts = 0

        while (attempts < maxAttempts) {
            delay(1_000)
            StatusSysPropsHelper.refreshIsHooked()

            if (StatusSysPropsHelper.isHooked) {
                LSPosedLogger.log("Boot completed - hook detected")
                bootRepository.setBootCompleted()
                return
            }

            attempts++
            LSPosedLogger.log("Attempt $attempts: hook not detected yet")
        }

        LSPosedLogger.log("By timer, boot is not still completed after $maxAttempts attempts")
        _isLoading.value = false
    }

    override fun onCleared() {
        observationJob?.cancel()
        super.onCleared()
    }
}