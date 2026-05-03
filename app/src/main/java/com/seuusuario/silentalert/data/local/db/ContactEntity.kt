package com.seuusuario.silentalert.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val channels: String  // JSON serializado ex: "SMS,EMAIL"
)
