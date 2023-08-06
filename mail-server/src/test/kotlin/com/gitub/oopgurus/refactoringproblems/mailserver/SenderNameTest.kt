package com.gitub.oopgurus.refactoringproblems.mailserver

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SenderNameTest {
    @Test
    fun blank_name() {
        assertThrows(RuntimeException::class.java) { SenderName("   ") }
    }
}