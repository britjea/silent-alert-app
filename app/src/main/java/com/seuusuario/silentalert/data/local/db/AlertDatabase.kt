package com.seuusuario.silentalert.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlertDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}
