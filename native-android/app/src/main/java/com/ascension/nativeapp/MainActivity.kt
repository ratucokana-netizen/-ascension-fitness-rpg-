package com.ascension.nativeapp

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

private val bg = Color(0xFF030714)
private val panel = Color(0xFF101A30)
private val cyan = Color(0xFF78DFFF)
private val violet = Color(0xFF9A7CFF)
private val white = Color(0xFFEAF4FF)
private val muted = Color(0xFF9BAFCE)
private data class Move(val name:String, val muscle:String)
private val workouts = mapOf(
    1 to listOf(Move("Bench Press","Chest"),Move("Incline Dumbbell Press","Chest"),Move("Shoulder Press","Shoulders"),Move("Lateral Raise","Shoulders"),Move("Triceps Pushdown","Triceps")),
    3 to listOf(Move("Lat Pulldown","Back"),Move("Seated Cable Row","Back"),Move("Reverse Fly","Rear delts"),Move("Dumbbell Curl","Biceps"),Move("Hammer Curl","Biceps")),
    4 to listOf(Move("Barbell Squat","Quads"),Move("Romanian Deadlift","Hamstrings"),Move("Leg Press","Quads"),Move("Leg Curl","Hamstrings"),Move("Calf Raise","Calves")),
    5 to listOf(Move("Incline Bench Press","Chest"),Move("Chest Supported Row","Back"),Move("Dumbbell Shoulder Press","Shoulders"),Move("Cable Curl","Biceps"),Move("Triceps Extension","Triceps")),
    6 to listOf(Move("Easy Run 30 minutes","Cardio"))
)
private val days = mapOf(1 to "PUSH",3 to "PULL",4 to "LEGS",5 to "UPPER",6 to "RUN")
private val classes = listOf("Vanguard","Berserker","Shadow","Sentinel","Warden")

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts:TextToSpeech? = null
    private var ttsReady = false
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3,7,20)
        window.navigationBarColor = android.graphics.Color.BLACK
        tts = TextToSpeech(this,this)
        val prefs = getSharedPreferences("ascension_native_v2",MODE_PRIVATE)
        setContent {
            val data = remember { mutableStateMapOf<String,String>().apply {
                prefs.all.forEach { (k,v) -> put(k,v.toString()) }
            } }
            fun save(k:String,v:String) { data[k]=v; prefs.edit().putString(k,v).apply() }
            SystemScreen(data,::save) { message ->
                if(ttsReady) tts?.speak(message,TextToSpeech.QUEUE_FLUSH,null,"system")
            }
        }
    }
    override fun onInit(status:Int) {
        if(status==TextToSpeech.SUCCESS) {
            tts?.language=Locale.ENGLISH
            tts?.setPitch(0.78f)
            tts?.setSpeechRate(0.88f)
            ttsReady=true
        }
    }
    override fun onDestroy() { tts?.shutdown();super.onDestroy() }
}

