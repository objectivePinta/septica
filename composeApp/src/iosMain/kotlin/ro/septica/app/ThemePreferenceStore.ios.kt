package ro.septica.app

import platform.Foundation.NSUserDefaults

internal actual fun createThemePreferenceStore(): ThemePreferenceStore = IosThemePreferenceStore()

private class IosThemePreferenceStore : ThemePreferenceStore {
    override fun readThemeId(): Result<String?> =
        Result.success(NSUserDefaults.standardUserDefaults.stringForKey(THEME_PREFERENCE_KEY))

    override fun writeThemeId(themeId: String): Result<Unit> {
        NSUserDefaults.standardUserDefaults.setObject(themeId, forKey = THEME_PREFERENCE_KEY)
        return Result.success(Unit)
    }
}
