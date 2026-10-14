package com.ashish.leadzam.messaging

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ashish.leadzam.MainActivity
import com.ashish.leadzam.data.model.DialRequest

object NotificationHelper {

    const val CHANNEL_ID = "crm_call_requests_channel"
    private const val CHANNEL_NAME = "CRM Call Requests"
    private const val CHANNEL_DESCRIPTION = "High-priority notifications for incoming CRM click-to-call requests"

    const val EXTRA_REQUEST_ID = "extra_request_id"
    const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    const val EXTRA_CONTACT_NAME = "extra_contact_name"
    const val EXTRA_SOURCE = "extra_source"
    const val EXTRA_AUTO_DIAL = "extra_auto_dial"

    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showCallNotification(context: Context, dialRequest: DialRequest) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_REQUEST_ID, dialRequest.id)
            putExtra(EXTRA_PHONE_NUMBER, dialRequest.phoneNumber)
            putExtra(EXTRA_CONTACT_NAME, dialRequest.displayContactName)
            putExtra(EXTRA_SOURCE, dialRequest.source)
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            context,
            dialRequest.id.hashCode(),
            intent,
            pendingIntentFlags
        )

        // Notification Action: Open Dialer
        val dialIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_REQUEST_ID, dialRequest.id)
            putExtra(EXTRA_PHONE_NUMBER, dialRequest.phoneNumber)
            putExtra(EXTRA_CONTACT_NAME, dialRequest.displayContactName)
            putExtra(EXTRA_SOURCE, dialRequest.source)
            putExtra(EXTRA_AUTO_DIAL, true)
        }

        val dialPendingIntent = PendingIntent.getActivity(
            context,
            (dialRequest.id + "_dial").hashCode(),
            dialIntent,
            pendingIntentFlags
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("CRM Call: ${dialRequest.displayContactName}")
            .setContentText("Tap to open dialer with ${dialRequest.phoneNumber}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Incoming call request for ${dialRequest.displayContactName}\nNumber: ${dialRequest.phoneNumber}\nSource: ${dialRequest.source}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_call,
                "Open Dialer",
                dialPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(dialRequest.id.hashCode(), notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
