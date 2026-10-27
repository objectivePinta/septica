package ro.septica.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ro.septica.game.Card
import ro.septica.game.GamePhase
import ro.septica.game.GameState
import ro.septica.game.PlayedCard
import ro.septica.game.SepticaPlayer as Player

@Composable
internal fun GameScreen(
    state: GameState,
    statusText: String,
    selectedCardId: String?,
    legalCardIds: Set<String>,
    humanCanPass: Boolean,
    onCardSelected: (String) -> Unit,
    onPlaySelected: () -> Unit,
    onPass: () -> Unit,
    onNewGame: () -> Unit,
    onShowRules: () -> Unit,
    onMenu: () -> Unit,
    coachContent: @Composable () -> Unit = {},
) {
    val humanTurn = state.phase == GamePhase.PLAYING && state.turn == Player.HUMAN

    TableBackdrop {
        BoxWithConstraints(
            Modifier.widthIn(max = 680.dp).fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val contentHeight = maxHeight.coerceAtLeast(680.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .height(contentHeight)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                GameHeader(
                    onNewGame = onNewGame,
                    onShowRules = onShowRules,
                    onMenu = onMenu,
                )
                coachContent()
                Spacer(Modifier.height(10.dp))
                ScorePanel(state)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.players.filter { it != Player.HUMAN }.forEach { player ->
                        OpponentHand(
                            cardCount = state.handFor(player).size,
                            label = player.label,
                            active = state.turn == player,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                GameTable(
                    state = state,
                    statusText = statusText,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.height(8.dp))
                HumanHand(
                    cards = state.humanHand,
                    selectedCardId = selectedCardId,
                    legalCardIds = legalCardIds,
                    humanTurn = humanTurn,
                    continuationOnly = humanCanPass,
                    eightsCut = state.players.size == 3,
                    onCardSelected = onCardSelected,
                )
                Spacer(Modifier.height(8.dp))
                ActionArea(
                    state = state,
                    selectedCardId = selectedCardId,
                    humanTurn = humanTurn,
                    humanCanPass = humanCanPass,
                    hasLegalContinuation = legalCardIds.isNotEmpty(),
                    onPlaySelected = onPlaySelected,
                    onPass = onPass,
                    onNewGame = onNewGame,
                )
            }
        }
    }
}

@Composable
private fun GameHeader(
    onNewGame: () -> Unit,
    onShowRules: () -> Unit,
    onMenu: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ȘEPTICĂ",
                style = MaterialTheme.typography.displaySmall,
                color = SepticaColors.Cream,
            )
            Text(
                text = "‹ Înapoi la jocuri",
                style = MaterialTheme.typography.bodyMedium,
                color = SepticaColors.Muted,
                modifier = Modifier.clickable(role = Role.Button, onClick = onMenu)
                    .padding(vertical = 8.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        HeaderButton(
            text = "?",
            description = "Deschide regulile",
            onClick = onShowRules,
        )
        Spacer(Modifier.width(8.dp))
        HeaderButton(
            text = "↻",
            description = "Începe un joc nou",
            onClick = onNewGame,
        )
    }
}

@Composable
private fun HeaderButton(
    text: String,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SepticaColors.Panel.copy(alpha = 0.92f))
            .border(1.dp, SepticaColors.Gold.copy(alpha = 0.5f), CircleShape)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = SepticaColors.PaleGold,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
    }
}

@Composable
private fun ScorePanel(state: GameState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(20.dp),
        color = SepticaColors.Panel.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.players.forEach { player ->
                ScoreValue(
                    label = player.label.uppercase(),
                    score = state.scoreFor(player),
                    alignment = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
            DeckCounter(state.deck.size)
        }
    }
}

