package com.ascension.nativeapp

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private val violet = Color(0xFFAA58FF)
private val background = Color(0xFF090613)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var speaker: TextToSpeech? = null
    private var ready = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = TextToSpeech(this, this)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { SystemApp { speakBriefing() } }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            speaker?.language = Locale.ENGLISH
            speaker?.setPitch(0.82f)
            speaker?.setSpeechRate(0.90f)
            ready = true
        }
    }
    private fun speakBriefing() {
        if (ready) speaker?.speak(
            "System online. Welcome back, Hunter. Your status is ready. Open your daily quests to begin training.",
            TextToSpeech.QUEUE_FLUSH, null, "daily-brief"
        )
    }
    override fun onDestroy() {
        speaker?.stop()
        speaker?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun SystemApp(onWelcome: () -> Unit) {
    var booted by remember { mutableStateOf(false) }
    var stage by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf("STATUS") }
    var questDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        repeat(6) { index ->
            stage = index
            delay(if (index == 2 || index == 3) 650L else 850L)
        }
        booted = true
        onWelcome()
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = violet, background = background, surface = Color(0xFF191026))) {
        Column(
            Modifier.fillMaxSize().background(background).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(42.dp))
            Text("A S C E N S I O N", fontSize = 23.sp, fontWeight = FontWeight.Black, color = violet)
            Text("S Y S T E M   2 . 0", fontSize = 12.sp, color = Color.LightGray)
            Spacer(Modifier.height(42.dp))
            if (!booted) {
                Text(
                    when(stage) {
                        0 -> "INITIALIZING SYSTEM"
                        1 -> "AUTHENTICATING HUNTER"
                        2,3 -> "S̷Y̷S̷T̷E̷M̷  I̷N̷T̷E̷R̷F̷E̷R̷E̷N̷C̷E̷"
                        4 -> "RECALIBRATING"
                        else -> "SYSTEM ONLINE"
                    }, color = if (stage in 2..3) Color(0xFFFF5EAF) else violet,
                    fontSize = 17.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(progress = { (stage + 1) / 6f }, modifier = Modifier.fillMaxWidth(), color = violet)
                if (stage in 2..3) {
                    repeat(9) { i ->
                        Spacer(Modifier.height((i % 3 + 2).dp))
                        HorizontalDivider(thickness = (i % 3 + 1).dp, color = if (i % 2 == 0) violet else Color.Cyan)
                    }
                }
            } else {
                Text("WELCOME BACK, HUNTER", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("STATUS", "QUESTS", "TRAINING").forEach { name ->
                        OutlinedButton(onClick = { tab = name }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Text(name, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Column(Modifier.fillMaxWidth().border(1.dp, violet, RoundedCornerShape(10.dp)).padding(18.dp)) {
                    when(tab) {
                        "STATUS" -> {
                            Text("HUNTER STATUS", color = violet, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("Level: Awaiting saved profile", color = Color.White)
                            Text("XP: Awaiting saved profile", color = Color.White)
                            Text("Streak: Awaiting saved profile", color = Color.White)
                        }
                        "QUESTS" -> {
                            Text("DAILY QUEST", color = violet, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("Open your assigned workout and complete today's objective.", color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { questDone = !questDone }) {
                                Text(if (questDone) "QUEST MARKED COMPLETE" else "MARK QUEST COMPLETE")
                            }
                        }
                        else -> {
                            Text("TRAINING PROTOCOL", color = violet, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("Native workout logger integration is next.", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
