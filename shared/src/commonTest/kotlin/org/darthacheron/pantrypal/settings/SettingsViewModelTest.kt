package org.darthacheron.pantrypal.settings

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: SettingsViewModel
    private lateinit var repository: SettingsRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock<SettingsRepository>()
        every { repository.getSettingsFlow() } returns flowOf(Settings(themeMode = ThemeMode.DARK))
        viewModel = SettingsViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoading() = runTest {
        val state = viewModel.settingsFlow.value
        assertTrue(state.hasData)
        assertEquals(ThemeMode.DARK, state.data?.themeMode)
    }

    @Test
    fun testOnThemeModeSelected() {
        viewModel.onThemeModeSelected(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.settingsFlow.value.data?.themeMode)
    }

    @Test
    fun testSaveSettings() = runTest {
        everySuspend { repository.saveSettings(any()) } returns Unit
        var successCalled = false
        
        viewModel.onThemeModeSelected(ThemeMode.SYSTEM)
        viewModel.saveSettings { successCalled = true }
        
        verifySuspend { repository.saveSettings(Settings(themeMode = ThemeMode.SYSTEM)) }
        assertTrue(successCalled)
    }

    @Test
    fun testRevertChanges() {
        viewModel.onThemeModeSelected(ThemeMode.LIGHT)
        viewModel.revertChanges()
        assertEquals(ThemeMode.DARK, viewModel.settingsFlow.value.data?.themeMode)
    }
}
