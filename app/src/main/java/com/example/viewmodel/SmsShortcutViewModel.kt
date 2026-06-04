package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.db.ShortcutContact
import com.example.db.ShortcutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmsShortcutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ShortcutRepository

    init {
        val dao = AppDatabase.getDatabase(application).shortcutDao()
        repository = ShortcutRepository(dao)
    }

    val shortcuts: StateFlow<List<ShortcutContact>> = repository.allShortcuts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addShortcut(name: String, phoneNumber: String, message: String, shortcutCode: String) {
        viewModelScope.launch {
            repository.insert(
                ShortcutContact(
                    name = name,
                    phoneNumber = phoneNumber,
                    message = message,
                    shortcutCode = shortcutCode.trim()
                )
            )
        }
    }

    fun updateShortcut(shortcut: ShortcutContact) {
        viewModelScope.launch {
            repository.update(shortcut.copy(shortcutCode = shortcut.shortcutCode.trim()))
        }
    }

    fun deleteShortcut(shortcut: ShortcutContact) {
        viewModelScope.launch {
            repository.delete(shortcut)
        }
    }
}
