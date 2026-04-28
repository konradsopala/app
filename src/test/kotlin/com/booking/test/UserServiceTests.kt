package com.booking.test

import com.booking.model.User
import com.booking.service.UserService
import com.booking.util.PasswordHasher

fun runUserServiceTests() = TestRunner.suite("UserService & PasswordHasher") {

    TestRunner.test("hash + verify round-trips, rejects wrong password") {
        val h = PasswordHasher.hash("hunter2!")
        assertTrue(PasswordHasher.verify("hunter2!", h))
        assertFalse(PasswordHasher.verify("hunter2", h))
        assertFalse(PasswordHasher.verify("nonsense", h))
    }

    TestRunner.test("two hashes of same password differ (random salt)") {
        val a = PasswordHasher.hash("samepw")
        val b = PasswordHasher.hash("samepw")
        assertFalse(a == b, "salt must differ")
        assertTrue(PasswordHasher.verify("samepw", a))
        assertTrue(PasswordHasher.verify("samepw", b))
    }

    TestRunner.test("verify on malformed stored value returns false (no throw)") {
        assertFalse(PasswordHasher.verify("x", "garbage"))
        assertFalse(PasswordHasher.verify("x", ""))
        assertFalse(PasswordHasher.verify("x", "1:not-base64:also-not-base64"))
    }

    TestRunner.test("seedDefaultAdminIfEmpty seeds once") {
        val u = UserService()
        assertTrue(u.seedDefaultAdminIfEmpty())
        assertFalse(u.seedDefaultAdminIfEmpty())
        assertEquals(1, u.listUsers().size)
        assertEquals(User.Role.ADMIN, u.listUsers().first().role)
    }

    TestRunner.test("login sets currentUser; wrong password leaves it null") {
        val u = UserService()
        u.addUser("alice", "alicepw", User.Role.ADMIN)
        assertNull(u.currentUser)
        assertNotNull(u.login("alice", "alicepw"))
        assertEquals("alice", u.currentUser?.username)

        u.logout()
        assertNull(u.currentUser)
        assertNull(u.login("alice", "wrong"))
        assertNull(u.currentUser)
    }

    TestRunner.test("addUser rejects short password and duplicates") {
        val u = UserService()
        assertThrows<IllegalArgumentException> { u.addUser("a", "short", User.Role.USER) }
        u.addUser("alice", "alicepw", User.Role.USER)
        assertThrows<IllegalArgumentException> { u.addUser("alice", "alicepw", User.Role.USER) }
    }

    TestRunner.test("removeUser refuses to remove the active session user") {
        val u = UserService()
        u.addUser("alice", "alicepw", User.Role.ADMIN)
        u.login("alice", "alicepw")
        assertFalse(u.removeUser("alice"))
        u.logout()
        assertTrue(u.removeUser("alice"))
    }

    TestRunner.test("changePassword invalidates old, accepts new") {
        val u = UserService()
        u.addUser("alice", "alicepw", User.Role.USER)
        assertTrue(u.changePassword("alice", "newpassword"))
        assertNull(u.login("alice", "alicepw"))
        assertNotNull(u.login("alice", "newpassword"))
    }
}
