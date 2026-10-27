package com.glyphpings.ping

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/** Receives every notification (after you allow it in Settings > Notification access). */
class PingListenerService : NotificationListenerService() {
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()
        PingCenter.init(this)
        prefs = Prefs(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val ping = try {
            PingParser.parse(this, sbn, prefs)
        } catch (t: Throwable) {
            Log.w("PingListener", "couldn't read notification from ${sbn.packageName}", t); null
        } ?: return
        Log.d("PingListener", "ping from ${ping.source.label} (${if (ping.group != null) "group" else "direct"})")
        PingCenter.onPing(ping)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (Source.of(sbn.packageName) != null) PingCenter.onRemoved(sbn.key)
    }
}
