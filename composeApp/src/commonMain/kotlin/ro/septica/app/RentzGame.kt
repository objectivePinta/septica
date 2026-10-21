package ro.septica.app

import androidx.compose.runtime.Composable
import ro.septica.tricks.TrickMode

@Composable
internal fun RentzGame(onMenu: () -> Unit, paused: Boolean) {
    TrickGameUi(TrickMode.RENTZ, onMenu, paused)
}
