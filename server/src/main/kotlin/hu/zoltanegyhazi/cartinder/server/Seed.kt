package hu.zoltanegyhazi.cartinder.server

import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.eq

data class DemoCar(
    val seller: String,
    val make: String,
    val model: String,
    val year: Int,
    val priceHuf: Int,
    val km: Int,
    val horsepower: Int,
    val fuel: Fuel,
    val body: BodyType,
    val city: String,
    val distanceKm: Int,
    val pickiness: Float,
    val bio: String,
    val opener: String,
    val photoUrl: String,
    val photoCredit: String,
)

/** Demó eladók (botok) a hirdetéseikkel, ha még nincsenek az adatbázisban. */
fun seedDemoData(db: Database) = transaction(db) {
    if (Users.selectAll().where { Users.isBot eq true }.count() > 0) return@transaction
    val now = System.currentTimeMillis()
    demoCars.forEachIndexed { index, car ->
        val sellerId = Users.insert {
            it[email] = "bot${index + 1}@demo.cartinder.hu"
            it[passwordHash] = "!"
            it[name] = car.seller
            it[city] = car.city
            it[isBot] = true
            it[createdAt] = now
        }[Users.id]
        Listings.insert {
            it[Listings.sellerId] = sellerId
            it[make] = car.make
            it[model] = car.model
            it[year] = car.year
            it[priceHuf] = car.priceHuf
            it[km] = car.km
            it[horsepower] = car.horsepower
            it[fuel] = car.fuel.name
            it[body] = car.body.name
            it[city] = car.city
            it[distanceKm] = car.distanceKm
            it[bio] = car.bio
            it[opener] = car.opener
            it[pickiness] = car.pickiness
            it[photoUrl] = car.photoUrl
            it[photoCredit] = car.photoCredit
            it[createdAt] = now + index
        }
    }
}

