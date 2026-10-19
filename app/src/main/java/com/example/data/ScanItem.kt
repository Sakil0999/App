package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,
    val format: String, // "QR Code", "EAN-13", "UPC-A", "Code 128", etc.
    val barcodeFormatInt: Int,
    val type: String, // "URL", "WIFI", "PHONE", "EMAIL", "SMS", "CONTACT", "GEO", "PRODUCT", "TEXT"
    val title: String,
    val detailsJson: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
