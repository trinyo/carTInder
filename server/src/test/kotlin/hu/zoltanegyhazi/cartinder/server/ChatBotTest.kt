package hu.zoltanegyhazi.cartinder.server

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatBotTest {
    private val car = ListingRow(
        id = 1, sellerId = 1, sellerName = "Józsi", sellerIsBot = true, make = "Volkswagen", model = "Golf IV",
        year = 2002, priceHuf = 1_290_000, km = 389_000, horsepower = 101, fuel = Fuel.DIZEL,
        body = BodyType.HATCHBACK, city = "Kecskemét", distanceKm = 12, bio = "", opener = "", pickiness = 0.2f,
        photoUrl = null, photoCredit = null, active = true,
    )

    @Test
    fun priceQuestionGetsPriceAnswer() {
        repeat(10) { seed ->
            val reply = ChatBot.reply(car, "Mennyi az ára?", 2, Random(seed))
            assertTrue("Ft" in reply || "forint" in reply || "tankolás" in reply, reply)
        }
    }

    @Test
    fun shortKeywordsMatchOnlyWordStarts() {
        // A "már" nem ár-kérdés, a "vártam" sem.
        repeat(10) { seed ->
            val reply = ChatBot.reply(car, "már nagyon vártam", 2, Random(seed))
            assertFalse("Ft" in reply, reply)
        }
    }

    @Test
    fun meetingQuestionMentionsCity() {
        repeat(10) { seed ->
            val reply = ChatBot.reply(car, "Mikor érsz rá egy próbaútra?", 2, Random(seed))
            assertTrue("Kecskemét" in reply || "Szombat" in reply || "szombat" in reply, reply)
        }
    }

    @Test
    fun fuelSpecificAnswer() {
        val reply = ChatBot.reply(car, "Mennyit fogyasztasz?", 2, Random(1))
        assertTrue("tengerig" in reply, reply)
    }
}
