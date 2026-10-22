package com.example.inspectorappupdate.utils

import android.nfc.Tag
import android.nfc.tech.NfcA

/**
 * MIFARE Ultralight EV1 commands used on a tap: the NXP originality signature, the tap counter (raised, then read)
 * and setting our per-card password. Every step is optional: a command the chip or phone refuses is skipped and the
 * tap carries on. Calls block, so run them off the main thread.
 */
object CardChip {

    private const val GET_VERSION = 0x60
    private const val READ = 0x30
    private const val WRITE = 0xA2
    private const val READ_SIG = 0x3C
    private const val READ_CNT = 0x39
    private const val INCR_CNT = 0xA5
    private const val PWD_AUTH = 0x1B
    private const val TAP_COUNTER = 0
    // Writes from this page on need the password; reading stays open.
    private const val PROTECT_FROM_PAGE = 0x04
    private const val TIMEOUT_MS = 300

    // A test password an earlier POS build may have put on some cards; such cards are moved to their own password.
    private val LEGACY_PWD = hex("12345678")
    private val LEGACY_PACK = hex("9876")

    enum class Protection { OPEN, PROTECTED, UNKNOWN }

    data class Reading(
        val uid: String,
        val ev1: Boolean,
        val signature: String? = null,
        /** The counter after this tap raised it; null when it couldn't be raised. */
        val counter: Int? = null,
        val protection: Protection = Protection.UNKNOWN,
    )

    enum class SetupResult { DONE, ALREADY, FOREIGN_PASSWORD, FAILED, NOT_EV1 }

    // EV1 MF0UL11 keeps its configuration at page 0x10, MF0UL21 at 0x25.
    private fun configPage(version: ByteArray?): Int? = when {
        version == null || version.size < 8 || version[1] != 0x04.toByte() || version[2] != 0x03.toByte() -> null
        version[6] == 0x0B.toByte() -> 0x10
        version[6] == 0x0E.toByte() -> 0x25
        else -> null
    }

    /** Reads the chip; raises the tap counter first unless [raiseCounter] is false. */
    fun read(tag: Tag, raiseCounter: Boolean = true, password: ByteArray? = null): Reading {
        val uid = tag.id.toHex()
        val chip = NfcA.get(tag) ?: return Reading(uid, false)
        return try {
            chip.connect()
            chip.timeout = TIMEOUT_MS
            val cfg = configPage(send(chip, GET_VERSION)) ?: return Reading(uid, false)
            val signature = send(chip, READ_SIG, 0x00)?.takeIf { it.size == 32 }?.toHex()
            val protection = send(chip, READ, cfg)?.takeIf { it.size >= 16 }
                ?.let { if ((it[3].toInt() and 0xFF) == 0xFF) Protection.OPEN else Protection.PROTECTED } ?: Protection.UNKNOWN
            var counter: Int? = null
            if (raiseCounter) {
                var raised = raise(chip)
                // Some chips may only count after the password; then it is given and the count tried again.
                if (!raised && password != null) {
                    reconnect(chip)
                    raised = send(chip, PWD_AUTH, *password.ints()) != null && raise(chip)
                }
                if (!raised) reconnect(chip)
                counter = if (raised) send(chip, READ_CNT, TAP_COUNTER)?.takeIf { it.size >= 3 }?.let { littleEndian(it) } else null
            }
            Reading(uid, true, signature, counter, protection)
        } catch (e: Exception) {
            Reading(uid, false)
        } finally {
            try { chip.close() } catch (_: Exception) { }
        }
    }

    /**
     * Puts our password on a chip that has none: password, its answer (PACK), write protection with reads open, then
     * the protected-from page last, so a card taken away midway stays open and is set up on a later tap.
     * A chip already protected by us is ALREADY; by the old test password it is moved to ours; otherwise FOREIGN_PASSWORD.
     */
    fun setUp(tag: Tag, password: ByteArray, pack: ByteArray): SetupResult {
        val chip = NfcA.get(tag) ?: return SetupResult.NOT_EV1
        return try {
            chip.connect()
            chip.timeout = TIMEOUT_MS
            val cfg = configPage(send(chip, GET_VERSION)) ?: return SetupResult.NOT_EV1
            val pages = send(chip, READ, cfg)?.takeIf { it.size >= 16 } ?: return SetupResult.FAILED
            if ((pages[3].toInt() and 0xFF) != 0xFF) {
                if (answers(chip, password, pack)) return SetupResult.ALREADY
                reconnect(chip)
                if (!answers(chip, LEGACY_PWD, LEGACY_PACK)) return SetupResult.FOREIGN_PASSWORD
                return if (write(chip, cfg + 2, password) && write(chip, cfg + 3, byteArrayOf(pack[0], pack[1], 0, 0))) SetupResult.DONE
                else SetupResult.FAILED
            }

            // ACCESS: reads stay open (PROT 0), configuration not locked (CFGLCK 0), no limit on wrong passwords (AUTHLIM 0).
            val access = (pages[4].toInt() and 0x38).toByte()
            val ok = write(chip, cfg + 2, password) &&
                write(chip, cfg + 3, byteArrayOf(pack[0], pack[1], 0, 0)) &&
                write(chip, cfg + 1, byteArrayOf(access, pages[5], pages[6], pages[7])) &&
                write(chip, cfg, byteArrayOf(pages[0], pages[1], pages[2], PROTECT_FROM_PAGE.toByte()))
            if (!ok) return SetupResult.FAILED
            reconnect(chip)
            if (answers(chip, password, pack)) SetupResult.DONE else SetupResult.FAILED
        } catch (e: Exception) {
            SetupResult.FAILED
        } finally {
            try { chip.close() } catch (_: Exception) { }
        }
    }

    fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun answers(chip: NfcA, password: ByteArray, pack: ByteArray): Boolean =
        send(chip, PWD_AUTH, *password.ints())?.let { it.size >= 2 && it[0] == pack[0] && it[1] == pack[1] } ?: false

    private fun raise(chip: NfcA): Boolean = ack(chip, INCR_CNT, TAP_COUNTER, 0x01, 0x00, 0x00, 0x00)

    private fun write(chip: NfcA, page: Int, data: ByteArray): Boolean = ack(chip, WRITE, page, *data.ints())

    // A write answers with a 4-bit ACK (0xA); a refusal is a NAK or no answer.
    private fun ack(chip: NfcA, vararg command: Int): Boolean = try {
        val answer = chip.transceive(command.map { it.toByte() }.toByteArray())
        answer.isEmpty() || (answer[0].toInt() and 0x0F) == 0x0A
    } catch (e: Exception) {
        false
    }

    // A refused command leaves the chip halted; a short answer (NAK) counts as no answer.
    private fun send(chip: NfcA, vararg command: Int): ByteArray? = try {
        chip.transceive(command.map { it.toByte() }.toByteArray()).takeIf { it.size > 1 }
    } catch (e: Exception) {
        null
    }

    private fun reconnect(chip: NfcA) {
        try { chip.close() } catch (_: Exception) { }
        try { chip.connect(); chip.timeout = TIMEOUT_MS } catch (_: Exception) { }
    }

    private fun littleEndian(bytes: ByteArray): Int =
        (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8) or ((bytes[2].toInt() and 0xFF) shl 16)

    private fun ByteArray.ints(): IntArray = map { it.toInt() and 0xFF }.toIntArray()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
