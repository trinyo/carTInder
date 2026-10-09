package hu.zoltanegyhazi.cartinder.data

import kotlin.random.Random

/** Egyszerű kulcsszavas válaszgenerátor, hogy az autók visszaírjanak. */
object ChatBot {
    fun reply(car: Car, userText: String, carMessageCount: Int, random: Random = Random.Default): String {
        val text = userText.lowercase()
        val tokens = text.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        // Szó eleji egyezés, hogy pl. az "ár" ne találjon a "már"-ra.
        fun has(vararg keys: String) = keys.any { key ->
            if (' ' in key) key in text else tokens.any { it.startsWith(key) }
        }

        return when {
            has("mennyi", "ár", "alku", "olcsó", "drága", "forint", "ft") -> listOf(
                "${formatHuf(car.priceHuf)}, de neked engedek… mondjuk egy tankolást. ⛽",
                "Az ár ${formatHuf(car.priceHuf)}. Alku? Csak személyesen, a szemembe nézve. 👀",
                "Nem vagyok olcsó, de megérem minden forintot. 💸",
            ).random(random)

            has("km", "futás", "kilométer", "óra") -> listOf(
                "${formatKm(car.km)} van bennem. Mindegyikre büszke vagyok. 🛣️",
                "A km óra? Eredeti. Esküszöm. Majdnem. 😇",
                "${formatKm(car.km)}, de belül még fiatal vagyok.",
            ).random(random)

            has("fogyaszt", "tank", "liter", "tölt", "hatótáv", "akku") -> when (car.fuel) {
                Fuel.ELEKTROMOS -> "Most épp töltök, de rád mindig van energiám. 🔋"
                Fuel.HIBRID -> "Városban szinte semmit nem eszem. Diétás típus vagyok. 🍃"
                Fuel.DIZEL -> "Egy tankkal elviszlek a tengerig. Talán vissza is. 🌊"
                Fuel.BENZIN -> if (car.horsepower > 200) {
                    "Fogyasztás? Nem kérdezünk ilyet egy első randin. 🔥"
                } else {
                    "Szerény vagyok, 6-7 liter körül. Te mennyit eszel? 😄"
                }
            }

            has("találkoz", "randi", "próbaút", "megnéz", "mikor", "hol", "ráérsz") -> listOf(
                "Próbaút? Mondjuk szombat délelőtt? Helyszín: ${car.city}. 📍",
                "Gyere el hozzám (${car.city}), kávé után körbeviszlek. ☕",
                "Csak jogsival és jó szándékkal. Szombat jó? 🗓️",
            ).random(random)

            has("szia", "hello", "helló", "hali", "csá", "szevasz", "jó napot") -> listOf(
                "Szia! 😊 Örülök, hogy írtál!",
                "Hali! Épp a parkolóban unatkoztam.",
                "Szevasz! Végre valaki, aki nem csak a felnimet nézi. 😏",
            ).random(random)

            has("szép", "gyönyörű", "tetszel", "jól nézel", "cuki", "dögös") -> listOf(
                "Jaj, ne már, elpirul a fényezésem. 🙈",
                "Köszi! Tegnap voltam autómosóban. ✨",
                "Te is jól nézel ki a jogsifotódon, gondolom. 😉",
            ).random(random)

            has("rozsda", "hiba", "baj", "szerviz", "olaj", "műszaki") -> listOf(
                "Mindenkinek vannak hibái. Az enyémek legalább láthatók. 🔧",
                "Szervizkönyv? Van. Valahol a kesztyűtartóban. Talán.",
                "Műszaki vizsgán még sosem buktam meg. Na jó, egyszer.",
            ).random(random)

            else -> (personal(car) + generic).let { pool ->
                if (carMessageCount <= 1) "Na és te mivel jársz most? 🚗" else pool.random(random)
            }
        }
    }

    private fun personal(car: Car) = when (car.body) {
        BodyType.SUV -> listOf("Szereted a földutakat? Én imádom. 🏞️")
        BodyType.COUPE -> listOf("Kanyargós utakon érzem magam igazán elemben. 🏁")
        BodyType.KOMBI -> listOf("Hatalmas csomagterem van. A múltadnak is jut hely. 🧳")
        BodyType.HATCHBACK -> listOf("Kicsi vagyok, de mindenhol leparkolok. 🅿️")
        BodyType.SEDAN -> listOf("Klasszikus típus vagyok, nem divatból jöttem. 🎩")
    }

    private val generic = listOf(
        "Haha, ez jó! 😂",
        "Mesélj még magadról!",
        "Te milyen zenét hallgatnál bennem? 🎶",
        "Ez nagyon kedves tőled. 💕",
        "Hmm, ezen elgondolkodtam a garázsban.",
        "Hétvégén mit csinálsz? Nem akarsz egy kört?",
        "Tudod, a legtöbben csak a lóerőt kérdezik. Te más vagy.",
    )
}
