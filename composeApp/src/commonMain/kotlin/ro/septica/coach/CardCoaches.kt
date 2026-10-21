package ro.septica.coach

import kotlin.math.abs
import ro.septica.blackjack.handValue
import ro.septica.cards.StandardCard
import ro.septica.cards.StandardRank
import ro.septica.game.Player
import ro.septica.game.Suit
import ro.septica.macao.*
import ro.septica.tabinet.*
import ro.septica.tricks.*
import ro.septica.sixtysix.*

fun blackjackAdvice(hand: List<StandardCard>, dealerUp: StandardCard): Advice {
    val total = handValue(hand)
    val lowTotal = hand.sumOf { if (it.rank == StandardRank.ACE) 1 else minOf(10, it.rank.value) }
    val soft = total != lowTotal
    val dealer = if (dealerUp.rank == StandardRank.ACE) 11 else minOf(10, dealerUp.rank.value)
    val stand = when {
        soft -> total >= 19 || total == 18 && dealer in 2..8
        total >= 17 -> true
        total in 13..16 -> dealer in 2..6
        total == 12 -> dealer in 4..6
        else -> false
    }
    return Advice(
        if (stand) "Oprește-te la $total" else "Cere o carte la $total",
        "Mână ${if (soft) "soft (asul valorează 11)" else "hard"}; dealerul arată ${dealerUp.accessibilityLabel}. " +
            "Strategie de bază fără dublare sau împărțire, cu dealerul oprit la soft 17. Nu citesc cartea acoperită.",
        action = if (stand) CoachAction.Stand else CoachAction.Hit,
    )
}

fun macaoAdvice(hand: List<MacaoCard>, discard: MacaoCard, activeSuit: Suit, pendingDraw: Int): Advice {
    fun wild(card: MacaoCard) = card.rank in setOf(MacaoRank.ACE, MacaoRank.BLACK_JOKER, MacaoRank.RED_JOKER)
    fun penalty(card: MacaoCard) = when (card.rank) {
        MacaoRank.TWO -> 2
        MacaoRank.THREE -> 3
        MacaoRank.BLACK_JOKER -> 5
        MacaoRank.RED_JOKER -> 10
        else -> 0
    }
    val legal = hand.filter {
        if (pendingDraw > 0) penalty(it) > 0
        else wild(it) || it.suit == activeSuit || it.rank == discard.rank
    }
    val card = legal.maxByOrNull {
        (if (it.rank == MacaoRank.FOUR) 30 else 0) + penalty(it) * 3 +
            hand.count { other -> other != it && other.suit == it.suit } * 4 -
            if (wild(it) && pendingDraw == 0) 12 else 0
    } ?: return Advice("Trage ${maxOf(1, pendingDraw)} cărți", "Nu ai o carte legală pentru culoarea, rangul sau atacul curent.", action = CoachAction.Draw)
    val suit = if (wild(card)) Suit.entries.maxBy { s -> hand.count { it != card && it.suit == s } } else null
    return Advice("Joacă ${card.accessibilityLabel}" + (suit?.let { " · ${it.romanianName}" } ?: ""),
        when {
            hand.size == 1 -> "Ultima carte încheie imediat jocul, inclusiv un atac."
            pendingDraw > 0 -> "Transferă penalizarea publică de $pendingDraw cărți printr-un atac legal."
            card.rank == MacaoRank.FOUR -> "Patru sare rândul adversarului; păstrezi inițiativa."
            else -> "Compar cărțile legale după atac și numărul de continuări în aceeași culoare; păstrez jokerii când există alternative."
        } + (suit?.let { " Culoarea aleasă lasă cele mai multe continuări în mâna ta." } ?: ""),
        card.id, CoachAction.Play(card.id, suit))
}

