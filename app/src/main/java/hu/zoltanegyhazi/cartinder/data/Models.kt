package hu.zoltanegyhazi.cartinder.data

import hu.zoltanegyhazi.cartinder.data.api.ListingDto

enum class Fuel(val label: String) {
    BENZIN("Benzin"),
    DIZEL("Dízel"),
    HIBRID("Hibrid"),
    ELEKTROMOS("Elektromos"),
}

enum class BodyType(val label: String) {
    SEDAN("Szedán"),
    HATCHBACK("Ferdehátú"),
    KOMBI("Kombi"),
    SUV("SUV"),
    COUPE("Kupé"),
}

enum class SwipeDirection { LEFT, RIGHT, UP }

data class CarFilter(
    val maxPrice: Int = MAX_PRICE,
    val maxKm: Int = MAX_KM,
    val fuels: Set<Fuel> = Fuel.entries.toSet(),
) {
    /** A szerver query paraméterei; a "bármi" értékeket nem küldjük. */
    fun toQuery(): Map<String, String> = buildMap {
        if (maxPrice < MAX_PRICE) put("maxPrice", maxPrice.toString())
        if (maxKm < MAX_KM) put("maxKm", maxKm.toString())
        if (fuels != Fuel.entries.toSet()) put("fuels", fuels.sortedBy { it.ordinal }.joinToString(",") { it.name })
    }

    companion object {
        const val MAX_PRICE = 25_000_000
        const val MAX_KM = 450_000
    }
}

val ListingDto.title get() = "$make $model"

/** A szerver relatív (/uploads/...) és abszolút fotó URL-jeit is kezeli. */
fun resolvePhotoUrl(baseUrl: String, photoUrl: String?): String? = when {
    photoUrl.isNullOrBlank() -> null
    photoUrl.startsWith("/") -> baseUrl.trimEnd('/') + photoUrl
    else -> photoUrl
}

fun formatHuf(value: Int): String = groupThousands(value) + " Ft"

fun formatKm(value: Int): String = groupThousands(value) + " km"

private fun groupThousands(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(" ").reversed()

/** Helyi hálózati cím-e (ehhez Android 17-től külön engedély kell). */
fun isLocalNetworkUrl(url: String): Boolean {
    val host = url.substringAfter("://").substringBefore('/').substringBefore(':').lowercase()
    if (host == "localhost" || host.endsWith(".local")) return true
    val parts = host.split('.').map { it.toIntOrNull() ?: return false }
    if (parts.size != 4) return false
    val (a, b) = parts
    return a == 10 || a == 127 || (a == 192 && b == 168) || (a == 172 && b in 16..31) || (a == 169 && b == 254)
}
