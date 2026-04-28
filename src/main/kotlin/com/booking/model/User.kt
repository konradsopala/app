package com.booking.model

/**
 * Authenticated user of the booking system.
 *
 * [passwordHash] stores the PBKDF2 output produced by
 * [com.booking.util.PasswordHasher]; the plain password is never retained.
 */
data class User(
    val username: String,
    val passwordHash: String,
    val role: Role
) {
    enum class Role { ADMIN, USER }
}
