package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY id DESC")
    fun getAllShortcuts(): Flow<List<ShortcutContact>>

    @Query("SELECT * FROM shortcuts WHERE shortcutCode = :code LIMIT 1")
    suspend fun getShortcutByCode(code: String): ShortcutContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ShortcutContact)

    @Update
    suspend fun updateShortcut(shortcut: ShortcutContact)

    @Delete
    suspend fun deleteShortcut(shortcut: ShortcutContact)
}
