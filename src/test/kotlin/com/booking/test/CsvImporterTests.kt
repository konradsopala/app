package com.booking.test

import com.booking.service.BookingService
import com.booking.service.BookingValidator
import com.booking.service.CsvImporter
import java.io.File
import java.time.LocalDate

fun runCsvImporterTests() = TestRunner.suite("CsvImporter") {

    fun futureDate(daysAhead: Long = 30) = LocalDate.now().plusDays(daysAhead)

    TestRunner.test("imports valid rows and skips bad ones") {
        val s = BookingService()
        val v = BookingValidator(s)
        val imp = CsvImporter(s, v)

        val tmp = File.createTempFile("import-", ".csv").apply { deleteOnExit() }
        tmp.writeText(
            """
            customer,date,start,duration,description
            Alice,${futureDate()},09:00,60,Coffee
            Bob,${futureDate(31)},14:00,30,Tea
            ,${futureDate(32)},09:00,60,Missing customer
            Carol,not-a-date,09:00,60,Bad date
            """.trimIndent()
        )

        val r = imp.import(tmp)
        assertEquals(2, r.imported.size, "expected 2 imports, got ${r.imported.size}")
        assertEquals(2, r.failures.size, "expected 2 failures, got ${r.failures.map { it.errors }}")
        assertTrue(s.auditLog.getAll().any { it.action.name == "IMPORTED" })
    }

    TestRunner.test("missing required column rejects file") {
        val s = BookingService()
        val v = BookingValidator(s)
        val imp = CsvImporter(s, v)

        val tmp = File.createTempFile("import-", ".csv").apply { deleteOnExit() }
        tmp.writeText("customer,date,start,description\nAlice,${futureDate()},09:00,Coffee\n")

        assertThrows<IllegalArgumentException> { imp.import(tmp) }
    }

    TestRunner.test("capacity is enforced during import sequence") {
        val s = BookingService()
        val v = BookingValidator(s)
        val imp = CsvImporter(s, v)

        val date = futureDate()
        val tmp = File.createTempFile("import-", ".csv").apply { deleteOnExit() }
        tmp.writeText(
            """
            customer,date,start,duration,description
            Alice,$date,10:00,60,first
            Bob,$date,10:30,60,second-overlap
            """.trimIndent()
        )

        val r = imp.import(tmp)
        assertEquals(1, r.imported.size)
        assertEquals(1, r.failures.size)
        assertTrue(r.failures.first().errors.any { it.contains("Time slot is full") })
    }
}
