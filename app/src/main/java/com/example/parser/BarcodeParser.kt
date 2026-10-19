package com.example.parser

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode

object BarcodeParser {

    fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_PDF417 -> "PDF417"
            else -> "Barcode"
        }
    }

    fun parse(barcode: Barcode): ParsedBarcode {
        val raw = barcode.rawValue ?: barcode.displayValue ?: ""
        val formatName = getFormatName(barcode.format)
        val valueType = barcode.valueType

        when (valueType) {
            Barcode.TYPE_URL -> {
                val urlObj = barcode.url
                val url = urlObj?.url ?: raw
                val host = try {
                    Uri.parse(url).host ?: url
                } catch (e: Exception) {
                    url
                }
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.URL,
                    title = host,
                    subtitle = url,
                    details = mapOf("URL" to url, "Domain" to host)
                )
            }

            Barcode.TYPE_WIFI -> {
                val wifi = barcode.wifi
                val ssid = wifi?.ssid ?: "Unknown Wi-Fi"
                val password = wifi?.password ?: ""
                val encType = when (wifi?.encryptionType) {
                    Barcode.WiFi.TYPE_OPEN -> "Open (None)"
                    Barcode.WiFi.TYPE_WEP -> "WEP"
                    Barcode.WiFi.TYPE_WPA -> "WPA/WPA2/WPA3"
                    else -> "Unknown"
                }
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.WIFI,
                    title = ssid,
                    subtitle = "Wi-Fi Network ($encType)",
                    details = buildMap {
                        put("Network Name (SSID)", ssid)
                        if (password.isNotEmpty()) put("Password", password)
                        put("Security", encType)
                    }
                )
            }

            Barcode.TYPE_PHONE -> {
                val phone = barcode.phone?.number ?: raw
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.PHONE,
                    title = phone,
                    subtitle = "Phone Number",
                    details = mapOf("Phone Number" to phone)
                )
            }

            Barcode.TYPE_EMAIL -> {
                val email = barcode.email
                val address = email?.address ?: raw
                val subject = email?.subject ?: ""
                val body = email?.body ?: ""
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.EMAIL,
                    title = address,
                    subtitle = if (subject.isNotEmpty()) subject else "Email Message",
                    details = buildMap {
                        put("Recipient", address)
                        if (subject.isNotEmpty()) put("Subject", subject)
                        if (body.isNotEmpty()) put("Body", body)
                    }
                )
            }

            Barcode.TYPE_SMS -> {
                val sms = barcode.sms
                val phone = sms?.phoneNumber ?: raw
                val message = sms?.message ?: ""
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.SMS,
                    title = phone,
                    subtitle = if (message.isNotEmpty()) message else "SMS Message",
                    details = buildMap {
                        put("Send To", phone)
                        if (message.isNotEmpty()) put("Message", message)
                    }
                )
            }

            Barcode.TYPE_CONTACT_INFO -> {
                val contact = barcode.contactInfo
                val name = contact?.name?.formattedName ?: "Contact Info"
                val org = contact?.organization ?: ""
                val phones = contact?.phones?.joinToString(", ") { it.number ?: "" } ?: ""
                val emails = contact?.emails?.joinToString(", ") { it.address ?: "" } ?: ""
                val urls = contact?.urls?.joinToString(", ") ?: ""
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.CONTACT,
                    title = name,
                    subtitle = if (org.isNotEmpty()) org else phones,
                    details = buildMap {
                        put("Name", name)
                        if (org.isNotEmpty()) put("Organization", org)
                        if (phones.isNotEmpty()) put("Phone(s)", phones)
                        if (emails.isNotEmpty()) put("Email(s)", emails)
                        if (urls.isNotEmpty()) put("Website", urls)
                    }
                )
            }

            Barcode.TYPE_GEO -> {
                val geo = barcode.geoPoint
                val lat = geo?.lat ?: 0.0
                val lng = geo?.lng ?: 0.0
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.GEO,
                    title = "Coordinates: $lat, $lng",
                    subtitle = "Geographic Location",
                    details = mapOf("Latitude" to "$lat", "Longitude" to "$lng")
                )
            }

            Barcode.TYPE_CALENDAR_EVENT -> {
                val cal = barcode.calendarEvent
                val summary = cal?.summary ?: "Calendar Event"
                val location = cal?.location ?: ""
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.CALENDAR,
                    title = summary,
                    subtitle = if (location.isNotEmpty()) location else "Event",
                    details = buildMap {
                        put("Event", summary)
                        if (location.isNotEmpty()) put("Location", location)
                        cal?.description?.let { if (it.isNotEmpty()) put("Description", it) }
                    }
                )
            }

            Barcode.TYPE_PRODUCT -> {
                return ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = barcode.format,
                    type = BarcodeType.PRODUCT,
                    title = raw,
                    subtitle = "$formatName Product Code",
                    details = mapOf(
                        "Product Code" to raw,
                        "Format" to formatName
                    )
                )
            }

            else -> {
                // If it's a 1D barcode format, mark as PRODUCT
                val is1DBarcode = barcode.format in listOf(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_ITF
                )

                if (is1DBarcode) {
                    return ParsedBarcode(
                        rawText = raw,
                        formatName = formatName,
                        barcodeFormatInt = barcode.format,
                        type = BarcodeType.PRODUCT,
                        title = raw,
                        subtitle = "$formatName Barcode",
                        details = mapOf("Code" to raw, "Format" to formatName)
                    )
                }

                val lower = raw.lowercase().trim()
                return when {
                    lower.startsWith("http://") || lower.startsWith("https://") -> {
                        val host = try {
                            Uri.parse(raw).host ?: raw
                        } catch (e: Exception) {
                            raw
                        }
                        ParsedBarcode(
                            rawText = raw,
                            formatName = formatName,
                            barcodeFormatInt = barcode.format,
                            type = BarcodeType.URL,
                            title = host,
                            subtitle = raw,
                            details = mapOf("URL" to raw, "Domain" to host)
                        )
                    }
                    lower.startsWith("tel:") -> {
                        val number = raw.removePrefix("tel:").removePrefix("TEL:")
                        ParsedBarcode(
                            rawText = raw,
                            formatName = formatName,
                            barcodeFormatInt = barcode.format,
                            type = BarcodeType.PHONE,
                            title = number,
                            subtitle = "Phone Number",
                            details = mapOf("Phone Number" to number)
                        )
                    }
                    lower.startsWith("mailto:") -> {
                        val address = raw.removePrefix("mailto:").removePrefix("MAILTO:")
                        ParsedBarcode(
                            rawText = raw,
                            formatName = formatName,
                            barcodeFormatInt = barcode.format,
                            type = BarcodeType.EMAIL,
                            title = address,
                            subtitle = "Email Address",
                            details = mapOf("Recipient" to address)
                        )
                    }
                    lower.startsWith("wifi:") -> {
                        parseRawWifi(raw, formatName, barcode.format)
                    }
                    else -> {
                        ParsedBarcode(
                            rawText = raw,
                            formatName = formatName,
                            barcodeFormatInt = barcode.format,
                            type = BarcodeType.TEXT,
                            title = if (raw.length > 40) raw.take(40) + "..." else raw,
                            subtitle = "Plain Text",
                            details = mapOf("Content" to raw)
                        )
                    }
                }
            }
        }
    }

    private fun parseRawWifi(raw: String, formatName: String, formatInt: Int): ParsedBarcode {
        val clean = raw.removePrefix("WIFI:").removePrefix("wifi:")
        val tokens = clean.split(";")
        var ssid = ""
        var pass = ""
        var type = ""
        for (token in tokens) {
            when {
                token.startsWith("S:") || token.startsWith("s:") -> ssid = token.substring(2)
                token.startsWith("P:") || token.startsWith("p:") -> pass = token.substring(2)
                token.startsWith("T:") || token.startsWith("t:") -> type = token.substring(2)
            }
        }
        return ParsedBarcode(
            rawText = raw,
            formatName = formatName,
            barcodeFormatInt = formatInt,
            type = BarcodeType.WIFI,
            title = if (ssid.isNotEmpty()) ssid else "Wi-Fi Network",
            subtitle = "Wi-Fi Network ($type)",
            details = buildMap {
                put("Network Name (SSID)", ssid)
                if (pass.isNotEmpty()) put("Password", pass)
                if (type.isNotEmpty()) put("Security", type)
            }
        )
    }

    fun parseFromRaw(raw: String, formatName: String, formatInt: Int): ParsedBarcode {
        val lower = raw.lowercase().trim()
        val is1D = formatInt in listOf(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_ITF
        )

        return when {
            is1D -> ParsedBarcode(
                rawText = raw,
                formatName = formatName,
                barcodeFormatInt = formatInt,
                type = BarcodeType.PRODUCT,
                title = raw,
                subtitle = "$formatName Barcode",
                details = mapOf("Code" to raw, "Format" to formatName)
            )
            lower.startsWith("http://") || lower.startsWith("https://") -> {
                val host = try {
                    Uri.parse(raw).host ?: raw
                } catch (e: Exception) {
                    raw
                }
                ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = formatInt,
                    type = BarcodeType.URL,
                    title = host,
                    subtitle = raw,
                    details = mapOf("URL" to raw, "Domain" to host)
                )
            }
            lower.startsWith("wifi:") -> parseRawWifi(raw, formatName, formatInt)
            lower.startsWith("tel:") -> {
                val num = raw.removePrefix("tel:").removePrefix("TEL:")
                ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = formatInt,
                    type = BarcodeType.PHONE,
                    title = num,
                    subtitle = "Phone Number",
                    details = mapOf("Phone Number" to num)
                )
            }
            lower.startsWith("mailto:") -> {
                val mail = raw.removePrefix("mailto:").removePrefix("MAILTO:")
                ParsedBarcode(
                    rawText = raw,
                    formatName = formatName,
                    barcodeFormatInt = formatInt,
                    type = BarcodeType.EMAIL,
                    title = mail,
                    subtitle = "Email Address",
                    details = mapOf("Recipient" to mail)
                )
            }
            else -> ParsedBarcode(
                rawText = raw,
                formatName = formatName,
                barcodeFormatInt = formatInt,
                type = BarcodeType.TEXT,
                title = if (raw.length > 40) raw.take(40) + "..." else raw,
                subtitle = "Plain Text",
                details = mapOf("Content" to raw)
            )
        }
    }
}