val demoCars = listOf(
    DemoCar(
        seller = "Józsi",
        make = "Volkswagen", model = "Golf IV 1.9 TDI", year = 2002, priceHuf = 1_290_000, km = 389_000, horsepower = 101,
        fuel = Fuel.DIZEL, body = BodyType.HATCHBACK, city = "Kecskemét", distanceKm = 12, pickiness = 0.2f,
        bio = "Csak egy idős hölgy használta, vasárnaponként a templomba. 389 ezer km, de a motor még csak most melegedett be.",
        opener = "Szia! A PD-m többet húz, mint amennyit fogyasztok. Kávé? ☕",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/1999_Volkswagen_Golf_SE_1.6_%28Front%29.jpg?width=800",
        photoCredit = "DieselFordMondeo / CC BY-SA 4.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Bence",
        make = "BMW", model = "330i (E46)", year = 2003, priceHuf = 2_490_000, km = 241_000, horsepower = 231,
        fuel = Fuel.BENZIN, body = BodyType.SEDAN, city = "Budapest", distanceKm = 4, pickiness = 0.55f,
        bio = "Index? Az a gazdagoknak való. Hátsókerék-hajtás, hat henger, nulla kompromisszum. Körforgalomban oldalazok.",
        opener = "Hé, láttalak a parkolóban. Driftelünk egyet? 😏",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/BMW_E46_front_20080328.jpg?width=800",
        photoCredit = "Rudolf Stricker / CC BY-SA 3.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Erzsi néni",
        make = "Trabant", model = "601 S", year = 1988, priceHuf = 1_150_000, km = 74_000, horsepower = 26,
        fuel = Fuel.BENZIN, body = BodyType.SEDAN, city = "Szeged", distanceKm = 31, pickiness = 0.1f,
        bio = "Duroplast karosszéria, rozsdamentes szív. Kétütemű vagyok, úgyhogy mindig kétszer gondolom át a dolgokat.",
        opener = "Szia! Nálam a keverék 1:33, nálunk mennyi lenne? 💨",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/1988_VEB_Sachenring_Trabant_P601S_Beige_%281_bearb_Sp%29_%28cropped%29.jpg?width=800",
        photoCredit = "Damian B Oh / CC BY-SA 4.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Kata",
        make = "Tesla", model = "Model 3 Long Range", year = 2021, priceHuf = 13_900_000, km = 68_000, horsepower = 498,
        fuel = Fuel.ELEKTROMOS, body = BodyType.SEDAN, city = "Budapest", distanceKm = 7, pickiness = 0.75f,
        bio = "0-100: 4,4 mp. Csendes típus vagyok, de a frissítéseim mindig meglepnek. Robotpilóta, de a szívemet te vezeted.",
        opener = "Over-the-air jöttem, hogy elmondjam: tetszel. ⚡",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/2019_Tesla_Model_3_Long_Range_Dual_Motor_in_Red_Multi-Coat%2C_front_left%2C_2021-05-30.jpg?width=800",
        photoCredit = "Elise240SX / CC BY-SA 4.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Pisti",
        make = "Suzuki", model = "Swift 1.3 GLX", year = 2006, priceHuf = 990_000, km = 182_000, horsepower = 92,
        fuel = Fuel.BENZIN, body = BodyType.HATCHBACK, city = "Esztergom", distanceKm = 46, pickiness = 0.15f,
        bio = "Esztergomban születtem, büszke magyar vagyok. Megbízható, olcsó fenntartású, minden parkolóhelyre beférek.",
        opener = "Szia! Hazai pálya, hazai alkatrészek. Ennél jobb nem lesz. 🇭🇺",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Suzuki_Swift_GS_perlrot_front.JPG?width=800",
        photoCredit = "Claus Märkl / Public domain, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Réka",
        make = "Mazda", model = "MX-5 NB", year = 1999, priceHuf = 3_200_000, km = 156_000, horsepower = 140,
        fuel = Fuel.BENZIN, body = BodyType.COUPE, city = "Debrecen", distanceKm = 220, pickiness = 0.5f,
        bio = "Kicsi vagyok, könnyű vagyok, és a válasz mindig Miata. Nyáron leengedem a tetőt, télen a garázsban várok rád.",
        opener = "Leengedjük a tetőt és megyünk a Balcsira? 🌅",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/1999_Mazda_MX-5_Miata_Base_in_Highlight_Silver_Metallic%2C_Front_Left%2C_08-06-2022.jpg?width=800",
        photoCredit = "Elise240SX / CC BY-SA 4.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Laci bácsi",
        make = "Lada", model = "2107", year = 1994, priceHuf = 890_000, km = 95_000, horsepower = 72,
        fuel = Fuel.BENZIN, body = BodyType.SEDAN, city = "Miskolc", distanceKm = 178, pickiness = 0.05f,
        bio = "Szovjet mérnöki csoda. Ha elromlok, egy kalapáccsal és egy kis szeretettel megjavítasz. Fűtés: van, és nem kapcsol ki.",
        opener = "Szia! Nálam a hideg indítás igazi szerelmi vallomás. 🧊",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Lada_2107_aka_Lada_Riva_October_1995_1452cc.jpg?width=800",
        photoCredit = "Charles01 / CC BY-SA 3.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Gergő",
        make = "Škoda", model = "Octavia Combi 2.0 TDI", year = 2017, priceHuf = 5_490_000, km = 212_000, horsepower = 150,
        fuel = Fuel.DIZEL, body = BodyType.KOMBI, city = "Győr", distanceKm = 125, pickiness = 0.3f,
        bio = "610 liter csomagtér, esernyő az ajtóban, jégkaparó a tanksapkában. A józan választás, akit anyukád is szeretne.",
        opener = "Elférne nálam a bőröndöd is, meg a kutyád is. 🐕",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/2018_Skoda_Octavia_VRS_Estate_Front.jpg?width=800",
        photoCredit = "Vauxford / CC BY-SA 4.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Ádám",
        make = "Toyota", model = "Prius III", year = 2012, priceHuf = 3_590_000, km = 298_000, horsepower = 136,
        fuel = Fuel.HIBRID, body = BodyType.HATCHBACK, city = "Budapest", distanceKm = 2, pickiness = 0.35f,
        bio = "Taxis múlttal, de elpusztíthatatlan jövővel. 4,2 liter/100 km, és csendben suhanok el a dugóban.",
        opener = "Szia! Takarékos vagyok, de veled nem spórolnék. 🍃",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/2009_Toyota_Prius_%28ZVW30R%29_liftback_%282011-12-06%29_01.jpg?width=800",
        photoCredit = "OSX / Public domain, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Marcsi",
        make = "Fiat", model = "Multipla", year = 2001, priceHuf = 790_000, km = 210_000, horsepower = 103,
        fuel = Fuel.BENZIN, body = BodyType.HATCHBACK, city = "Pécs", distanceKm = 196, pickiness = 0.0f,
        bio = "Tudom, mit mondanak rólam. De elöl is hárman ülhetünk. A belső szépség számít, nem?",
        opener = "Mindenki kinevet, de te mertél jobbra húzni. ❤️",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Fiat_Multipla_front_20080825.jpg?width=800",
        photoCredit = "Rudolf Stricker / Attribution, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Dominik",
        make = "Porsche", model = "911 Carrera (997)", year = 2008, priceHuf = 24_900_000, km = 118_000, horsepower = 325,
        fuel = Fuel.BENZIN, body = BodyType.COUPE, city = "Budapest", distanceKm = 9, pickiness = 0.9f,
        bio = "Motor hátul, szív a helyén. Nem keresek komoly kapcsolatot, csak hétvégi kanyargós utakat.",
        opener = "Szia. A boxer hangomat ismered már? 🏁",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Porsche_997_Turbo_-_Flickr_-_Alexandre_Pr%C3%A9vot_%288%29.jpg?width=800",
        photoCredit = "Alexandre Prévot from Nancy, France / CC BY-SA 2.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Feri",
        make = "Dacia", model = "Duster 1.5 dCi 4x4", year = 2019, priceHuf = 5_990_000, km = 97_000, horsepower = 115,
        fuel = Fuel.DIZEL, body = BodyType.SUV, city = "Veszprém", distanceKm = 98, pickiness = 0.25f,
        bio = "Nem vagyok flancos, de a földúton én nevetek utoljára. Klíma van, ennél többet ne kérj.",
        opener = "Szia! Sárban, hóban, Bakonyban – én ott leszek. 🏔️",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Dacia_Duster_Mk2_-_Front_View.jpg?width=800",
        photoCredit = "(G)jabz / CC BY 4.0, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Sanyi",
        make = "Volvo", model = "240 GL Kombi", year = 1991, priceHuf = 2_150_000, km = 412_000, horsepower = 116,
        fuel = Fuel.BENZIN, body = BodyType.KOMBI, city = "Sopron", distanceKm = 210, pickiness = 0.4f,
        bio = "Téglaforma, téglabiztonság. Svéd minimalizmus a nyolcvanas évekből. Túléltem már három tulajdonost.",
        opener = "Hej! Velem a következő 400 ezer km is biztonságban telik. 🧱",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/1988-1991_Volvo_240_GL_station_wagon_%282011-06-15%29_01.jpg?width=800",
        photoCredit = "OSX / Public domain, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Zoli",
        make = "Opel", model = "Astra G 1.6", year = 2000, priceHuf = 650_000, km = 265_000, horsepower = 101,
        fuel = Fuel.BENZIN, body = BodyType.HATCHBACK, city = "Nyíregyháza", distanceKm = 240, pickiness = 0.1f,
        bio = "Műszaki még két hónapig. A rozsda csak a karakterem része. Kis rejtély: hol szivárog az olaj?",
        opener = "Szia! Gyors döntés kell, mert jövő hónapban vizsga. 😅",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Opel_Astra_G_front_20081128.jpg?width=800",
        photoCredit = "Rudolf Stricker / Attribution, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Tamás",
        make = "Audi", model = "A4 Avant 2.0 TDI (B8)", year = 2014, priceHuf = 4_790_000, km = 276_000, horsepower = 143,
        fuel = Fuel.DIZEL, body = BodyType.KOMBI, city = "Székesfehérvár", distanceKm = 64, pickiness = 0.6f,
        bio = "Quattro nélkül, de nagy szívvel. Kiváló állapotban, németországi behozatal, a km óra persze eredeti.",
        opener = "Szia! Vezetett szervizkönyvem van. Neked? 📘",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Audi_A4_B8_Avant_front_20080811.jpg?width=800",
        photoCredit = "Rudolf Stricker / Attribution, Wikimedia Commons",
    ),
    DemoCar(
        seller = "Nóra",
        make = "Hyundai", model = "Kona Electric", year = 2022, priceHuf = 10_490_000, km = 41_000, horsepower = 204,
        fuel = Fuel.ELEKTROMOS, body = BodyType.SUV, city = "Tatabánya", distanceKm = 58, pickiness = 0.45f,
        bio = "Zöld rendszám, ingyen parkolás, 450 km hatótáv. A jövő vagyok, de a jelenben is jól érzem magam.",
        opener = "Töltsünk együtt egy kis időt? 🔌",
        photoUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/2020_Hyundai_Kona_Electric_in_Chalk_White%2C_front_left.jpg?width=800",
        photoCredit = "Mr.choppers / CC BY-SA 3.0, Wikimedia Commons",
    ),
)
