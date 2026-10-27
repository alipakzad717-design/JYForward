package ir.jomyekta.jyforward.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Logs ONLY incoming SMS messages that triggered an active [FilterRuleEntity].
 */
@Entity(tableName = "history_logs")
data class HistoryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: Long,
    val ruleTitle: String,
    val sender: String,
    val messageBody: String,
    val matchedKeyword: String,
    val simSlotIndex: Int,       // 0 = SIM 1, 1 = SIM 2, -1 = Unknown
    val subscriptionId: Int,     // Raw Android Telephony subscription_id
    val destinationUrl: String,
    val httpMethod: String,
    val isSuccess: Boolean,
    val httpStatusCode: Int,
    val responseSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)
