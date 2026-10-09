package hu.zoltanegyhazi.cartinder.server

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.io.File
import java.time.Year
import java.util.UUID
import kotlin.random.Random

data class ListingRow(
    val id: Int,
    val sellerId: Int,
    val sellerName: String,
    val sellerIsBot: Boolean,
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
    val bio: String,
    val opener: String,
    val pickiness: Float,
    val photoUrl: String?,
    val photoCredit: String?,
    val active: Boolean,
) {
    fun toDto() = ListingDto(
        id, sellerId, sellerName, make, model, year, priceHuf, km, horsepower, fuel, body, city, distanceKm, bio,
        photoUrl, photoCredit, active,
    )
}

data class FeedFilter(val maxPrice: Int? = null, val maxKm: Int? = null, val fuels: Set<Fuel>? = null)

private const val NONE = "NONE"
private const val PENDING = "PENDING"
private const val ACCEPTED = "ACCEPTED"
private const val DECLINED = "DECLINED"

private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

class CarTinderService(
    private val db: Database,
    private val config: Config,
    private val bots: BotRunner,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    init {
        bots.reply = ::botReply
    }

    // --- Fiókok ---

    suspend fun register(req: RegisterRequest): Int = db.query {
        val email = req.email.trim().lowercase()
        if (!emailRegex.matches(email)) badRequest("Érvénytelen e-mail cím.")
        if (req.password.length < 8) badRequest("A jelszó legalább 8 karakter legyen.")
        val name = req.name.trim()
        if (name.isEmpty() || name.length > 60) badRequest("Add meg a neved (legfeljebb 60 karakter).")
        if (Users.selectAll().where { Users.email eq email }.count() > 0) {
            conflict("Ezzel az e-mail címmel már regisztráltak.")
        }
        Users.insert {
            it[Users.email] = email
            it[passwordHash] = BCrypt.withDefaults().hashToString(10, req.password.toCharArray())
            it[Users.name] = name
            it[city] = req.city.trim().take(60)
            it[createdAt] = clock()
        }[Users.id]
    }

    suspend fun login(req: LoginRequest): Int = db.query {
        val row = Users.selectAll()
            .where { (Users.email eq req.email.trim().lowercase()) and (Users.isBot eq false) }
            .singleOrNull()
        val ok = row != null && BCrypt.verifyer().verify(req.password.toCharArray(), row[Users.passwordHash]).verified
        if (!ok) unauthorized("Hibás e-mail cím vagy jelszó.")
        row!![Users.id]
    }

    suspend fun me(userId: Int): MeDto = db.query {
        val u = Users.selectAll().where { Users.id eq userId }.singleOrNull() ?: unauthorized("A fiók nem létezik.")
        MeDto(
            id = userId,
            email = u[Users.email],
            name = u[Users.name],
            city = u[Users.city],
            bio = u[Users.bio],
            currentCar = u[Users.currentCar],
            stats = StatsDto(
                swipes = Swipes.selectAll().where { Swipes.userId eq userId }.count().toInt(),
                likes = Swipes.selectAll().where { (Swipes.userId eq userId) and (Swipes.direction neq SwipeDirection.LEFT.name) }.count().toInt(),
                matches = Matches.selectAll().where { (Matches.buyerId eq userId) or (Matches.sellerId eq userId) }.count().toInt(),
                listings = Listings.selectAll().where { Listings.sellerId eq userId }.count().toInt(),
            ),
        )
    }

    suspend fun updateProfile(userId: Int, req: UpdateProfileRequest): MeDto {
        val name = req.name.trim()
        if (name.isEmpty() || name.length > 60) badRequest("Add meg a neved (legfeljebb 60 karakter).")
        db.query {
            Users.update({ Users.id eq userId }) {
                it[Users.name] = name
                it[city] = req.city.trim().take(60)
                it[bio] = req.bio.trim().take(500)
                it[currentCar] = req.currentCar.trim().take(100)
            }
        }
        return me(userId)
    }

    // --- Felfedezés ---

    suspend fun feed(userId: Int, filter: FeedFilter, limit: Int = 50): List<ListingDto> = db.query {
        val seen = Swipes.select(Swipes.listingId).where { Swipes.userId eq userId }
        val query = listingQuery()
            .where { (Listings.active eq true) and (Listings.sellerId neq userId) and (Listings.id notInSubQuery seen) }
        filter.maxPrice?.let { query.andWhere { Listings.priceHuf lessEq it } }
        filter.maxKm?.let { query.andWhere { Listings.km lessEq it } }
        filter.fuels?.let { fuels -> query.andWhere { Listings.fuel inList fuels.map { it.name } } }
        query.orderBy(Listings.createdAt to SortOrder.ASC, Listings.id to SortOrder.ASC)
            .limit(limit)
            .map { it.toListing().toDto() }
    }

    suspend fun swipe(userId: Int, listingId: Int, direction: SwipeDirection): SwipeResponse {
        val matchId = db.query {
            val listing = findListing(listingId) ?: notFound("A hirdetés nem található.")
            if (listing.sellerId == userId) forbidden("A saját hirdetésedet nem húzhatod.")
            if (!listing.active) notFound("A hirdetés már nem aktív.")
            if (Swipes.selectAll().where { (Swipes.userId eq userId) and (Swipes.listingId eq listingId) }.count() > 0) {
                conflict("Ezt az autót már láttad.")
            }
            val liked = direction != SwipeDirection.LEFT
            val swipeId = Swipes.insert {
                it[Swipes.userId] = userId
                it[Swipes.listingId] = listingId
                it[Swipes.direction] = direction.name
                it[status] = if (liked) PENDING else NONE
                it[createdAt] = clock()
            }[Swipes.id]

            if (liked && listing.sellerIsBot) {
                // A demó eladók azonnal döntenek; az elutasítást a vevő nem látja.
                val accepts = direction == SwipeDirection.UP || random.nextFloat() >= listing.pickiness
                if (accepts) {
                    createMatch(swipeId, listing, buyerId = userId, superLike = direction == SwipeDirection.UP)
                } else {
                    Swipes.update({ Swipes.id eq swipeId }) { it[status] = DECLINED }
                    null
                }
            } else {
                null
            }
        }
        return SwipeResponse(match = matchId?.let { match(userId, it) })
    }

    suspend fun undoLastSwipe(userId: Int): UndoResponse = db.query {
        val last = Swipes.selectAll().where { Swipes.userId eq userId }
            .orderBy(Swipes.createdAt to SortOrder.DESC, Swipes.id to SortOrder.DESC)
            .limit(1)
            .singleOrNull() ?: notFound("Nincs mit visszavonni.")
        val swipeId = last[Swipes.id]
        Matches.selectAll().where { Matches.swipeId eq swipeId }.singleOrNull()?.let { match ->
            val wrote = Messages.selectAll()
                .where { (Messages.matchId eq match[Matches.id]) and (Messages.senderId eq userId) }
                .count() > 0
            if (wrote) conflict("Már írtál neki, ezt nem lehet visszavonni.")
            Matches.deleteWhere { Matches.id eq match[Matches.id] }
        }
        Swipes.deleteWhere { Swipes.id eq swipeId }
        UndoResponse(findListing(last[Swipes.listingId])!!.toDto())
    }

    // --- Beérkező lájkok (eladóként) ---

    suspend fun incomingLikes(userId: Int): List<IncomingLikeDto> = db.query {
        Swipes.join(Listings, JoinType.INNER, Swipes.listingId, Listings.id)
            .join(Users, JoinType.INNER, Swipes.userId, Users.id)
            .selectAll()
            .where { (Listings.sellerId eq userId) and (Swipes.status eq PENDING) }
            .orderBy(Swipes.createdAt to SortOrder.DESC)
            .map { row ->
                IncomingLikeDto(
                    id = row[Swipes.id],
                    listing = findListing(row[Swipes.listingId])!!.toDto(),
                    buyer = row.toPublicUser(),
                    superLike = row[Swipes.direction] == SwipeDirection.UP.name,
                    createdAt = row[Swipes.createdAt],
                )
            }
            .sortedByDescending { it.superLike }
    }

    suspend fun acceptLike(userId: Int, swipeId: Int): MatchDto {
        val matchId = db.query {
            val (swipe, listing) = ownedPendingSwipe(userId, swipeId)
            createMatch(swipeId, listing, swipe[Swipes.userId], swipe[Swipes.direction] == SwipeDirection.UP.name)
        }
        return match(userId, matchId)
    }

    suspend fun declineLike(userId: Int, swipeId: Int) = db.query {
        ownedPendingSwipe(userId, swipeId)
        Swipes.update({ Swipes.id eq swipeId }) { it[status] = DECLINED }
        Unit
    }

    // --- Matchek és chat ---

    suspend fun matches(userId: Int): List<MatchDto> = db.query {
        Matches.selectAll()
            .where { (Matches.buyerId eq userId) or (Matches.sellerId eq userId) }
            .map { it.toMatchDto(userId) }
            .sortedByDescending { it.lastMessage?.createdAt ?: it.createdAt }
    }

    suspend fun match(userId: Int, matchId: Int): MatchDto = db.query { matchRow(userId, matchId).toMatchDto(userId) }

    suspend fun unmatch(userId: Int, matchId: Int) = db.query {
        matchRow(userId, matchId)
        Matches.deleteWhere { Matches.id eq matchId }
        Unit
    }

    suspend fun messages(userId: Int, matchId: Int, after: Int): MessagesResponse = db.query {
        matchRow(userId, matchId)
        val list = Messages.selectAll()
            .where { (Messages.matchId eq matchId) and (Messages.id greater after) }
            .orderBy(Messages.id to SortOrder.ASC)
            .limit(500)
            .map { it.toMessage(userId) }
        MessagesResponse(list, bots.isTyping(matchId))
    }

    suspend fun sendMessage(userId: Int, matchId: Int, text: String): MessageDto {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) badRequest("Üres üzenetet nem küldhetsz.")
        if (trimmed.length > 2000) badRequest("Az üzenet legfeljebb 2000 karakter lehet.")
        val (message, otherIsBot) = db.query {
            val match = matchRow(userId, matchId)
            val otherId = if (match[Matches.buyerId] == userId) match[Matches.sellerId] else match[Matches.buyerId]
            val id = insertMessage(matchId, userId, trimmed)
            markReadInternal(match, userId, id)
            val bot = Users.select(Users.isBot).where { Users.id eq otherId }.single()[Users.isBot]
            MessageDto(id, matchId, fromMe = true, text = trimmed, createdAt = clock()) to bot
        }
        if (otherIsBot) bots.schedule(matchId)
        return message
    }

    suspend fun markRead(userId: Int, matchId: Int, lastMessageId: Int) = db.query {
        markReadInternal(matchRow(userId, matchId), userId, lastMessageId)
    }

    /** Egy demó eladó válasza a vevő utolsó üzenetére. */
    suspend fun botReply(matchId: Int) = db.query {
        val match = Matches.selectAll().where { Matches.id eq matchId }.singleOrNull() ?: return@query
        val listing = findListing(match[Matches.listingId]) ?: return@query
        val sellerId = match[Matches.sellerId]
        val thread = Messages.selectAll().where { Messages.matchId eq matchId }.orderBy(Messages.id to SortOrder.ASC).toList()
        val lastFromBuyer = thread.lastOrNull { it[Messages.senderId] != sellerId }?.get(Messages.text).orEmpty()
        val sellerCount = thread.count { it[Messages.senderId] == sellerId }
        insertMessage(matchId, sellerId, ChatBot.reply(listing, lastFromBuyer, sellerCount, random))
        Unit
    }

    // --- Saját hirdetések ---

    suspend fun myListings(userId: Int): List<ListingDto> = db.query {
        listingQuery().where { Listings.sellerId eq userId }
            .orderBy(Listings.createdAt to SortOrder.DESC)
            .map { it.toListing().toDto() }
    }

    suspend fun createListing(userId: Int, req: ListingRequest): ListingDto {
        validate(req)
        val id = db.query {
            Listings.insert {
                it[sellerId] = userId
                it.fill(req)
                it[createdAt] = clock()
            }[Listings.id]
        }
        return db.query { findListing(id)!!.toDto() }
    }

    suspend fun updateListing(userId: Int, listingId: Int, req: ListingRequest): ListingDto {
        validate(req)
        return db.query {
            ownedListing(userId, listingId)
            Listings.update({ Listings.id eq listingId }) { it.fill(req) }
            findListing(listingId)!!.toDto()
        }
    }

    suspend fun deleteListing(userId: Int, listingId: Int) {
        val listing = db.query {
            ownedListing(userId, listingId).also { Listings.deleteWhere { Listings.id eq listingId } }
        }
        localPhotoFile(listing.photoUrl)?.delete()
    }

    suspend fun setListingPhoto(userId: Int, listingId: Int, bytes: ByteArray, extension: String): ListingDto {
        if (bytes.isEmpty()) badRequest("Üres fájl.")
        if (bytes.size > MAX_PHOTO_BYTES) badRequest("A fotó legfeljebb 8 MB lehet.")
        val old = db.query { ownedListing(userId, listingId) }
        config.uploadDir.mkdirs()
        val name = "${UUID.randomUUID()}.$extension"
        File(config.uploadDir, name).writeBytes(bytes)
        localPhotoFile(old.photoUrl)?.delete()
        return db.query {
            Listings.update({ Listings.id eq listingId }) {
                it[photoUrl] = "/uploads/$name"
                it[photoCredit] = null
            }
            findListing(listingId)!!.toDto()
        }
    }

    // --- Segédfüggvények (tranzakción belül hívandók) ---

    private fun listingQuery() =
        Listings.join(Users, JoinType.INNER, Listings.sellerId, Users.id).selectAll()

    private fun findListing(id: Int): ListingRow? =
        listingQuery().where { Listings.id eq id }.singleOrNull()?.toListing()

    private fun ownedListing(userId: Int, listingId: Int): ListingRow {
        val listing = findListing(listingId) ?: notFound("A hirdetés nem található.")
        if (listing.sellerId != userId) forbidden("Ez nem a te hirdetésed.")
        return listing
    }

    private fun ownedPendingSwipe(userId: Int, swipeId: Int): Pair<ResultRow, ListingRow> {
        val swipe = Swipes.selectAll().where { Swipes.id eq swipeId }.singleOrNull() ?: notFound("A kedvelés nem található.")
        val listing = findListing(swipe[Swipes.listingId]) ?: notFound("A hirdetés nem található.")
        if (listing.sellerId != userId) notFound("A kedvelés nem található.")
        if (swipe[Swipes.status] != PENDING) conflict("Erről a kedvelésről már döntöttél.")
        return swipe to listing
    }

    private fun matchRow(userId: Int, matchId: Int): ResultRow {
        val row = Matches.selectAll().where { Matches.id eq matchId }.singleOrNull() ?: notFound("A match nem található.")
        if (row[Matches.buyerId] != userId && row[Matches.sellerId] != userId) notFound("A match nem található.")
        return row
    }

    private fun createMatch(swipeId: Int, listing: ListingRow, buyerId: Int, superLike: Boolean): Int {
        Swipes.update({ Swipes.id eq swipeId }) { it[status] = ACCEPTED }
        val matchId = Matches.insert {
            it[Matches.swipeId] = swipeId
            it[listingId] = listing.id
            it[Matches.buyerId] = buyerId
            it[sellerId] = listing.sellerId
            it[Matches.superLike] = superLike
            it[createdAt] = clock()
        }[Matches.id]
        if (listing.opener.isNotBlank()) insertMessage(matchId, listing.sellerId, listing.opener)
        return matchId
    }

    private fun insertMessage(matchId: Int, senderId: Int, text: String): Int = Messages.insert {
        it[Messages.matchId] = matchId
        it[Messages.senderId] = senderId
        it[Messages.text] = text
        it[createdAt] = clock()
    }[Messages.id]

    private fun markReadInternal(match: ResultRow, userId: Int, lastMessageId: Int) {
        val column = if (match[Matches.buyerId] == userId) Matches.buyerLastRead else Matches.sellerLastRead
        if (lastMessageId > match[column]) {
            Matches.update({ Matches.id eq match[Matches.id] }) { it[column] = lastMessageId }
        }
    }

    private fun ResultRow.toMatchDto(userId: Int): MatchDto {
        val matchId = this[Matches.id]
        val iAmBuyer = this[Matches.buyerId] == userId
        val otherId = if (iAmBuyer) this[Matches.sellerId] else this[Matches.buyerId]
        val lastRead = if (iAmBuyer) this[Matches.buyerLastRead] else this[Matches.sellerLastRead]
        val last = Messages.selectAll().where { Messages.matchId eq matchId }
            .orderBy(Messages.id to SortOrder.DESC).limit(1).singleOrNull()
        val unread = Messages.selectAll()
            .where { (Messages.matchId eq matchId) and (Messages.senderId neq userId) and (Messages.id greater lastRead) }
            .count().toInt()
        return MatchDto(
            id = matchId,
            listing = findListing(this[Matches.listingId])!!.toDto(),
            other = Users.selectAll().where { Users.id eq otherId }.single().toPublicUser(),
            role = if (iAmBuyer) Role.BUYER else Role.SELLER,
            superLike = this[Matches.superLike],
            lastMessage = last?.toMessage(userId),
            unread = unread,
            otherTyping = bots.isTyping(matchId),
            createdAt = this[Matches.createdAt],
        )
    }

    private fun localPhotoFile(url: String?): File? =
        url?.takeIf { it.startsWith("/uploads/") }?.let { File(config.uploadDir, it.removePrefix("/uploads/")) }

    private fun validate(req: ListingRequest) {
        if (req.make.isBlank() || req.make.length > 40) badRequest("Add meg a márkát.")
        if (req.model.isBlank() || req.model.length > 80) badRequest("Add meg a modellt.")
        if (req.year !in 1900..Year.now().value + 1) badRequest("Érvénytelen évjárat.")
        if (req.priceHuf !in 0..1_000_000_000) badRequest("Érvénytelen ár.")
        if (req.km !in 0..3_000_000) badRequest("Érvénytelen futásteljesítmény.")
        if (req.horsepower !in 1..2_000) badRequest("Érvénytelen teljesítmény.")
        if (req.city.isBlank() || req.city.length > 60) badRequest("Add meg a várost.")
        if (req.bio.length > 1000) badRequest("A leírás legfeljebb 1000 karakter lehet.")
        if (req.opener.length > 300) badRequest("A nyitóüzenet legfeljebb 300 karakter lehet.")
    }

    companion object {
        const val MAX_PHOTO_BYTES = 8 * 1024 * 1024
    }
}

