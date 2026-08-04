package di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import page.chat.ChatStrings
import page.chat.biz.ChatViewModel
import page.chat.biz.ChatPersistenceFlushRegistry
import page.settings.biz.SettingsViewModel
import repository.agent.CookModel

val uiModule = module {
    single { ChatPersistenceFlushRegistry() }
    viewModel { parameters ->
        ChatViewModel(
            cookRepository = get(),
            historyRepository = get(),
            strings = parameters.get<ChatStrings>(),
            initialModel = parameters.get<CookModel>(),
            settingsStore = get(),
        )
    }
    viewModel { SettingsViewModel(get()) }
}
