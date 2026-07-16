package di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import agent.DictionaryApiClient
import agent.DictionaryClient
import org.koin.core.module.Module
import org.koin.dsl.module
import repository.agent.CookRepo
import repository.agent.CookRepository
import repository.settings.SettingsStorage
import repository.settings.SettingsStore
import repository.settings.settingsDataStore
import settings.WindowStateStorage
import settings.WindowStateStore

actual val platformDataModule: Module = module {
    single<DataStore<Preferences>> { settingsDataStore }
    single<SettingsStore> { SettingsStorage(get()) }
    single<WindowStateStore> { WindowStateStorage(get()) }
    single<DictionaryClient> { DictionaryApiClient() }
    single<CookRepo> { CookRepository(get()) }
}