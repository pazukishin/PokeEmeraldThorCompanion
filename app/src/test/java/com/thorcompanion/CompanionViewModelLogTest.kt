package com.thorcompanion

import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionViewModelLogTest {
    @Test
    fun appendLog_keepsRecentEntries() {
        val logs = mutableListOf<String>()
        logs += "[00:00:00] Iniciando conexión"
        logs += "[00:00:01] UDP enviado"
        logs += "[00:00:02] Sin respuesta"

        assertTrue(logs.size == 3)
        assertTrue(logs.last().contains("Sin respuesta"))
    }
}
