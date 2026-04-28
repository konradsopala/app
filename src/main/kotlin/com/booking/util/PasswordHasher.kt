package com.booking.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-WithHmacSHA256 password hashing using only the JDK.
 *
 * Hashes are encoded as `iterations:base64Salt:base64Hash` so a single string
 * round-trips through any text-based persistence layer.
 */
object PasswordHasher {

    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private val random = SecureRandom()

    fun hash(password: String): String {
        require(password.isNotEmpty()) { "Password cannot be empty." }
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS)
        return "$ITERATIONS:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(hash)}"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 3) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val salt = try { Base64.getDecoder().decode(parts[1]) } catch (_: IllegalArgumentException) { return false }
        val expected = try { Base64.getDecoder().decode(parts[2]) } catch (_: IllegalArgumentException) { return false }
        val actual = pbkdf2(password, salt, iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
