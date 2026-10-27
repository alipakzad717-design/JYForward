package ir.jomyekta.jyforward.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.jomyekta.jyforward.service.SmsForwardingService

/**
 * Ensures the sticky Foreground Service restarts automatically after device reboot or app update.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            SmsForwardingService.startStickyService(context)
        }
    }
}
