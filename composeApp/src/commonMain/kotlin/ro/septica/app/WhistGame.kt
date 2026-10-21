package ro.septica.app

import androidx.compose.runtime.Composable
import ro.septica.tricks.TrickMode

@Composable
internal fun WhistGame(onMenu: () -> Unit, paused: Boolean) {
    TrickGameUi(TrickMode.WHIST, onMenu, paused)
}
