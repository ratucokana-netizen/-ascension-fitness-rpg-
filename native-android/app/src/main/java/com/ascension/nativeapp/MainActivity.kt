package com.ascension.nativeapp

import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val bg = Color(0xFF05040D)
private val panel = Color(0xFF121024)
private val cyan = Color(0xFFBBA5FF)
private val violet = Color(0xFF7445EA)
private val white = Color(0xFFEAF4FF)
private val muted = Color(0xFF9A94B5)
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
    var clockTick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while(true) { delay(1000);clockTick=SystemClock.elapsedRealtime() } }
    val startedAt = data["session:start"]?.toLongOrNull()
    val elapsed = if(startedAt!=null) ((clockTick-startedAt)/1000).coerceAtLeast(0) else 0L
    val restUntil = data["rest:until"]?.toLongOrNull() ?: 0L
    val restSeconds = ((restUntil-clockTick+999)/1000).coerceAtLeast(0)
    val totalXp = data["xp"]?.toIntOrNull() ?: 0
    val level = 1 + totalXp / 500
    val rank = when { level>=40->"S";level>=25->"A";level>=15->"B";level>=8->"C";level>=4->"D";else->"E" }
    val transition = rememberInfiniteTransition(label="systemPulse")
    val pulse by transition.animateFloat(initialValue=0.35f,targetValue=0.95f,animationSpec=infiniteRepeatable(tween(1450),RepeatMode.Reverse),label="pulse")
    LaunchedEffect(Unit) {
        repeat(12) { boot=it;delay(if(it in 4..7) 95L else 210L) }
        boot=12
        speak("System initialized. Welcome back, Hunter. Rank $rank. Level $level. Today's protocol: $quest. $done of ${plan.size} objectives completed.")
    }
    MaterialTheme(colorScheme=darkColorScheme(primary=cyan,background=bg,surface=panel,onSurface=white)) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bg,Color(0xFF110B27),bg))).padding(horizontal=16.dp)) {
            if(boot<12) {
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp)) {
                        SystemSigil(pulse)
                        Text(if(boot in 4..7) "S Y S T E M   I N T E R F E R E N C E" else "S Y S T E M   A W A K E N I N G",color=if(boot in 4..7) Color(0xFFFF4B8B) else cyan,fontSize=13.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp,textAlign=TextAlign.Center)
                        Text(listOf("ESTABLISHING LINK","IDENTITY CONFIRMED","READING HUNTER PROFILE","SCANNING ATTRIBUTES","SIGNAL DISRUPTION","RECONNECTING","SIGNAL DISRUPTION","OVERRIDING","RESTORING INTERFACE","CALIBRATING QUEST ENGINE","SYNCING PROGRESSION","SYSTEM ONLINE")[boot],color=white,fontSize=11.sp,letterSpacing=1.sp)
                        LinearProgressIndicator(progress={boot/12f},modifier=Modifier.fillMaxWidth(.75f),color=violet,trackColor=panel)
                        if(boot in 4..7) repeat(5) { HorizontalDivider(color=if(it%2==0)violet else Color(0xFFFF4B8B),thickness=(it%3+1).dp) }
                    }
                }
            } else {
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column {
                        Text("A S C E N S I O N",color=white,fontSize=23.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp)
                        Text("THE SYSTEM  /  HUNTER INTERFACE",color=muted,fontSize=9.sp,letterSpacing=1.sp)
                    }
                    Box(Modifier.size(42.dp).border(1.dp,violet,CircleShape).background(panel,CircleShape),contentAlignment=Alignment.Center) {
                        Text(rank,color=cyan,fontSize=21.sp,fontWeight=FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Panel("HUNTER STATUS") {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                        SystemSigil(pulse,92)
                        Column(Modifier.weight(1f)) {
                            Text("RANK $rank  /  ${data["class"] ?: "Vanguard"}".uppercase(),color=cyan,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                            Text("LEVEL $level",color=white,fontSize=30.sp,fontWeight=FontWeight.Black)
                            Text("$totalXp XP  •  ${500-totalXp%500} TO NEXT LEVEL",color=muted,fontSize=10.sp)
                            LinearProgressIndicator(progress={(totalXp%500)/500f},modifier=Modifier.fillMaxWidth().padding(top=9.dp),color=violet,trackColor=Color(0xFF302647))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("QUEST" to quest,"CLEARED" to "$done/${plan.size}","STREAK" to "${data["streak"] ?: "0"}D").forEach { (label,value) ->
                        Column(Modifier.weight(1f).background(panel,RoundedCornerShape(8.dp)).border(1.dp,violet.copy(alpha=.3f),RoundedCornerShape(8.dp)).padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(value,color=white,fontSize=14.sp,fontWeight=FontWeight.Black,maxLines=1)
                            Text(label,color=muted,fontSize=9.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("SYSTEM","TRAIN","QUESTS","METRICS","CLASS").forEach { item ->
                        OutlinedButton(onClick={tab=item},border=BorderStroke(1.dp,if(tab==item)cyan else violet.copy(alpha=.35f)),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(tab==item)violet.copy(alpha=.45f) else panel),contentPadding=PaddingValues(horizontal=12.dp,vertical=2.dp)) {
                            Text(item,fontSize=10.sp,color=if(tab==item)white else muted,fontWeight=FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=45.dp)) {
                    when(tab) {
                        "SYSTEM" -> {
                            item { Panel("DAILY BRIEFING") {
                                Text("PROTOCOL: $quest",color=cyan,fontWeight=FontWeight.Bold)
                                Text("$done / ${plan.size} exercises completed",color=white)
                                Text("CLASS: ${data["class"] ?: "Vanguard"}",color=muted)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick={speak("Hunter. Your current rank is $rank. Today's objective is $quest. Complete your training to earn experience.")}) { Text("◈  INITIATE SYSTEM BRIEFING") }
                            } }
                            item { Panel("PROGRESSION") {
                                Text("XP: $totalXp  •  LEVEL: $level  •  RANK: $rank",color=white)
                                Text("Next level in ${500-totalXp%500} XP",color=muted)
                                Text("Earn 20 XP per completed exercise. Your data is saved on this device.",color=muted)
                            } }
                        }
                        "TRAIN" -> {
                            item { Panel("SESSION CONTROL") {
                                Text(if(startedAt==null) "READY TO BEGIN" else "SESSION ACTIVE  •  %02d:%02d:%02d".format(elapsed/3600,(elapsed/60)%60,elapsed%60),color=cyan,fontWeight=FontWeight.Black,fontSize=17.sp)
                                if(startedAt==null) {
                                    Button(onClick={save("session:start",SystemClock.elapsedRealtime().toString());speak("Training session initiated. Begin your protocol.")}) { Text("▶ BEGIN SESSION") }
                                } else {
                                    Text("$done / ${plan.size} OBJECTIVES CLEARED",color=muted)
                                    Button(onClick={
                                        val minutes=(elapsed/60).coerceAtLeast(1)
                                        save("duration:$today",elapsed.toString())
                                        save("session:start","")
                                        save("rest:until","0")
                                        speak("Training session complete. Duration $minutes minutes. Progress recorded.")
                                    }) { Text("■ END & SAVE SESSION") }
                                }
                                if(restSeconds>0) Text("REST TIMER  •  ${restSeconds}s REMAINING",color=cyan,fontWeight=FontWeight.Bold)
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    listOf(60L,90L,120L).forEach { seconds ->
                                        OutlinedButton(onClick={save("rest:until",(SystemClock.elapsedRealtime()+seconds*1000).toString())}) { Text("${seconds}s REST") }
                                    }
                                }
                                if((data["duration:$today"]?.toLongOrNull() ?: 0L)>0L) Text("LAST SESSION: ${(data["duration:$today"]!!.toLong()/60)} MINUTES",color=muted)
                            } }
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
                                        if(valid) {
                                            save(key,"true")
                                            save("xp",((data["xp"]?.toIntOrNull() ?: 0)+20).toString())
                                            val load=weight.toDoubleOrNull() ?: 0.0
                                            val volume=(1..3).sumOf { (data["reps:$today:${move.name}:$it"]?.toIntOrNull() ?: 0)*load }
                                            save("volume:$today:${move.name}",volume.toString())
                                            val previous=data["best:${move.name}"]?.toDoubleOrNull() ?: 0.0
                                            if(volume>previous) { save("best:${move.name}",volume.toString());speak("New personal record. Objective complete. Twenty experience points awarded.") }
                                            else speak("Objective complete. Twenty experience points awarded.")
                                        }
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
                                Text("TRAINING VOLUME: ${plan.sumOf { data["volume:$today:${it.name}"]?.toDoubleOrNull() ?: 0.0 }.toInt()} KG",color=cyan)
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
    Column(Modifier.fillMaxWidth().border(1.dp,violet.copy(alpha=.52f),RoundedCornerShape(10.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1C1535),panel)),RoundedCornerShape(10.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Text(title,color=cyan,fontWeight=FontWeight.Bold,fontSize=15.sp)
        content()
    }
}
@Composable
private fun MetricField(label:String,key:String,data:Map<String,String>,save:(String,String)->Unit) {
    var value by remember(key) { mutableStateOf(data[key] ?: "") }
    OutlinedTextField(value=value,onValueChange={value=it;save(key,it)},label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
}

@Composable
private fun SystemSigil(pulse:Float,size:Int=190) {
    Box(Modifier.size(size.dp),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center=Offset(this.size.width/2,this.size.height/2)
            val radius=this.size.minDimension*.39f
            drawCircle(violet.copy(alpha=pulse*.75f),radius,center,style=Stroke(width=2.5f))
            drawCircle(cyan.copy(alpha=.55f),radius*.76f,center,style=Stroke(width=1.5f))
            for(i in 0..7) {
                val angle=Math.PI*i/4
                val x=center.x+kotlin.math.cos(angle).toFloat()*radius
                val y=center.y+kotlin.math.sin(angle).toFloat()*radius
                drawLine(cyan.copy(alpha=pulse),center,Offset(x,y),strokeWidth=1.4f)
            }
        }
        Text("◇",fontSize=(size*.31f).sp,color=cyan.copy(alpha=pulse),fontWeight=FontWeight.Black)
    }
}
