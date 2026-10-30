package ro.septica.app

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ThemePreferenceTest {
    @Test
    fun missingOrUnknownThemeIdUsesClassicRomanianWithoutError() {
        assertEquals(
            ThemePreferenceLoad(GameTheme.CLASSIC_ROMANIAN, null),
            loadGameTheme(FakeThemePreferenceStore()),
        )
        assertEquals(
            ThemePreferenceLoad(GameTheme.CLASSIC_ROMANIAN, null),
            loadGameTheme(FakeThemePreferenceStore(storedThemeId = "unknown")),
        )
    }

    @Test
    fun savedThemeCanBeLoadedAfterRoundTrip() {
        val store = FakeThemePreferenceStore(storedThemeId = GameTheme.NIGHT.storageId)

        assertEquals(GameTheme.NIGHT, loadGameTheme(store).theme)
        assertNull(saveGameTheme(store, GameTheme.RETRO))
        assertEquals(GameTheme.RETRO, loadGameTheme(store).theme)
    }

    @Test
    fun failedReadFallsBackAndReportsRomanianError() {
        val loadedTheme = loadGameTheme(
            FakeThemePreferenceStore(readFailure = IllegalStateException("read failed")),
        )

        assertEquals(GameTheme.CLASSIC_ROMANIAN, loadedTheme.theme)
        assertContains(assertNotNull(loadedTheme.errorMessage), "Nu s-a putut încărca tema")
    }

    @Test
    fun failedWriteReportsThatThemeWasNotSaved() {
        val errorMessage = saveGameTheme(
            FakeThemePreferenceStore(writeFailure = IllegalStateException("write failed")),
            GameTheme.NIGHT,
        )

        assertContains(assertNotNull(errorMessage), "nu a putut fi salvată")
    }
}

private class FakeThemePreferenceStore(
    private var storedThemeId: String? = null,
    private val readFailure: Throwable? = null,
    private val writeFailure: Throwable? = null,
) : ThemePreferenceStore {
    override fun readThemeId(): Result<String?> {
        val failure = readFailure
        return if (failure == null) Result.success(storedThemeId) else Result.failure(failure)
    }

    override fun writeThemeId(themeId: String): Result<Unit> {
        val failure = writeFailure
        if (failure != null) return Result.failure(failure)

        storedThemeId = themeId
        return Result.success(Unit)
    }
}
