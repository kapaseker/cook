package repository.history

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlinx.coroutines.Dispatchers

internal fun createConversationHistoryDatabase(): ConversationHistoryDatabase {
    val directory = File(System.getProperty("user.home"), ".cook")
    check(directory.exists() || directory.mkdirs()) {
        "Unable to create Cook data directory: ${directory.absolutePath}"
    }
    val databaseFile = File(directory, "cook.db")
    return Room.databaseBuilder<ConversationHistoryDatabase>(databaseFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
