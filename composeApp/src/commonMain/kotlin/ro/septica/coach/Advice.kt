package ro.septica.coach

import ro.septica.game.Suit
import ro.septica.solitaire.SolitaireSource
import ro.septica.solitaire.SolitaireTarget

data class Advice(
    val title: String,
    val message: String,
    val recommendedCardId: String? = null,
    val action: CoachAction? = null,
)

sealed interface CoachAction {
    data class Play(val cardId: String, val suit: Suit? = null, val capturedIds: List<String> = emptyList(), val marriage: Boolean = false) : CoachAction
    data class Bid(val value: Int) : CoachAction
    data class Move(val source: SolitaireSource, val target: SolitaireTarget) : CoachAction
    data object Pass : CoachAction
    data object Draw : CoachAction
    data object Recycle : CoachAction
    data object Hit : CoachAction
    data object Stand : CoachAction
    data object ExchangeTrump : CoachAction
}

fun unavailableAdvice() = Advice("Așteaptă rândul tău", "Analiza este disponibilă numai cât timp poți decide o mutare.")

fun warAdvice() = Advice("Întoarce următoarea carte", "La Război nu alegi cartea sau ordinea pachetului. Nu există control strategic asupra duelului; cărțile ascunse nu pot fi prezise.")

fun oldMaidAdvice() = Advice("Alege o carte acoperită", "Pozițiile nu dezvăluie identitatea cărților. Nicio alegere ascunsă nu poate fi prezisă ca fiind mai bună; perechile sunt eliminate automat. Nu recomand o poziție arbitrară drept strategie.")
