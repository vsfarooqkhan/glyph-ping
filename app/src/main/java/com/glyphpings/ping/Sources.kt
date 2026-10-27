package com.glyphpings.ping

/** Apps we listen to. Add more by appending a line (package names are on the app's Play Store URL). */
enum class Source(val label: String, val packages: Set<String>) {
    WHATSAPP("WhatsApp", setOf("com.whatsapp", "com.whatsapp.w4b")),
    SNAPCHAT("Snapchat", setOf("com.snapchat.android")),
    INSTAGRAM("Instagram", setOf("com.instagram.android")),
    TEAMS("Microsoft Teams", setOf("com.microsoft.teams"));

    companion object {
        fun of(pkg: String): Source? = entries.firstOrNull { pkg in it.packages }
    }
}

data class Ping(
    val key: String,                 // notification key (used to clear it when you read it)
    val source: Source,
    val packageName: String,
    val sender: String,
    val group: String?,              // null for 1:1 chats
    val text: String,
    val time: Long,
    val icon: IntArray,              // 25x25 frame of the app's notification icon
) {
    override fun equals(other: Any?) = other is Ping && other.key == key && other.time == time
    override fun hashCode() = key.hashCode() * 31 + time.hashCode()
}