fun tabinetAdvice(hand: List<TabinetCard>, table: List<TabinetCard>, publicCapturedCount: Int): Advice {
    if (hand.isEmpty()) return unavailableAdvice()
    val engine = TabinetEngine()
    val publicState = TabinetState(hand, emptyList(), emptyList(), table, Player.HUMAN)
    val finalMove = hand.size == 1 && publicCapturedCount + table.size == 51
    val moves = hand.map { card ->
        val ids = engine.suggestCapture(publicState, Player.HUMAN, card.id)
        val captured = table.filter { it.id in ids }
        val sweep = captured.isNotEmpty() && captured.size == table.size && !finalMove
        Triple(card, ids, if (ids.isEmpty()) -card.points * 53 - card.rank.values.first()
        else (card.points + captured.sumOf { it.points } + if (sweep) 1 else 0) * 53 + ids.size + 1)
    }
    val (card, ids) = moves.maxBy { it.third }
    val names = table.filter { it.id in ids }.joinToString { it.accessibilityLabel }
    return Advice("Joacă ${card.accessibilityLabel}",
        if (ids.isEmpty()) "Nicio captură disponibilă în mâna ta. Pune această carte pe masă, păstrând cărțile cu puncte."
        else "Capturează: $names. Compar toate cărțile tale după puncte, tablă (+1, exceptând ultima mutare) și numărul capturat.",
        card.id, CoachAction.Play(card.id, capturedIds = ids))
}

data class TrickView(
    val hand: List<StandardCard>, val table: List<PlayedCard>, val trump: Suit?,
    val phase: TrickPhase, val active: Boolean, val legalBids: List<Int>,
    val bid: Int?, val tricks: Int, val trickIndex: Int, val contract: Contract?,
    val lanes: Set<StandardCard>,
) {
    companion object {
        fun from(state: TrickState) = TrickView(state.hands[0], state.table, state.trump,
            state.phase, state.turn == 0, ro.septica.tricks.legalBids(state),
            state.bids[0], state.tricks[0], state.tricks.sum(), state.contract, state.lanes)
    }
}

fun trickAdvice(view: TrickView): Advice {
    if (!view.active) return unavailableAdvice()
    if (view.phase == TrickPhase.BIDDING) {
        val estimate = view.hand.sumOf {
            when {
                it.rank == StandardRank.ACE -> 0.85
                it.suit == view.trump && it.rank.value >= 11 -> 0.7
                it.rank == StandardRank.KING -> 0.35
                else -> 0.0
            }
        }
        val bid = view.legalBids.minByOrNull { abs(it - estimate) } ?: return unavailableAdvice()
        return Advice("Licitează $bid", "Estimez levatele din ași, regi și atuuri mari, apoi aleg cea mai apropiată licitație permisă. Ultima licitație respectă totalul interzis.", action = CoachAction.Bid(bid))
    }
    if (view.phase != TrickPhase.PLAYING) return unavailableAdvice()
    val legal = if (view.contract == Contract.RENTZ) view.hand.filter { legalPlacement(it, view.lanes) }
    else legalTrickCards(view.hand, view.table, view.trump)
    if (legal.isEmpty()) return if (view.contract == Contract.RENTZ && view.hand.isNotEmpty())
        Advice("Pas", "Nu ai nicio carte care continuă legal șirurile publice.", action = CoachAction.Pass)
    else unavailableAdvice()
    if (view.contract == Contract.RENTZ) {
        val card = legal.maxBy { candidate ->
            (view.hand - candidate).count { legalPlacement(it, view.lanes + candidate) }
        }
        return Advice("Pune ${card.accessibilityLabel}", "Dintre plasările legale, aceasta deschide cele mai multe continuări pentru cărțile tale.", card.id, CoachAction.Play(card.id))
    }
    fun winning(card: StandardCard): Boolean {
        val cards = view.table.map { it.card } + card
        val suit = if (cards.any { it.suit == view.trump }) view.trump else cards.first().suit
        return card == cards.filter { it.suit == suit }.maxBy { it.rank.value }
    }
    val seekWin = view.contract == Contract.WHIST || view.contract == null && view.tricks < (view.bid ?: 0)
    fun penalty(card: StandardCard) = view.contract?.let {
        -contractScore(it, listOf(card), view.trickIndex)
    } ?: 0
    val desired = legal.filter { winning(it) == seekWin }.ifEmpty { legal }
    val card = if (seekWin) desired.minBy { it.rank.value + if (it.suit == view.trump) 20 else 0 }
    else desired.maxBy { (if (!winning(it)) penalty(it) * 10 + it.rank.value else -it.rank.value) }
    return Advice("Joacă ${card.accessibilityLabel}",
        if (seekWin) "Ai nevoie de levate: aleg cea mai ieftină carte care conduce momentan, respectând culoarea și atuul obligatoriu. Jucătorii rămași pot depăși cartea."
        else "Evit câștigarea levatei pentru ${view.contract?.title ?: "licitația deja atinsă"}; când pot pierde, descarc ranguri mari și penalizări ale contractului. Rezultatul rămâne incert.",
        card.id, CoachAction.Play(card.id))
}

