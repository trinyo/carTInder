package hu.zoltanegyhazi.cartinder.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val hungarian = Locale.forLanguageTag("hu-HU")

/** Ma: "14:05", az idén: "okt. 9.", korábban: "2025. 10. 09." */
fun formatMessageTime(epochMs: Long, now: Long = System.currentTimeMillis()): String {
    val then = Calendar.getInstance().apply { timeInMillis = epochMs }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val pattern = when {
        then.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> "yyyy. MM. dd."
        then.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) -> "MMM d."
        else -> "HH:mm"
    }
    return SimpleDateFormat(pattern, hungarian).format(Date(epochMs))
}