@Composable
private fun SystemScreen(data:Map<String,String>,save:(String,String)->Unit,speak:(String)->Unit) {
    var boot by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf("SYSTEM") }
    val today = remember { LocalDate.now().toString() }
    val day = remember { LocalDate.now().dayOfWeek.value }
    val plan = workouts[day].orEmpty()
    val quest = days[day] ?: "RECOVERY"
    val done = plan.count { data["done:$today:${it.name}"]=="true" }
    val totalXp = data["xp"]?.toIntOrNull() ?: 0
    val level = 1 + totalXp / 500
    val rank = when { level>=40->"S";level>=25->"A";level>=15->"B";level>=8->"C";level>=4->"D";else->"E" }
    LaunchedEffect(Unit) {
        repeat(8) { boot=it;delay(if(it in 3..5) 130L else 300L) }
        boot=8
        speak("System online. Welcome back, Hunter. Rank $rank. Today's protocol: $quest. $done of ${plan.size} objectives completed.")
    }
    MaterialTheme(colorScheme=darkColorScheme(primary=cyan,background=bg,surface=panel,onSurface=white)) {
        Column(Modifier.fillMaxSize().background(bg).padding(horizontal=16.dp)) {
            Spacer(Modifier.height(22.dp))
            Text("A S C E N S I O N",color=cyan,fontSize=23.sp,fontWeight=FontWeight.Black)
            Text("S Y S T E M   2 . 0  /  N A T I V E",color=muted,fontSize=10.sp)
            Spacer(Modifier.height(16.dp))
            if(boot<8) {
                Spacer(Modifier.height(75.dp))
                Text(if(boot in 3..5) "S̷Y̷S̷T̷E̷M̷  I̷N̷T̷E̷R̷F̷E̷R̷E̷N̷C̷E̷" else listOf("INITIALIZING","AUTHENTICATING","LOADING PROFILE","SIGNAL ERROR","RECALIBRATING","SIGNAL RESTORED","SYNCING","SYSTEM ONLINE")[boot],color=if(boot in 3..5) Color(0xFFFF66AD) else cyan,fontSize=20.sp)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(progress={boot/8f},modifier=Modifier.fillMaxWidth())
                if(boot in 3..5) repeat(12) { HorizontalDivider(color=if(it%2==0)violet else cyan,thickness=(it%3+1).dp) }
            } else {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("HUNTER  /  RANK $rank",color=white,fontWeight=FontWeight.Bold)
                    Text("LVL $level  •  $totalXp XP",color=cyan)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress={(totalXp%500)/500f},modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("SYSTEM","TRAIN","QUESTS","METRICS","CLASS").forEach { item ->
                        Button(onClick={tab=item},colors=ButtonDefaults.buttonColors(containerColor=if(tab==item)violet else panel),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)) { Text(item,fontSize=11.sp,color=white) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=45.dp)) {
                    when(tab) {
                        "SYSTEM" -> {
                            item { Panel("DAILY BRIEFING") {
                                Text("PROTOCOL: $quest",color=cyan,fontWeight=FontWeight.Bold)
                                Text("$done / ${plan.size} exercises completed",color=white)
                                Text("CLASS: ${data["class"] ?: "Vanguard"}",color=muted)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick={speak("Hunter. Your current rank is $rank. Today's objective is $quest. Complete your training to earn experience.")}) { Text("▶ SYSTEM BRIEFING") }
                            } }
                            item { Panel("PROGRESSION") {
                                Text("XP: $totalXp  •  LEVEL: $level  •  RANK: $rank",color=white)
                                Text("Next level in ${500-totalXp%500} XP",color=muted)
                                Text("Earn 20 XP per completed exercise. Your data is saved on this device.",color=muted)
                            } }
                        }
                        "TRAIN" -> {
                            item { Panel("TRAINING PROTOCOL / $quest") {
                                Text("3 sets • 8–12 reps • increase load every 2 weeks when all sets reach 12 clean reps.",color=muted)
                                if(plan.isEmpty()) Text("Recovery day. Mobility and walking recommended.",color=white)
                            } }
                            items(plan) { move ->
                                val key="done:$today:${move.name}"
                                val complete=data[key]=="true"
                                Panel(move.name.uppercase()) {
                                    Text(move.muscle,color=muted)
                                    var weight by remember(move.name) { mutableStateOf(data["weight:${move.name}"] ?: "") }
                                    OutlinedTextField(value=weight,onValueChange={weight=it;save("weight:${move.name}",it)},label={Text("Weight (kg)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
                                    for(set in 1..3) {
                                        val repKey="reps:$today:${move.name}:$set"
                                        var reps by remember(move.name,set) { mutableStateOf(data[repKey] ?: "") }
                                        OutlinedTextField(value=reps,onValueChange={reps=it;save(repKey,it)},label={Text("Set $set reps")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
                                    }
                                    Button(enabled=!complete,onClick={
                                        val valid=(1..3).all { (data["reps:$today:${move.name}:$it"]?.toIntOrNull() ?: 0)>0 }
                                        if(valid) { save(key,"true");save("xp",((data["xp"]?.toIntOrNull() ?: 0)+20).toString());speak("Objective complete. Twenty experience points awarded.") }
                                    }) { Text(if(complete)"✓ COMPLETE (+20 XP)" else "COMPLETE EXERCISE") }
                                }
                            }
                        }
                        "QUESTS" -> {
                            item { Panel("DAILY QUEST / $quest") {
                                Text("Complete today's prescribed workout.",color=white)
                                Text("$done / ${plan.size} objectives",color=cyan)
                                LinearProgressIndicator(progress={if(plan.isEmpty())0f else done.toFloat()/plan.size},modifier=Modifier.fillMaxWidth())
                                Text("Exercise rewards are awarded only once per day.",color=muted)
                            } }
                            item { Panel("ACHIEVEMENTS") {
                                Text(if(totalXp>=100)"✓ FIRST 100 XP" else "LOCKED • FIRST 100 XP",color=white)
                                Text(if(totalXp>=500)"✓ FIRST ASCENSION" else "LOCKED • REACH LEVEL 2",color=white)
                                Text(if(totalXp>=2000)"✓ RANK D" else "LOCKED • RANK D",color=white)
                            } }
                        }
                        "METRICS" -> {
                            item { Panel("WEEKLY WEIGH-IN") { MetricField("Bodyweight (kg)","weight:$today",data,save) } }
                            item { Panel("DAILY MACROS") {
                                MetricField("Calories (kcal)","cal:$today",data,save)
                                MetricField("Protein (g)","protein:$today",data,save)
                                MetricField("Carbohydrates (g)","carbs:$today",data,save)
                                MetricField("Fat (g)","fat:$today",data,save)
                            } }
                        }
                        "CLASS" -> {
                            item { Panel("HUNTER CLASS") {
                                Text("Select your specialization. Class selection is saved locally.",color=muted)
                                classes.forEach { hunterClass ->
                                    val selected=(data["class"] ?: "Vanguard")==hunterClass
                                    OutlinedButton(onClick={save("class",hunterClass);speak("$hunterClass class selected.")},modifier=Modifier.fillMaxWidth()) {
                                        Text((if(selected)"◆ " else "◇ ")+hunterClass)
                                    }
                                }
                            } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(title:String,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp,cyan.copy(alpha=.45f),RoundedCornerShape(12.dp)).background(panel,RoundedCornerShape(12.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text(title,color=cyan,fontWeight=FontWeight.Bold,fontSize=15.sp)
        content()
    }
}
@Composable
private fun MetricField(label:String,key:String,data:Map<String,String>,save:(String,String)->Unit) {
    var value by remember(key) { mutableStateOf(data[key] ?: "") }
    OutlinedTextField(value=value,onValueChange={value=it;save(key,it)},label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
}
