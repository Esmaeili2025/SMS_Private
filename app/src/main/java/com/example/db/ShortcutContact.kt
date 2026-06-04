package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcuts")
data class ShortcutContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val message: String,
    // The shortcut code dialed in dialer (e.g., "110", "5", "*1#")
    val shortcutCode: String
)
