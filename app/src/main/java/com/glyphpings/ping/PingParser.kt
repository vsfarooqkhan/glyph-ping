package com.glyphpings.ping

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.glyphpings.matrix.IconRasterizer

/**
 * Pulls "who / which group / what" out of a messaging notification.
 * Prefers MessagingStyle (what WhatsApp, Instagram and Teams use), falls back to title/text.
 */
object PingParser {
    private const val TAG = "PingParser"
    private val iconCache = HashMap<String, IntArray>()
    private val lastSeen = HashMap<String, String>()   // notification key -> last sender+text, to avoid replays

    private val junk = listOf(
        Regex("^checking for new messages", RegexOption.IGNORE_CASE),
        Regex("^\\d+ new messages?( from \\d+ chats?)?$", RegexOption.IGNORE_CASE),
        Regex("^\\d+ messages? from \\d+ chats?$", RegexOption.IGNORE_CASE),
        Regex("backup (in progress|complete)", RegexOption.IGNORE_CASE),
        Regex("^(whatsapp|snapchat|instagram|microsoft teams)( web)? is (running|active)", RegexOption.IGNORE_CASE),
        Regex("^(ringing|calling|ongoing (voice|video) call)", RegexOption.IGNORE_CASE),
    )

    fun parse(context: Context, sbn: StatusBarNotification, prefs: Prefs): Ping? {
        val source = Source.of(sbn.packageName) ?: return null
        if (!prefs.isEnabled(source)) return null
        val n = sbn.notification
        if (sbn.isOngoing || (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return null

        val extras = n.extras
        var sender: String? = null
        var text: String? = null
        var group: String? = null

        val style = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) }.getOrNull()
        val last = style?.messages?.lastOrNull()
        if (style != null && last != null) {
            sender = last.person?.name?.toString()
            text = last.text?.toString()
            if (style.isGroupConversation) group = style.conversationTitle?.toString()
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        if (sender.isNullOrBlank()) sender = title
        if (text.isNullOrBlank()) {
            text = (extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()
        }
        if (group == null && extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)) {
            group = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        }
        // Older WhatsApp style: title "Family (3 messages): Mom" or "Family: Mom"
        if (source == Source.WHATSAPP && group == null && sender != null && sender.contains(": ")) {
            group = sender.substringBefore(": ").replace(Regex("\\s*\\(\\d+ messages?\\)$"), "")
            sender = sender.substringAfter(": ")
        }
        // Group names sometimes carry a message count: "Family (3 messages)"
        group = group?.replace(Regex("\\s*\\(\\d+ messages?\\)$"), "")?.ifBlank { null }

        val who: String = sender?.trim().orEmpty()
        val body: String = text?.trim().orEmpty()
        var chat: String? = group?.trim()
        if (who.isBlank()) return null
        if (who.equals(source.label, ignoreCase = true) && body.isBlank()) return null
        if (junk.any { it.containsMatchIn(body) || it.containsMatchIn(who) }) return null
        if (chat != null && chat == who) chat = null
        if (chat != null && !prefs.showGroups) return null

        // WhatsApp/Instagram re-post the same notification as messages pile up: only show new content
        val signature = "$who|$chat|$body"
        if (lastSeen[sbn.key] == signature) return null
        lastSeen[sbn.key] = signature
        if (lastSeen.size > 200) lastSeen.keys.take(100).forEach { lastSeen.remove(it) }

        return Ping(
            key = sbn.key, source = source, packageName = sbn.packageName,
            sender = who, group = chat, text = body,
            time = sbn.postTime, icon = iconFor(context, sbn),
        )
    }

    fun forget(key: String) { lastSeen.remove(key) }

    /** The app's own notification icon (a white silhouette) -> 25x25. Falls back to the launcher icon. */
    private fun iconFor(context: Context, sbn: StatusBarNotification): IntArray =
        iconCache.getOrPut(sbn.packageName) {
            runCatching {
                sbn.notification.smallIcon?.loadDrawable(context)?.let { IconRasterizer.fromDrawable(it) }
            }.getOrNull()?.takeIf { px -> px.count { it > 0 } > 20 }
                ?: runCatching {
                    IconRasterizer.fromDrawable(context.packageManager.getApplicationIcon(sbn.packageName), useLuminance = true)
                }.onFailure { Log.w(TAG, "no icon for ${sbn.packageName}", it) }.getOrNull()
                ?: IconRasterizer.bubble()
        }
}
