package com.booking.test

import kotlin.system.exitProcess

/**
 * Tiny in-process test runner — no JUnit, no external deps.
 *
 * Each suite is a top-level function that calls [test] for individual cases;
 * [main] wires them up and exits with a non-zero status on any failure.
 */
object TestRunner {
    private var passed = 0
    private var failed = 0
    private val failures = mutableListOf<String>()

    fun suite(name: String, block: () -> Unit) {
        println("\n--- $name ---")
        block()
    }

    fun test(name: String, block: () -> Unit) {
        try {
            block()
            passed++
            println("  PASS  $name")
        } catch (e: Throwable) {
            failed++
            val msg = e.message ?: e::class.simpleName ?: "unknown"
            failures.add("$name :: $msg")
            println("  FAIL  $name -- $msg")
        }
    }

    fun summary(): Int {
        println("\n=== Results ===")
        println("Passed: $passed")
        println("Failed: $failed")
        if (failures.isNotEmpty()) {
            println("\nFailures:")
            failures.forEach { println("  - $it") }
        }
        return if (failed > 0) 1 else 0
    }
}

fun assertEquals(expected: Any?, actual: Any?, msg: String? = null) {
    if (expected != actual) {
        throw AssertionError(msg ?: "Expected <$expected>, got <$actual>")
    }
}

fun assertTrue(condition: Boolean, msg: String = "Expected true") {
    if (!condition) throw AssertionError(msg)
}

fun assertFalse(condition: Boolean, msg: String = "Expected false") {
    if (condition) throw AssertionError(msg)
}

fun assertNotNull(value: Any?, msg: String = "Expected non-null") {
    if (value == null) throw AssertionError(msg)
}

fun assertNull(value: Any?, msg: String = "Expected null, got <$value>") {
    if (value != null) throw AssertionError(msg)
}

inline fun <reified T : Throwable> assertThrows(noinline block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return
        throw AssertionError("Expected ${T::class.simpleName}, got ${e::class.simpleName}: ${e.message}")
    }
    throw AssertionError("Expected ${T::class.simpleName}, but nothing was thrown")
}

fun main() {
    runBookingServiceTests()
    runBookingValidatorTests()
    runBookingPricerTests()
    runCsvImporterTests()
    runPersistenceServiceTests()
    runUserServiceTests()
    exitProcess(TestRunner.summary())
}
