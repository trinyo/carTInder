package hu.zoltanegyhazi.cartinder.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 254).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val name = varchar("name", 60)
    val city = varchar("city", 60).default("")
    val bio = varchar("bio", 500).default("")
    val currentCar = varchar("current_car", 100).default("")
    val isBot = bool("is_bot").default(false)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Listings : Table("listings") {
    val id = integer("id").autoIncrement()
    val sellerId = integer("seller_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val make = varchar("make", 40)
    val model = varchar("model", 80)
    val year = integer("year")
    val priceHuf = integer("price_huf")
    val km = integer("km")
    val horsepower = integer("horsepower")
    val fuel = varchar("fuel", 20)
    val body = varchar("body", 20)
    val city = varchar("city", 60)
    val distanceKm = integer("distance_km").default(0)
    val bio = varchar("bio", 1000).default("")
    val opener = varchar("opener", 300).default("")
    val pickiness = float("pickiness").default(0.3f)
    val photoUrl = varchar("photo_url", 500).nullable()
    val photoCredit = varchar("photo_credit", 200).nullable()
    val active = bool("active").default(true)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Swipes : Table("swipes") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val listingId = integer("listing_id").references(Listings.id, onDelete = ReferenceOption.CASCADE)
    val direction = varchar("direction", 10)
    /** NONE (balra), PENDING, ACCEPTED, DECLINED */
    val status = varchar("status", 10)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId, listingId)
    }
}

object Matches : Table("matches") {
    val id = integer("id").autoIncrement()
    val swipeId = integer("swipe_id").references(Swipes.id, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val listingId = integer("listing_id").references(Listings.id, onDelete = ReferenceOption.CASCADE)
    val buyerId = integer("buyer_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val sellerId = integer("seller_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val superLike = bool("super_like")
    val buyerLastRead = integer("buyer_last_read").default(0)
    val sellerLastRead = integer("seller_last_read").default(0)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Messages : Table("messages") {
    val id = integer("id").autoIncrement()
    val matchId = integer("match_id").references(Matches.id, onDelete = ReferenceOption.CASCADE)
    val senderId = integer("sender_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val text = varchar("text", 2000)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

fun connectDatabase(config: Config): Database {
    if (config.databaseUrl.startsWith("jdbc:sqlite:")) {
        config.databaseUrl.removePrefix("jdbc:sqlite:").substringBefore('?').let { path ->
            if (path.isNotEmpty() && !path.startsWith(":") && !path.startsWith("file:")) {
                File(path).absoluteFile.parentFile?.mkdirs()
            }
        }
    }
    val db = if (config.databaseUrl.startsWith("jdbc:sqlite:")) {
        // A SQLite csak így tartja be a külső kulcsokat (ON DELETE CASCADE).
        Database.connect(
            config.databaseUrl,
            driver = "org.sqlite.JDBC",
            setupConnection = { it.createStatement().use { s -> s.execute("PRAGMA foreign_keys = ON") } },
        )
    } else {
        Database.connect(config.databaseUrl, user = config.databaseUser, password = config.databasePassword)
    }
    transaction(db) {
        SchemaUtils.create(Users, Listings, Swipes, Matches, Messages)
    }
    return db
}

suspend fun <T> Database.query(block: suspend () -> T): T =
    withContext(Dispatchers.IO) { suspendTransaction(this@query) { block() } }
