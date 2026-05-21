package com.booking.notification

/**
 * Fans out [NotificationEvent]s to every registered [Notifier].
 *
 * Notifier exceptions are isolated: a failure in one channel is logged to
 * stderr but never aborts dispatch to the rest. Notifiers are invoked in
 * registration order; downstream consumers must not rely on ordering for
 * correctness.
 */
class NotificationDispatcher {

    private val notifiers = mutableListOf<Notifier>()

    fun register(notifier: Notifier) {
        notifiers.add(notifier)
    }

    fun unregister(name: String): Boolean =
        notifiers.removeIf { it.name == name }

    fun registered(): List<String> = notifiers.map { it.name }

    fun dispatch(event: NotificationEvent) {
        for (n in notifiers) {
            try {
                n.handle(event)
            } catch (e: Exception) {
                System.err.println("[notify] '${n.name}' failed: ${e.message}")
            }
        }
    }
}
