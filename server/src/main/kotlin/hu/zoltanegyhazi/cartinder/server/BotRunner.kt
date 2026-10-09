package hu.zoltanegyhazi.cartinder.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/** A demó eladók késleltetett, "gépelős" válaszait ütemezi. */
class BotRunner(
    private val scope: CoroutineScope,
    private val delays: LongRange,
    private val random: Random,
) {
    private val log = LoggerFactory.getLogger(BotRunner::class.java)
    private val pending = ConcurrentHashMap.newKeySet<Int>()
    private val typing = ConcurrentHashMap.newKeySet<Int>()

    lateinit var reply: suspend (matchId: Int) -> Unit

    fun isTyping(matchId: Int) = matchId in typing

    fun schedule(matchId: Int) {
        if (!pending.add(matchId)) return
        scope.launch {
            try {
                delay(randomDelay() / 2)
                typing += matchId
                delay(randomDelay())
                reply(matchId)
            } catch (e: Exception) {
                log.warn("Bot reply failed for match $matchId", e)
            } finally {
                typing -= matchId
                pending -= matchId
            }
        }
    }

    private fun randomDelay() = if (delays.first >= delays.last) delays.first else random.nextLong(delays.first, delays.last)
}
