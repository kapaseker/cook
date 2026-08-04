package page.chat.biz

/** Holds the active chat's close-time persistence callback without retaining stale ViewModels. */
class ChatPersistenceFlushRegistry {
    private var owner: Any? = null
    private var flushAction: (suspend () -> Unit)? = null

    @Synchronized
    fun register(owner: Any, flushAction: suspend () -> Unit) {
        this.owner = owner
        this.flushAction = flushAction
    }

    @Synchronized
    fun unregister(owner: Any) {
        if (this.owner === owner) {
            this.owner = null
            flushAction = null
        }
    }

    suspend fun flush() {
        val action = synchronized(this) { flushAction }
        action?.invoke()
    }
}