data class SixtySixView(
    val hand: List<StandardCard>, val lead: StandardCard?, val trump: Suit,
    val stockOpen: Boolean, val active: Boolean, val marriages: Set<Suit>,
    val points: Int, val tricks: Int,
    val faceUpTrump: StandardCard? = null, val stockCount: Int = 0,
) {
    companion object {
        fun from(state: SixtySixState) = SixtySixView(state.human.hand, state.table.firstOrNull()?.card,
            state.trump, state.stockOpen, state.turn == Player.HUMAN, state.human.marriages,
            state.human.points, state.human.tricks, state.faceUpTrump, state.stock.size)
    }
}

fun sixtySixAdvice(view: SixtySixView): Advice {
    if (!view.active || view.hand.isEmpty()) return unavailableAdvice()
    val lead = view.lead
    val legal = if (lead == null || view.stockOpen) view.hand else {
        val same = view.hand.filter { it.suit == lead.suit }
        if (same.isNotEmpty()) same.filter { SixtySixEngine.beats(it, lead, view.trump) }.ifEmpty { same }
        else view.hand.filter { it.suit == view.trump }.ifEmpty { view.hand }
    }
    val marriages = if (lead != null) emptyList() else legal.filter { card ->
        card.rank in setOf(StandardRank.KING, StandardRank.QUEEN) && card.suit !in view.marriages &&
            view.hand.any { it.suit == card.suit && it.rank == if (card.rank == StandardRank.KING) StandardRank.QUEEN else StandardRank.KING }
    }
    val marriage = marriages.maxByOrNull { if (it.suit == view.trump) 40 else 20 }
    if (marriage == null && lead == null && view.stockOpen && view.stockCount >= 2 &&
        view.faceUpTrump != null && view.faceUpTrump.sixtySixPoints >= 3 &&
        view.hand.any { it.suit == view.trump && it.rank == StandardRank.NINE }) {
        return Advice("Schimbă nouarul de atu", "Ia ${view.faceUpTrump.accessibilityLabel}, cartea întoarsă public. Schimbul îmbunătățește rangul și punctele din mână fără a consuma rândul; apoi cere o analiză nouă.",
            action = CoachAction.ExchangeTrump)
    }
    val winners = if (lead == null) emptyList() else legal.filter { SixtySixEngine.beats(it, lead, view.trump) }
    val card = marriage ?: winners.ifEmpty { legal }.minBy { it.sixtySixPoints + if (it.suit == view.trump) 12 else 0 }
    val bonus = if (card.suit == view.trump) 40 else 20
    return Advice("Joacă ${card.accessibilityLabel}" + if (marriage != null) " și anunță căsătoria" else "",
        if (marriage != null) "+$bonus puncte ${if (view.tricks == 0) "în așteptare până câștigi o levată" else "imediat (scor ${view.points + bonus})"}. Bifează manual anunțul înainte de joc."
        else if (winners.isNotEmpty()) "Câștigă cu o carte ieftină; păstrează atuuri și puncte. Respectă obligația de culoare, depășire și atu când pachetul este închis."
        else "Păstrează cărțile cu puncte și atuurile: nu există câștigător sigur pe masa vizibilă.",
        card.id, CoachAction.Play(card.id, marriage = marriage != null))
}
