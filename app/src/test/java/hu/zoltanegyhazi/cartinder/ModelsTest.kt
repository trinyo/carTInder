package hu.zoltanegyhazi.cartinder

import hu.zoltanegyhazi.cartinder.data.BodyType
import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.formatHuf
import hu.zoltanegyhazi.cartinder.data.isLocalNetworkUrl
import hu.zoltanegyhazi.cartinder.data.resolvePhotoUrl
import hu.zoltanegyhazi.cartinder.ui.buildRequest
import hu.zoltanegyhazi.cartinder.ui.formatMessageTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ModelsTest {
    @Test
    fun formatsHungarianPrice() {
        assertEquals("1 290 000 Ft", formatHuf(1_290_000))
    }

    @Test
    fun filterQueryOmitsDefaults() {
        assertEquals(emptyMap<String, String>(), CarFilter().toQuery())
        assertEquals(mapOf("maxKm" to "100000", "fuels" to "HIBRID,ELEKTROMOS"),
            CarFilter(maxKm = 100_000, fuels = setOf(Fuel.ELEKTROMOS, Fuel.HIBRID)).toQuery())
    }

    @Test
    fun resolvesRelativeAndAbsolutePhotoUrls() {
        assertEquals("http://10.0.2.2:8080/uploads/a.jpg", resolvePhotoUrl("http://10.0.2.2:8080/", "/uploads/a.jpg"))
        assertEquals("https://example.com/a.jpg", resolvePhotoUrl("http://x", "https://example.com/a.jpg"))
        assertNull(resolvePhotoUrl("http://x", null))
    }

    @Test
    fun listingFormValidation() {
        fun build(year: String = "2004", price: String = "1500000", make: String = "Opel", city: String = "Szeged") =
            buildRequest(make, "Corsa", year, price, "180000", "60", Fuel.BENZIN, BodyType.HATCHBACK, city, " leírás ", "", true)

        val ok = build().getOrThrow()
        assertEquals(2004, ok.year)
        assertEquals("leírás", ok.bio)
        assertEquals("Add meg a márkát.", build(make = " ").exceptionOrNull()?.message)
        assertEquals("Add meg az évjáratot.", build(year = "").exceptionOrNull()?.message)
        assertEquals("Add meg a várost.", build(city = "").exceptionOrNull()?.message)
        assertTrue(build(price = "abc").isFailure)
    }

    @Test
    fun messageTimeFormats() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 9, 18, 0) }.timeInMillis
        val today = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 9, 14, 5) }.timeInMillis
        val earlier = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 2, 9, 0) }.timeInMillis
        val lastYear = Calendar.getInstance().apply { set(2025, Calendar.DECEMBER, 24, 9, 0) }.timeInMillis
        assertEquals("14:05", formatMessageTime(today, now))
        assertTrue(formatMessageTime(earlier, now).startsWith("márc"))
        assertEquals("2025. 12. 24.", formatMessageTime(lastYear, now))
    }

    @Test
    fun detectsLocalNetworkAddresses() {
        listOf("http://10.0.2.2:8080", "http://192.168.1.10:8080/", "http://172.20.0.5", "http://localhost:8080", "http://pc.local:8080")
            .forEach { assertTrue(it, isLocalNetworkUrl(it)) }
        listOf("https://cartinder.example.com", "http://172.32.0.1", "http://8.8.8.8", "http://192.169.0.1")
            .forEach { assertTrue(it, !isLocalNetworkUrl(it)) }
    }
}