private fun org.jetbrains.exposed.v1.core.statements.UpdateBuilder<*>.fill(req: ListingRequest) {
    this[Listings.make] = req.make.trim()
    this[Listings.model] = req.model.trim()
    this[Listings.year] = req.year
    this[Listings.priceHuf] = req.priceHuf
    this[Listings.km] = req.km
    this[Listings.horsepower] = req.horsepower
    this[Listings.fuel] = req.fuel.name
    this[Listings.body] = req.body.name
    this[Listings.city] = req.city.trim()
    this[Listings.bio] = req.bio.trim()
    this[Listings.opener] = req.opener.trim()
    this[Listings.active] = req.active
}

private fun ResultRow.toListing() = ListingRow(
    id = this[Listings.id],
    sellerId = this[Listings.sellerId],
    sellerName = this[Users.name],
    sellerIsBot = this[Users.isBot],
    make = this[Listings.make],
    model = this[Listings.model],
    year = this[Listings.year],
    priceHuf = this[Listings.priceHuf],
    km = this[Listings.km],
    horsepower = this[Listings.horsepower],
    fuel = Fuel.valueOf(this[Listings.fuel]),
    body = BodyType.valueOf(this[Listings.body]),
    city = this[Listings.city],
    distanceKm = this[Listings.distanceKm],
    bio = this[Listings.bio],
    opener = this[Listings.opener],
    pickiness = this[Listings.pickiness],
    photoUrl = this[Listings.photoUrl],
    photoCredit = this[Listings.photoCredit],
    active = this[Listings.active],
)

private fun ResultRow.toPublicUser() = UserPublicDto(
    id = this[Users.id],
    name = this[Users.name],
    city = this[Users.city],
    bio = this[Users.bio],
    currentCar = this[Users.currentCar],
)

private fun ResultRow.toMessage(userId: Int) = MessageDto(
    id = this[Messages.id],
    matchId = this[Messages.matchId],
    fromMe = this[Messages.senderId] == userId,
    text = this[Messages.text],
    createdAt = this[Messages.createdAt],
)
