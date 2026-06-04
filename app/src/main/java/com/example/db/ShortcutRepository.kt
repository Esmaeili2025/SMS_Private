package com.example.db

import kotlinx.coroutines.flow.Flow

class ShortcutRepository(private val shortcutDao: ShortcutDao) {
    val allShortcuts: Flow<List<ShortcutContact>> = shortcutDao.getAllShortcuts()

    suspend fun getShortcutByCode(code: String): ShortcutContact? {
        return shortcutDao.getShortcutByCode(code)
    }

    suspend fun insert(shortcut: ShortcutContact) {
        shortcutDao.insertShortcut(shortcut)
    }

    suspend fun update(shortcut: ShortcutContact) {
        shortcutDao.updateShortcut(shortcut)
    }

    suspend fun delete(shortcut: ShortcutContact) {
        shortcutDao.deleteShortcut(shortcut)
    }
}
