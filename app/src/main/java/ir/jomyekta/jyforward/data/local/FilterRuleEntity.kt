package ir.jomyekta.jyforward.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-defined SMS forwarding rule in the Room Database.
 *
 * - [senderFilter]: Phone number, short-code, or contact name. Use "*" or empty to match any sender.
 * - [keyword]: Case-insensitive keyword to search in the SMS body (e.g. "واریز" or "Deposit").
 *              Empty or "*" matches any SMS content.
 * - [destinationUrl]: Target webhook URL containing placeholders like {msg}, {sender}, {sim}, {time}.
 * - [httpMethod]: "GET" or "POST".
 * - [simSlotFilter]: -1 for Both SIMs, 0 for SIM 1, 1 for SIM 2.
 * - [isEnabled]: Active / Paused toggle.
 */
@Entity(tableName = "filter_rules")
data class FilterRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val senderFilter: String,
    val keyword: String,
    val destinationUrl: String,
    val httpMethod: String = "GET", // "GET" or "POST"
    val simSlotFilter: Int = -1,    // -1 = Both SIMs, 0 = SIM 1, 1 = SIM 2
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if an incoming SMS matches this active rule.
     */
    fun matches(incomingSender: String, incomingBody: String, incomingSimSlot: Int): Boolean {
        if (!isEnabled) return false

        // 1. SIM Slot Matching (-1 means Both SIMs)
        if (simSlotFilter != -1 && incomingSimSlot != -1 && simSlotFilter != incomingSimSlot) {
            return false
        }

        // 2. Sender Matching (case-insensitive substring or wildcard "*")
        val cleanSenderFilter = senderFilter.trim()
        val senderMatches = cleanSenderFilter.isEmpty() ||
            cleanSenderFilter == "*" ||
            incomingSender.contains(cleanSenderFilter, ignoreCase = true)

        if (!senderMatches) return false

        // 3. Case-Insensitive Keyword Matching (supports Persian/Farsi e.g., "واریز" and English)
        val cleanKeyword = keyword.trim()
        val keywordMatches = cleanKeyword.isEmpty() ||
            cleanKeyword == "*" ||
            incomingBody.contains(cleanKeyword, ignoreCase = true)

        return keywordMatches
    }

    /**
     * Generates a concise human-readable summary for the Filter card UI.
     */
    fun toSummaryText(isPersian: Boolean = false): String {
        val simText = when (simSlotFilter) {
            0 -> if (isPersian) "سیم‌کارت ۱" else "SIM 1"
            1 -> if (isPersian) "سیم‌کارت ۲" else "SIM 2"
            else -> if (isPersian) "هر دو سیم‌کارت" else "Both SIMs"
        }
        val senderText = if (senderFilter.isBlank() || senderFilter == "*") {
            if (isPersian) "همه فرستنده‌ها" else "Any Sender"
        } else senderFilter

        val keywordText = if (keyword.isBlank() || keyword == "*") {
            if (isPersian) "هر متنی" else "Any Keyword"
        } else "\"$keyword\""

        return if (isPersian) {
            "اگر پیامک از [$senderText] در [$simText] حاوی $keywordText بود ← ارسال با متد $httpMethod"
        } else {
            "If SMS from [$senderText] on [$simText] contains $keywordText → Forward via $httpMethod"
        }
    }
}
