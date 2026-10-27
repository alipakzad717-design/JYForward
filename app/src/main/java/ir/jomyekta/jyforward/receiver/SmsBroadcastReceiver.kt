package ir.jomyekta.jyforward.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import ir.jomyekta.jyforward.service.SmsForwardingService

/**
 * Standalone BroadcastReceiver for android.provider.Telephony.SMS_RECEIVED.
 *
 * Key Architecture Points:
 * 1. Does NOT replace the default system SMS app (only listens to SMS_RECEIVED).
 * 2. Concatenates multi-part SMS PDUs from the same sender into a single unified message body.
 * 3. Extracts Dual-SIM subscription ID (`subscription`, `android.telephony.extra.SUBSCRIPTION_INDEX`, `slot`, `simId`)
 *    and maps it to SIM Slot Index (0 = SIM 1, 1 = SIM 2).
 * 4. Hands off the parsed SMS to [SmsForwardingService] (Foreground Service) for Doze-safe webhook execution.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Parse Dual-SIM Subscription ID & Slot Index from Intent extras
        val extras = intent.extras
        val subscriptionId = extractSubscriptionId(extras)
        val simSlotIndex = resolveSimSlotIndex(context, extras, subscriptionId)

        // Group multi-part SMS segments by originating address
        val groupedBySender = linkedMapOf<String, StringBuilder>()
        var timestamp = System.currentTimeMillis()

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "Unknown"
            val bodyPart = sms.displayMessageBody ?: sms.messageBody ?: ""
            timestamp = sms.timestampMillis.takeIf { it > 0 } ?: timestamp
            groupedBySender.getOrPut(sender) { StringBuilder() }.append(bodyPart)
        }

        // Dispatch each reconstructed message to the sticky Foreground Service
        for ((sender, fullBodyBuilder) in groupedBySender) {
            val fullMessage = fullBodyBuilder.toString()
            if (fullMessage.isNotBlank()) {
                SmsForwardingService.enqueueSmsProcessing(
                    context = context,
                    sender = sender,
                    messageBody = fullMessage,
                    simSlotIndex = simSlotIndex,
                    subscriptionId = subscriptionId,
                    timestamp = timestamp
                )
            }
        }
    }

    private fun extractSubscriptionId(extras: Bundle?): Int {
        if (extras == null) return SubscriptionManager.INVALID_SUBSCRIPTION_ID
        val possibleKeys = listOf(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            "subscription",
            "android.telephony.extra.SUBSCRIPTION_INDEX",
            "sub_id",
            "simId"
        )
        for (key in possibleKeys) {
            if (extras.containsKey(key)) {
                val value = extras.getInt(key, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                if (value != SubscriptionManager.INVALID_SUBSCRIPTION_ID && value >= 0) {
                    return value
                }
            }
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID
    }

    private fun resolveSimSlotIndex(context: Context, extras: Bundle?, subscriptionId: Int): Int {
        if (extras != null) {
            val slotKeys = listOf(
                SubscriptionManager.EXTRA_SLOT_INDEX,
                "slot",
                "simSlot",
                "phone",
                "android.telephony.extra.SLOT_INDEX"
            )
            for (key in slotKeys) {
                if (extras.containsKey(key)) {
                    val slot = extras.getInt(key, -1)
                    if (slot == 0 || slot == 1) return slot
                }
            }
        }

        // Fallback: Query SubscriptionManager if READ_PHONE_STATE is granted
        if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val info = subManager?.getActiveSubscriptionInfo(subscriptionId)
                if (info != null && info.simSlotIndex >= 0) {
                    return info.simSlotIndex
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }

        return if (subscriptionId == 1) 0 else if (subscriptionId == 2) 1 else -1
    }
}
