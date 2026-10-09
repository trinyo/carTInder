package hu.zoltanegyhazi.cartinder.data

import java.net.URLEncoder

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

data class Car(
    val id: Int,
    val make: String,
    val model: String,
    val year: Int,
    val priceHuf: Int,
    val km: Int,
    val horsepower: Int,
    val fuel: Fuel,
    val body: BodyType,
    val color: Long,
    val background: Long,
    val city: String,
    val distanceKm: Int,
    val bio: String,
    val opener: String,
    /** 0 = mindenkivel matchel, 1 = senkivel. */
    val pickiness: Float,
    /** Fájlnév a Wikimedia Commonson. */
    val photoFile: String,
) {
    val title get() = "$make $model"

    val photoUrl get() =
        "https://commons.wikimedia.org/wiki/Special:FilePath/" +
            URLEncoder.encode(photoFile.replace(' ', '_'), "UTF-8").replace("+", "%20") + "?width=800"
}

fun formatHuf(value: Int): String = groupThousands(value) + " Ft"

fun formatKm(value: Int): String = groupThousands(value) + " km"

private fun groupThousands(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(" ").reversed()

val sampleCars = listOf(
    Car(
        1, "Volkswagen", "Golf IV 1.9 TDI", 2002, 1_290_000, 389_000, 101, Fuel.DIZEL, BodyType.HATCHBACK,
        0xFF2B4C7E, 0xFFB8D4F5, "Kecskemét", 12,
        "Csak egy idős hölgy használta, vasárnaponként a templomba. 389 ezer km, de a motor még csak most melegedett be.",
        "Szia! A PD-m többet húz, mint amennyit fogyasztok. Kávé? ☕", 0.2f,
        "1999 Volkswagen Golf SE 1.6 (Front).jpg",
    ),
    Car(
        2, "BMW", "330i (E46)", 2003, 2_490_000, 241_000, 231, Fuel.BENZIN, BodyType.SEDAN,
        0xFF1C1C1C, 0xFFC9CED6, "Budapest", 4,
        "Index? Az a gazdagoknak való. Hátsókerék-hajtás, hat henger, nulla kompromisszum. Körforgalomban oldalazok.",
        "Hé, láttalak a parkolóban. Driftelünk egyet? 😏", 0.55f,
        "BMW E46 front 20080328.jpg",
    ),
    Car(
        3, "Trabant", "601 S", 1988, 1_150_000, 74_000, 26, Fuel.BENZIN, BodyType.SEDAN,
        0xFF8FB3A3, 0xFFF3E6C4, "Szeged", 31,
        "Duroplast karosszéria, rozsdamentes szív. Kétütemű vagyok, úgyhogy mindig kétszer gondolom át a dolgokat.",
        "Szia! Nálam a keverék 1:33, nálunk mennyi lenne? 💨", 0.1f,
        "1988 VEB Sachenring Trabant P601S Beige (1 bearb Sp) (cropped).jpg",
    ),
    Car(
        4, "Tesla", "Model 3 Long Range", 2021, 13_900_000, 68_000, 498, Fuel.ELEKTROMOS, BodyType.SEDAN,
        0xFFE8E8EA, 0xFF2E3440, "Budapest", 7,
        "0-100: 4,4 mp. Csendes típus vagyok, de a frissítéseim mindig meglepnek. Robotpilóta, de a szívemet te vezeted.",
        "Over-the-air jöttem, hogy elmondjam: tetszel. ⚡", 0.75f,
        "2019 Tesla Model 3 Long Range Dual Motor in Red Multi-Coat, front left, 2021-05-30.jpg",
    ),
    Car(
        5, "Suzuki", "Swift 1.3 GLX", 2006, 990_000, 182_000, 92, Fuel.BENZIN, BodyType.HATCHBACK,
        0xFFC62828, 0xFFFFD6CC, "Esztergom", 46,
        "Esztergomban születtem, büszke magyar vagyok. Megbízható, olcsó fenntartású, minden parkolóhelyre beférek.",
        "Szia! Hazai pálya, hazai alkatrészek. Ennél jobb nem lesz. 🇭🇺", 0.15f,
        "Suzuki Swift GS perlrot front.JPG",
    ),
    Car(
        6, "Mazda", "MX-5 NB", 1999, 3_200_000, 156_000, 140, Fuel.BENZIN, BodyType.COUPE,
        0xFFFFB300, 0xFFFFF3C4, "Debrecen", 220,
        "Kicsi vagyok, könnyű vagyok, és a válasz mindig Miata. Nyáron leengedem a tetőt, télen a garázsban várok rád.",
        "Leengedjük a tetőt és megyünk a Balcsira? 🌅", 0.5f,
        "1999 Mazda MX-5 Miata Base in Highlight Silver Metallic, Front Left, 08-06-2022.jpg",
    ),
    Car(
        7, "Lada", "2107", 1994, 890_000, 95_000, 72, Fuel.BENZIN, BodyType.SEDAN,
        0xFF5D7F3A, 0xFFE3EBC9, "Miskolc", 178,
        "Szovjet mérnöki csoda. Ha elromlok, egy kalapáccsal és egy kis szeretettel megjavítasz. Fűtés: van, és nem kapcsol ki.",
        "Szia! Nálam a hideg indítás igazi szerelmi vallomás. 🧊", 0.05f,
        "Lada 2107 aka Lada Riva October 1995 1452cc.jpg",
    ),
    Car(
        8, "Škoda", "Octavia Combi 2.0 TDI", 2017, 5_490_000, 212_000, 150, Fuel.DIZEL, BodyType.KOMBI,
        0xFF6E7B85, 0xFFD5DEE5, "Győr", 125,
        "610 liter csomagtér, esernyő az ajtóban, jégkaparó a tanksapkában. A józan választás, akit anyukád is szeretne.",
        "Elférne nálam a bőröndöd is, meg a kutyád is. 🐕", 0.3f,
        "2018 Skoda Octavia VRS Estate Front.jpg",
    ),
    Car(
        9, "Toyota", "Prius III", 2012, 3_590_000, 298_000, 136, Fuel.HIBRID, BodyType.HATCHBACK,
        0xFF3D7EAA, 0xFFD8EEFA, "Budapest", 2,
        "Taxis múlttal, de elpusztíthatatlan jövővel. 4,2 liter/100 km, és csendben suhanok el a dugóban.",
        "Szia! Takarékos vagyok, de veled nem spórolnék. 🍃", 0.35f,
        "2009 Toyota Prius (ZVW30R) liftback (2011-12-06) 01.jpg",
    ),
    Car(
        10, "Fiat", "Multipla", 2001, 790_000, 210_000, 103, Fuel.BENZIN, BodyType.HATCHBACK,
        0xFF9CCC65, 0xFFF0F7DD, "Pécs", 196,
        "Tudom, mit mondanak rólam. De elöl is hárman ülhetünk. A belső szépség számít, nem?",
        "Mindenki kinevet, de te mertél jobbra húzni. ❤️", 0.0f,
        "Fiat Multipla front 20080825.jpg",
    ),
    Car(
        11, "Porsche", "911 Carrera (997)", 2008, 24_900_000, 118_000, 325, Fuel.BENZIN, BodyType.COUPE,
        0xFFB71C1C, 0xFF263238, "Budapest", 9,
        "Motor hátul, szív a helyén. Nem keresek komoly kapcsolatot, csak hétvégi kanyargós utakat.",
        "Szia. A boxer hangomat ismered már? 🏁", 0.9f,
        "Porsche 997 Turbo - Flickr - Alexandre Prévot (8).jpg",
    ),
    Car(
        12, "Dacia", "Duster 1.5 dCi 4x4", 2019, 5_990_000, 97_000, 115, Fuel.DIZEL, BodyType.SUV,
        0xFFD84315, 0xFFFFE0CC, "Veszprém", 98,
        "Nem vagyok flancos, de a földúton én nevetek utoljára. Klíma van, ennél többet ne kérj.",
        "Szia! Sárban, hóban, Bakonyban – én ott leszek. 🏔️", 0.25f,
        "Dacia Duster Mk2 - Front View.jpg",
    ),
    Car(
        13, "Volvo", "240 GL Kombi", 1991, 2_150_000, 412_000, 116, Fuel.BENZIN, BodyType.KOMBI,
        0xFF1565C0, 0xFFE1ECF7, "Sopron", 210,
        "Téglaforma, téglabiztonság. Svéd minimalizmus a nyolcvanas évekből. Túléltem már három tulajdonost.",
        "Hej! Velem a következő 400 ezer km is biztonságban telik. 🧱", 0.4f,
        "1988-1991 Volvo 240 GL station wagon (2011-06-15) 01.jpg",
    ),
    Car(
        14, "Opel", "Astra G 1.6", 2000, 650_000, 265_000, 101, Fuel.BENZIN, BodyType.HATCHBACK,
        0xFF9E9E9E, 0xFFEDEDED, "Nyíregyháza", 240,
        "Műszaki még két hónapig. A rozsda csak a karakterem része. Kis rejtély: hol szivárog az olaj?",
        "Szia! Gyors döntés kell, mert jövő hónapban vizsga. 😅", 0.1f,
        "Opel Astra G front 20081128.jpg",
    ),
    Car(
        15, "Audi", "A4 Avant 2.0 TDI (B8)", 2014, 4_790_000, 276_000, 143, Fuel.DIZEL, BodyType.KOMBI,
        0xFF37474F, 0xFFCFD8DC, "Székesfehérvár", 64,
        "Quattro nélkül, de nagy szívvel. Kiváló állapotban, németországi behozatal, a km óra persze eredeti.",
        "Szia! Vezetett szervizkönyvem van. Neked? 📘", 0.6f,
        "Audi A4 B8 Avant front 20080811.jpg",
    ),
    Car(
        16, "Hyundai", "Kona Electric", 2022, 10_490_000, 41_000, 204, Fuel.ELEKTROMOS, BodyType.SUV,
        0xFF00897B, 0xFFCCF2EC, "Tatabánya", 58,
        "Zöld rendszám, ingyen parkolás, 450 km hatótáv. A jövő vagyok, de a jelenben is jól érzem magam.",
        "Töltsünk együtt egy kis időt? 🔌", 0.45f,
        "2020 Hyundai Kona Electric in Chalk White, front left.jpg",
    ),
)
