package com.dragon.fm.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import android.os.SystemClock

class SleepTimerManager(private val onTimerExpired: () -> Unit) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow<Int?>(null)
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()

    private val _selectedMinutes = MutableStateFlow<Int?>(null)
    val selectedMinutes: StateFlow<Int?> = _selectedMinutes.asStateFlow()

    fun startTimer(minutes: Int) {
        cancelTimer()
        _selectedMinutes.value = minutes
        val deadlineMs = SleepTimerClock.deadlineAfterMinutes(minutes, SystemClock.elapsedRealtime())
        _remainingSeconds.value = SleepTimerClock.remainingSeconds(deadlineMs, SystemClock.elapsedRealtime())

        timerJob = scope.launch {
            while (isActive) {
                val nowMs = SystemClock.elapsedRealtime()
                val remainingSec = SleepTimerClock.remainingSeconds(deadlineMs, nowMs)
                if (remainingSec <= 0) break
                _remainingSeconds.value = remainingSec
                delay((deadlineMs - nowMs).coerceAtMost(1000L))
            }
            _remainingSeconds.value = null
            _selectedMinutes.value = null
            onTimerExpired()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = null
        _selectedMinutes.value = null
    }

    fun isRunning(): Boolean = timerJob != null && timerJob?.isActive == true
}