@Composable
private fun ScoreValue(
    label: String,
    score: Int,
    alignment: TextAlign,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = SepticaColors.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            textAlign = alignment,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = score.toString(),
            color = SepticaColors.Cream,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            textAlign = alignment,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DeckCounter(cardsLeft: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(82.dp),
    ) {
        Box(
            modifier = Modifier
                .width(31.dp)
                .height(39.dp)
                .shadow(4.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(SepticaColors.Burgundy)
                .border(1.dp, SepticaColors.Gold, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cardsLeft.toString(),
                color = SepticaColors.Cream,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
            )
        }
        Text(
            text = "ÎN PACHET",
            color = SepticaColors.Muted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun OpponentHand(cardCount: Int, label: String, active: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase() + if (active) " · JOACĂ" else "",
            color = if (active) SepticaColors.PaleGold else SepticaColors.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "$cardCount ${if (cardCount == 1) "carte" else "cărți"}",
            color = SepticaColors.Muted,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(5.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy((-8).dp),
            modifier = Modifier.height(62.dp).horizontalScroll(rememberScrollState()),
        ) {
            repeat(cardCount) {
                CardBack(
                    modifier = Modifier
                        .width(28.dp)
                        .aspectRatio(0.70f)
                        .graphicsLayer { rotationZ = (it - (cardCount - 1) / 2f) * 2f },
                )
            }
        }
    }
}

@Composable
private fun GameTable(
    state: GameState,
    statusText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.13f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StatusPill(
                text = statusText,
                highlighted = state.phase != GamePhase.PLAYING,
            )
            Spacer(Modifier.height(10.dp))

            if (state.table.isEmpty()) {
                EmptyTable()
            } else {
                TableCards(state.table)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "CARTE DE PORNIRE: ${state.targetRank?.symbol}",
                    color = SepticaColors.PaleGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    highlighted: Boolean,
) {
    Surface(
        shape = CircleShape,
        color = if (highlighted) {
            SepticaColors.Gold.copy(alpha = 0.18f)
        } else {
            SepticaColors.Panel.copy(alpha = 0.86f)
        },
        border = BorderStroke(
            1.dp,
            if (highlighted) {
                SepticaColors.Gold.copy(alpha = 0.55f)
            } else {
                Color.White.copy(alpha = 0.08f)
            },
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = if (highlighted) SepticaColors.PaleGold else SepticaColors.SoftWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyTable() {
    Box(
        modifier = Modifier
            .width(72.dp)
            .aspectRatio(0.70f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                SepticaColors.Gold.copy(alpha = 0.24f),
                RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "7",
            color = SepticaColors.Gold.copy(alpha = 0.22f),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
        )
    }
}

@Composable
private fun TableCards(cards: List<PlayedCard>) {
    val cardWidth = when (cards.size) {
        in 1..4 -> 64.dp
        5 -> 54.dp
        6 -> 47.dp
        else -> 40.dp
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cards.forEachIndexed { index, played ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(played.player.label, color = SepticaColors.Muted, fontSize = 9.sp)
                PlayingCard(
                    card = played.card,
                    selected = false,
                    muted = false,
                    compact = true,
                    onClick = null,
                    modifier = Modifier
                        .width(cardWidth)
                        .aspectRatio(0.70f)
                        .graphicsLayer {
                            rotationZ = if (index % 2 == 0) -1.5f else 1.5f
                        },
                )
            }
        }
    }
}

@Composable
private fun HumanHand(
    cards: List<Card>,
    selectedCardId: String?,
    legalCardIds: Set<String>,
    humanTurn: Boolean,
    continuationOnly: Boolean,
    eightsCut: Boolean,
    onCardSelected: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "MÂNA TA",
                color = SepticaColors.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.weight(1f))
            if (humanTurn && continuationOnly) {
                Text(
                    text = if (eightsCut) "cartea de pornire, 7 sau 8" else "doar cartea de pornire sau 7",
                    color = SepticaColors.PaleGold,
                    fontSize = 10.sp,
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(121.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val gap = 7.dp
            val availablePerCard =
                if (cards.isEmpty()) 76.dp else (maxWidth - gap * (cards.size - 1)) / cards.size
            val cardWidth = availablePerCard.coerceIn(58.dp, 80.dp)

            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Bottom,
            ) {
                cards.forEach { card ->
                    val legal = card.id in legalCardIds
                    PlayingCard(
                        card = card,
                        selected = card.id == selectedCardId,
                        muted = humanTurn && !legal,
                        compact = false,
                        onClick = if (humanTurn && legal) {
                            { onCardSelected(card.id) }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .width(cardWidth)
                            .aspectRatio(0.70f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayingCard(
    card: Card,
    selected: Boolean,
    muted: Boolean,
    compact: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    CardFace(
        rankSymbol = card.rank.symbol,
        suitSymbol = card.suit.symbol,
        isRed = card.suit.isRed,
        accessibilityLabel = card.accessibilityLabel,
        selected = selected,
        muted = muted,
        compact = compact,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ActionArea(
    state: GameState,
    selectedCardId: String?,
    humanTurn: Boolean,
    humanCanPass: Boolean,
    hasLegalContinuation: Boolean,
    onPlaySelected: () -> Unit,
    onPass: () -> Unit,
    onNewGame: () -> Unit,
) {
    when {
        state.phase == GamePhase.FINISHED -> {
            Button(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = goldButtonColors(),
            ) {
                Text("JOACĂ DIN NOU")
            }
        }

        humanTurn && humanCanPass -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onPass,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SepticaColors.Gold.copy(alpha = 0.7f)),
                ) {
                    Text(
                        text = if (hasLegalContinuation) "MĂ OPRESC" else "NU POT TĂIA",
                        color = SepticaColors.PaleGold,
                        fontSize = 12.sp,
                    )
                }
                Button(
                    onClick = onPlaySelected,
                    enabled = selectedCardId != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = goldButtonColors(),
                ) {
                    Text("CONTINUĂ")
                }
            }
        }

        humanTurn -> {
            Button(
                onClick = onPlaySelected,
                enabled = selectedCardId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = goldButtonColors(),
            ) {
                Text(if (selectedCardId == null) "ALEGE O CARTE" else "JOACĂ ACEASTĂ CARTE")
            }
        }

        else -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = goldButtonColors(),
            ) {
                Text(
                    if (state.phase == GamePhase.TRICK_REVEAL) {
                        "SE STRÂNGE MÂNA…"
                    } else {
                        "${state.turn?.label?.uppercase()} JOACĂ…"
                    },
                )
            }
        }
    }
}

@Composable
private fun goldButtonColors() = ButtonDefaults.buttonColors(
    containerColor = SepticaColors.Gold,
    contentColor = SepticaColors.Ink,
    disabledContainerColor = Color.White.copy(alpha = 0.10f),
    disabledContentColor = SepticaColors.Muted.copy(alpha = 0.65f),
)

@Composable
internal fun RulesDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f)
                .widthIn(max = 520.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp),
            color = SepticaColors.Panel,
            border = BorderStroke(1.dp, SepticaColors.Gold.copy(alpha = 0.35f)),
            shadowElevation = 20.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            text = "Cum se joacă",
                            style = MaterialTheme.typography.headlineSmall,
                            color = SepticaColors.Cream,
                        )
                        Text(
                            text = "2–4 jucători · fiecare pentru el",
                            color = SepticaColors.Muted,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("ÎNCHIDE", color = SepticaColors.PaleGold)
                    }
                }
                Spacer(Modifier.height(18.dp))

                RuleItem(
                    number = "1",
                    title = "Pachetul",
                    body = "Alegi 2, 3 sau 4 jucători în total: tu și 1–3 adversari automați (Ion, Costică, Vasile), fără echipe. În 2 sau 4 se joacă cu 32 de cărți, de la 7 la As. În 3 se scot 8 de cupă și 8 de caro: rămân 30 de cărți. Fiecare primește patru cărți.",
                )
                RuleItem(
                    number = "2",
                    title = "Tăierea",
                    body = "Poți pune orice carte când răspunzi. Tai cu un 7 sau cu o carte de aceeași valoare ca prima carte a mâinii. Numai în 3 jucători, cei doi optari rămași taie orice valoare, exact ca șeptarii. Culoarea nu contează.",
                )
                RuleItem(
                    number = "3",
                    title = "Continuarea",
                    body = "Toți ceilalți răspund pe rând. Dacă ultimul care a tăiat nu este cel care a deschis, acesta poate continua cu un 7, un 8 (numai în 3) sau cu valoarea de pornire, ori poate ceda mâna. La continuare, ceilalți răspund din nou.",
                )
                RuleItem(
                    number = "4",
                    title = "Cine ia mâna",
                    body = "După răspunsurile tuturor, dacă cel care a deschis are ultima tăiere sau cedează, ultimul care a tăiat ia masa. El deschide mâna următoare; dacă nu mai are cărți, deschide următorul cu cărți.",
                )
                RuleItem(
                    number = "5",
                    title = "Punctajul",
                    body = "Fiecare As și fiecare 10 valorează un punct: opt puncte în total. Câștigă jucătorul cu cele mai multe puncte; un maxim comun înseamnă egalitate. La 8 puncte apare „l-ai batut la fundu gol” sau „ai fost batut la fundu gol”.",
                )
                RuleItem(
                    number = "6",
                    title = "Cărți noi",
                    body = "După fiecare mână se împarte câte o carte fiecăruia, pe rând, începând cu câștigătorul. Se repetă până când fiecare are patru cărți sau se termină pachetul; cei cu patru cărți nu mai primesc. Când pachetul se termină, jucătorii fără cărți sunt săriți; cel care a deschis poate ceda dacă nu mai poate continua.",
                )
            }
        }
    }
}

@Composable
private fun RuleItem(
    number: String,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = SepticaColors.Gold,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = SepticaColors.Ink,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SepticaColors.Cream,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                color = SepticaColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
