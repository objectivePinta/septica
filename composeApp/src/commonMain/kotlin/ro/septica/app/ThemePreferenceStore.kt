package ro.septica.app

internal const val THEME_PREFERENCE_KEY = "ro.septica.app.theme"

internal interface ThemePreferenceStore {
    fun readThemeId(): Result<String?>

    fun writeThemeId(themeId: String): Result<Unit>
}

internal expect fun createThemePreferenceStore(): ThemePreferenceStore

internal data class ThemePreferenceLoad(
    val theme: GameTheme,
    val errorMessage: String?,
)

internal fun loadGameTheme(store: ThemePreferenceStore): ThemePreferenceLoad {
    val themeId = store.readThemeId().getOrElse {
        return ThemePreferenceLoad(
            theme = GameTheme.CLASSIC_ROMANIAN,
            errorMessage = "Nu s-a putut încărca tema salvată. Se folosește Clasic românesc.",
        )
    }

    return ThemePreferenceLoad(
        theme = GameTheme.fromStorageId(themeId),
        errorMessage = null,
    )
}

internal fun saveGameTheme(store: ThemePreferenceStore, theme: GameTheme): String? =
    store.writeThemeId(theme.storageId).exceptionOrNull()?.let {
        "Tema a fost schimbată pentru această sesiune, dar nu a putut fi salvată."
    }
