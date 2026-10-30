package ro.septica.app

import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

internal actual fun createThemePreferenceStore(): ThemePreferenceStore = DesktopThemePreferenceStore()

private class DesktopThemePreferenceStore : ThemePreferenceStore {
    override fun readThemeId(): Result<String?> =
        try {
            Result.success(
                Preferences.userNodeForPackage(DesktopThemePreferenceStore::class.java)
                    .get(THEME_PREFERENCE_KEY, null),
            )
        } catch (exception: SecurityException) {
            Result.failure(exception)
        }

    override fun writeThemeId(themeId: String): Result<Unit> =
        try {
            Preferences.userNodeForPackage(DesktopThemePreferenceStore::class.java).apply {
                put(THEME_PREFERENCE_KEY, themeId)
                flush()
            }
            Result.success(Unit)
        } catch (exception: BackingStoreException) {
            Result.failure(exception)
        } catch (exception: SecurityException) {
            Result.failure(exception)
        }
}
