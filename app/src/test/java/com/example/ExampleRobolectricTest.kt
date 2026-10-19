package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.parser.BarcodeParser
import com.example.parser.BarcodeType
import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("QR Scanner", appName)
    }

    @Test
    fun `test url parsing`() {
        val raw = "https://example.com/test"
        val parsed = BarcodeParser.parseFromRaw(raw, "QR Code", Barcode.FORMAT_QR_CODE)
        assertEquals(BarcodeType.URL, parsed.type)
        assertEquals("example.com", parsed.title)
    }

    @Test
    fun `test wifi parsing`() {
        val raw = "WIFI:S:MyNetwork;T:WPA;P:Secret123;;"
        val parsed = BarcodeParser.parseFromRaw(raw, "QR Code", Barcode.FORMAT_QR_CODE)
        assertEquals(BarcodeType.WIFI, parsed.type)
        assertEquals("MyNetwork", parsed.title)
    }

    @Test
    fun `test phone parsing`() {
        val raw = "tel:+1234567890"
        val parsed = BarcodeParser.parseFromRaw(raw, "QR Code", Barcode.FORMAT_QR_CODE)
        assertEquals(BarcodeType.PHONE, parsed.type)
        assertEquals("+1234567890", parsed.title)
    }
}
