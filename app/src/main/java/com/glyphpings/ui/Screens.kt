package com.glyphpings.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.glyphpings.matrix.Matrix
import com.glyphpings.ping.PingCenter
import com.glyphpings.ping.Prefs
import com.glyphpings.ping.Source

private val Red = Color(0xFFD71921)
private val Mono = FontFamily.Monospace

@Composable
fun PingsApp() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var hasAccess by remember { mutableStateOf(hasNotificationAccess(ctx)) }
    LifecycleResumeEffect(Unit) {
        hasAccess = hasNotificationAccess(ctx)
        onPauseOrDispose { }
    }
    val frame by PingCenter.frame.collectAsState()
    val recent by PingCenter.recent.collectAsState()
    val unread by PingCenter.unread.collectAsState()
    val toyActive by PingCenter.toyActive.collectAsState()

    MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, background = Color.Black, surface = Color(0xFF111111))) {
        Column(
            Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text("GLYPH PINGS", fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 3.sp, color = Color.White)
            Text("who's pinging you, on the back of your phone", fontFamily = Mono, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            // ---- setup steps
            Step(
                done = hasAccess, n = 1,
                title = "Allow notification access",
                body = "Lets Glyph Pings read WhatsApp, Snapchat, Instagram and Teams notifications. Nothing leaves your phone.",
                action = "OPEN SETTINGS",
            ) { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            Step(
                done = toyActive, n = 2,
                title = "Choose \"Pings\" as your Glyph Toy",
                body = "Settings → Glyph Interface → Glyph Toys. Also set it as the Always-on toy so pings show with the screen off.",
                action = "OPEN GLYPH TOYS",
            ) { openToys(ctx) }

            // ---- live preview
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                MatrixPreview(frame, Modifier.widthIn(max = 260.dp).fillMaxWidth().padding(12.dp))
                Text("${unread.size} unread", fontFamily = Mono, fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Button(onClick = { PingCenter.test() },
                        colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)) {
                        Text("TEST PING", fontFamily = Mono)
                    }
                    OutlinedButton(onClick = { PingCenter.replay() }) { Text("REPLAY", fontFamily = Mono) }
                    OutlinedButton(onClick = { PingCenter.clearUnread() }) { Text("CLEAR", fontFamily = Mono) }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF222222))

            // ---- options
            Section("WHAT TO SHOW")
            var namesOnly by remember { mutableStateOf(prefs.namesOnly) }
            Toggle("Names only", "Show who pinged you, never the message text", namesOnly) { namesOnly = it; prefs.namesOnly = it }
            var groups by remember { mutableStateOf(prefs.showGroups) }
            Toggle("Group chats", "Show group messages as \"Sender in Group\"", groups) { groups = it; prefs.showGroups = it }

            Section("APPS")
            Source.entries.forEach { s ->
                var on by remember { mutableStateOf(prefs.isEnabled(s)) }
                Toggle(s.label, null, on) { on = it; prefs.setEnabled(s, it) }
            }

            Section("DISPLAY")
            var bg by remember { mutableStateOf(prefs.lightWithoutToy) }
            Toggle("Light up without the toy (experimental)",
                "Tries to show pings even when Pings isn't the active toy. NothingOS may block this — the toy always works.", bg) {
                bg = it; prefs.lightWithoutToy = it
            }
            var speed by remember { mutableIntStateOf(prefs.scrollSpeed) }
            Text("Scroll speed", fontFamily = Mono, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "normal", 2 to "fast").forEach { (v, l) ->
                    FilterChip(selected = speed == v, onClick = { speed = v; prefs.scrollSpeed = v }, label = { Text(l, fontFamily = Mono) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black))
                }
            }
            var bright by remember { mutableFloatStateOf(prefs.brightness) }
            Text("Brightness ${(bright * 100).toInt()}%", fontFamily = Mono, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            Slider(value = bright, onValueChange = { bright = it }, valueRange = 0.2f..1f,
                onValueChangeFinished = { prefs.brightness = bright; PingCenter.applyBrightness(bright) })

            // ---- recent
            Section("RECENT")
            if (recent.isEmpty()) Text("nothing yet", fontFamily = Mono, fontSize = 12.sp, color = Color.Gray)
            recent.take(12).forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (p.group != null) "${p.sender} · ${p.group}" else p.sender, fontFamily = Mono, fontSize = 14.sp, color = Color.White)
                        Text(p.source.label, fontFamily = Mono, fontSize = 11.sp, color = Color.Gray)
                    }
                    Text(DateUtils.getRelativeTimeSpanString(p.time).toString(), fontFamily = Mono, fontSize = 11.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Step(done: Boolean, n: Int, title: String, body: String, action: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .background(Color(0xFF101010), RoundedCornerShape(16.dp)).padding(14.dp),
    ) {
        Text((if (done) "✓ " else "$n. ") + title, fontFamily = Mono, fontWeight = FontWeight.Bold,
            color = if (done) Color.White else Red, fontSize = 14.sp)
        Text(body, fontFamily = Mono, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
        if (!done) OutlinedButton(onClick = onClick) { Text(action, fontFamily = Mono) }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, fontFamily = Mono, fontSize = 12.sp, color = Color.Gray, letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
}

@Composable
private fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontFamily = Mono, fontSize = 14.sp, color = Color.White)
            subtitle?.let { Text(it, fontFamily = Mono, fontSize = 11.sp, color = Color.Gray) }
        }
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Red, checkedThumbColor = Color.White))
    }
}

@Composable
fun MatrixPreview(pixels: IntArray, modifier: Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        val cell = size.minDimension / Matrix.SIZE
        val r = cell * 0.40f
        for (i in 0 until Matrix.COUNT) {
            if (!Matrix.mask[i]) continue
            val v = pixels.getOrElse(i) { 0 }
            val c = Offset((i % Matrix.SIZE + 0.5f) * cell, (i / Matrix.SIZE + 0.5f) * cell)
            if (v <= 0) drawCircle(Color(0xFF1C1C1C), r, c)
            else drawCircle(Color.White.copy(alpha = (0.15f + 0.85f * v / 255f).coerceIn(0f, 1f)), r, c)
        }
    }
}

private fun hasNotificationAccess(ctx: Context) =
    NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

private fun openToys(ctx: Context) {
    val toys = Intent().setComponent(ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { ctx.startActivity(toys) } catch (_: Exception) {
        ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
