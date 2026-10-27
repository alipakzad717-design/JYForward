package ir.jomyekta.jyforward.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import ir.jomyekta.jyforward.JYForwardApplication
import ir.jomyekta.jyforward.MainActivity
import ir.jomyekta.jyforward.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sticky Foreground Service that keeps JY Forward alive when locked or in Doze mode,
 * acquires a temporary Partial WakeLock while dispatching webhooks over OkHttp,
 * and updates its persistent notification.
 */
class SmsForwardingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        promoteToForeground()
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()
        _isRunning.value = true

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                _isRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_PROCESS_SMS -> {
                val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty()
                val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
                val simSlot = intent.getIntExtra(EXTRA_SIM_SLOT, -1)
                val subId = intent.getIntExtra(EXTRA_SUB_ID, -1)
                val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

                if (body.isNotBlank()) {
                    processSmsWithWakeLock(sender, body, simSlot, subId, timestamp)
                }
            }
        }

        // Return START_STICKY so Android recreates the service if memory is reclaimed
        return START_STICKY
    }

    private fun processSmsWithWakeLock(
        sender: String,
        body: String,
        simSlot: Int,
        subId: Int,
        timestamp: Long
    ) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "JYForward::SmsWebhookWakeLock"
        ).apply {
            acquire(30_000L) // Hold up to 30 seconds for webhook completion during Doze mode
        }

        serviceScope.launch {
            try {
                val app = application as JYForwardApplication
                app.repository.processIncomingSms(
                    sender = sender,
                    messageBody = body,
                    simSlotIndex = simSlot,
                    subscriptionId = subId,
                    timestamp = timestamp
                )
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    private fun promoteToForeground() {
        val notification = buildStickyNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildStickyNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_body))
            .setSmallIcon(R.drawable.ic_jy_forward_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_body)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "jy_forward_sticky_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "ir.jomyekta.jyforward.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "ir.jomyekta.jyforward.action.STOP_SERVICE"
        const val ACTION_PROCESS_SMS = "ir.jomyekta.jyforward.action.PROCESS_SMS"

        private const val EXTRA_SENDER = "extra_sender"
        private const val EXTRA_BODY = "extra_body"
        private const val EXTRA_SIM_SLOT = "extra_sim_slot"
        private const val EXTRA_SUB_ID = "extra_sub_id"
        private const val EXTRA_TIMESTAMP = "extra_timestamp"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startStickyService(context: Context) {
            val intent = Intent(context, SmsForwardingService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopStickyService(context: Context) {
            val intent = Intent(context, SmsForwardingService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun enqueueSmsProcessing(
            context: Context,
            sender: String,
            messageBody: String,
            simSlotIndex: Int,
            subscriptionId: Int,
            timestamp: Long
        ) {
            val intent = Intent(context, SmsForwardingService::class.java).apply {
                action = ACTION_PROCESS_SMS
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, messageBody)
                putExtra(EXTRA_SIM_SLOT, simSlotIndex)
                putExtra(EXTRA_SUB_ID, subscriptionId)
                putExtra(EXTRA_TIMESTAMP, timestamp)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
