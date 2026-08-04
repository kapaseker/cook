package page.chat.biz

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatPersistenceFlushRegistryTest {
    @Test
    fun `flush invokes only the currently registered chat`() = runBlocking {
        val registry = ChatPersistenceFlushRegistry()
        val calls = mutableListOf<String>()
        val firstOwner = Any()
        val secondOwner = Any()

        registry.register(firstOwner) { calls += "first" }
        registry.register(secondOwner) { calls += "second" }
        registry.unregister(firstOwner)
        registry.flush()

        assertEquals(listOf("second"), calls)
    }
}
