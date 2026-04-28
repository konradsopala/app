package com.booking.service

import com.booking.model.User
import com.booking.util.PasswordHasher

/**
 * Manages users and the active session.
 *
 * The store is in-memory; durability is provided by [PersistenceService] which
 * snapshots and reloads via [listUsers]/[restoreUsers].
 */
class UserService {

    private val users = linkedMapOf<String, User>()

    var currentUser: User? = null
        private set

    /** Seeds a default admin if no users exist, returning true when seeding occurred. */
    fun seedDefaultAdminIfEmpty(): Boolean {
        if (users.isNotEmpty()) return false
        users["admin"] = User("admin", PasswordHasher.hash("admin123"), User.Role.ADMIN)
        return true
    }

    fun addUser(username: String, password: String, role: User.Role): User {
        require(username.isNotBlank()) { "Username cannot be empty." }
        require(username !in users) { "User $username already exists." }
        require(password.length >= 6) { "Password must be at least 6 characters." }
        val user = User(username, PasswordHasher.hash(password), role)
        users[username] = user
        return user
    }

    fun removeUser(username: String): Boolean {
        if (currentUser?.username == username) return false
        return users.remove(username) != null
    }

    fun changePassword(username: String, newPassword: String): Boolean {
        require(newPassword.length >= 6) { "Password must be at least 6 characters." }
        val existing = users[username] ?: return false
        users[username] = existing.copy(passwordHash = PasswordHasher.hash(newPassword))
        return true
    }

    fun listUsers(): List<User> = users.values.toList()

    fun login(username: String, password: String): User? {
        val user = users[username] ?: return null
        if (!PasswordHasher.verify(password, user.passwordHash)) return null
        currentUser = user
        return user
    }

    fun logout() {
        currentUser = null
    }

    /** Replaces the in-memory store with the supplied users; used by persistence. */
    fun restoreUsers(toLoad: List<User>) {
        users.clear()
        toLoad.forEach { users[it.username] = it }
    }
}
