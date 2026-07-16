package di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import page.chat.ChatStrings
import page.chat.biz.ChatViewModel
import page.settings.biz.SettingsViewModel

val uiModule = module {
    viewModel { parameters -> ChatViewModel(get(), parameters.get<ChatStrings>()) }
    viewModel { SettingsViewModel(get()) }
}