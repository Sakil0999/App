package com.example.parser

enum class BarcodeType {
    URL,
    WIFI,
    PHONE,
    EMAIL,
    SMS,
    CONTACT,
    GEO,
    CALENDAR,
    PRODUCT,
    TEXT
}

data class ParsedBarcode(
    val rawText: String,
    val formatName: String,
    val barcodeFormatInt: Int,
    val type: BarcodeType,
    val title: String,
    val subtitle: String = "",
    val details: Map<String, String> = emptyMap()
)
