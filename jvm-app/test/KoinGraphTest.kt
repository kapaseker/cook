import di.platformDataModule
import di.uiModule
import org.koin.core.context.startKoin
import repository.agent.CookRepo
import repository.settings.SettingsStore
import settings.WindowStateStore
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinGraphTest {
    /** Verifies that production data bindings resolve from the application graph. */
    @Test
    fun `production data bindings resolve from the application graph`() {
        val application = startKoin {
            modules(uiModule, platformDataModule)
        }

        try {
            assertNotNull(application.koin.get<CookRepo>())
            assertNotNull(application.koin.get<SettingsStore>())
            assertNotNull(application.koin.get<WindowStateStore>())
        } finally {
            application.close()
        }
    }
}