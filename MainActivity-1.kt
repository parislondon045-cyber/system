package com.example.system

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// خليها true من بعد: ما تقدرش تشيك المهمة قبل وقتها
const val STRICT = false

// ================= البرنامج =================
// t(4, 30) = 04:30 | آخر رقم = XP | أوقات الصلاة تقريبية، عدّلها حسب مدينتك
data class Task(val id: String, val title: String, val start: Int, val end: Int, val xp: Int = 10)

private fun t(h: Int, m: Int = 0) = h * 60 + m

private val wake = Task("wake", "رتب السرير + أذكار الاستيقاظ + وضوء وصلاة", t(4), t(4, 30), 15)
private val quran = Task("quran", "المدرسة القرآنية / القرآن", t(4, 30), t(6), 20)
private val fajr = Task("fajr", "صلاة الصبح في الجامع", t(6), t(6, 30), 15)
private val run = Task("run", "الجري", t(6, 30), t(7), 15)
private val adhkarAm = Task("adhkar_am", "أذكار الصباح", t(7), t(7, 30))
private val school1 = Task("school1", "الدراسة (الصباح)", t(8), t(12), 20)
private val dhuhr = Task("dhuhr", "صلاة الظهر", t(12, 30), t(13, 30), 15)
private val school2 = Task("school2", "الدراسة (العشية)", t(14), t(17), 20)
private val asr = Task("asr", "صلاة العصر + أذكار المساء", t(17), t(17, 45), 15)
private val maghrib = Task("maghrib", "صلاة المغرب", t(18, 15), t(19), 15)
private val isha = Task("isha", "صلاة العشاء", t(19, 45), t(20, 30), 15)
private val studyEve = Task("study_eve", "الدراسة (المساء)", t(19), t(21, 30), 25)
private val sleep = Task("sleep", "العشاء + أذكار النوم + النوم", t(21, 30), t(22, 15), 15)

private val standard = listOf(
    wake, quran, fajr, run, adhkarAm, school1, dhuhr, school2, asr,
    maghrib, studyEve, isha, sleep
)

private val tuesday = listOf(
    wake, quran, fajr, run, adhkarAm, school1, dhuhr,
    Task("class_tue", "حصة الدراسة", t(14), t(15), 20),
    asr.copy(start = t(15, 15), end = t(16, 45)),
    Task("training_tue", "التدريب", t(17), t(19), 25),
    Task("study_tue", "الدراسة (ساعة)", t(19), t(20), 20),
    maghrib, isha, sleep
)

private val friday = listOf(
    Task("wake_fri", "رتب السرير + أذكار الاستيقاظ + وضوء وصلاة", t(5), t(6), 15),
    fajr, run, adhkarAm,
    Task("lecture_fri", "ليكور", t(10), t(12), 20),
    dhuhr,
    Task("study_fri", "الدراسة", t(14), t(16), 25),
    Task("training_fri", "التدريب", t(16), t(17), 25),
    asr, maghrib, studyEve, isha, sleep
)

private val saturday = listOf(
    wake, quran, fajr, run, adhkarAm,
    Task("study_sat_am", "الدراسة (الصباح)", t(8), t(10), 25),
    dhuhr,
    Task("study_sat_pm", "الدراسة (العشية)", t(14), t(16), 25),
    Task("training_sat", "التدريب", t(16), t(17), 25),
    asr, maghrib, isha,
    Task("study_sat_eve", "الدراسة (المساء)", t(20), t(21, 30), 25),
    sleep
)

val schedule: Map<Int, List<Task>> = mapOf(
    Calendar.SUNDAY to standard,
    Calendar.MONDAY to standard,
    Calendar.TUESDAY to tuesday,
    Calendar.WEDNESDAY to standard,
    Calendar.THURSDAY to standard,
    Calendar.FRIDAY to friday,
    Calendar.SATURDAY to saturday,
)

// ================= التخزين =================
class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("system", Context.MODE_PRIVATE)

    var xp: Int
        get() = p.getInt("xp", 0)
        set(v) {
            p.edit().putInt("xp", v.coerceAtLeast(0)).apply()
        }

    fun getSet(key: String): Set<String> = p.getStringSet(key, emptySet()) ?: emptySet()
    fun add(key: String, id: String) {
        p.edit().putStringSet(key, getSet(key) + id).apply()
    }

    fun stat(c: String) = p.getInt("stat_$c", 0)
    fun addStat(c: String) {
        p.edit().putInt("stat_$c", stat(c) + 1).apply()
    }
}

// ================= الثيم =================
private val Blue = Color(0xFF3DA5FF)
private val Cyan = Color(0xFF8FE3FF)
private val Panel = Color(0xE60A1830)
private val Dim = Color(0xFF6C86A8)
private val Red = Color(0xFFFF4D5E)
private val Gold = Color(0xFFFFD166)
private val Mono = FontFamily.Monospace

private val ranks = listOf("E", "D", "C", "B", "A", "S")
private val rankColors = listOf(Color(0xFF9CA3AF), Color(0xFF34D399), Cyan, Color(0xFF8B7CFF), Gold, Red)
private val dayNames = listOf("", "أحد", "إثن", "ثلا", "أرب", "خمي", "جمع", "سبت")

class DayStat(val label: String, val pct: Float)
class Pop(val title: String, val body: String, val color: Color)

private fun minutesNow(): Int {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
}

private fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

// STR تدريب | AGI جري | INT دراسة | VIT نوم واستيقاظ | SENSE عبادة وأذكار
private fun catOf(t: Task) = when {
    t.id.startsWith("training") -> "STR"
    t.id.startsWith("run") -> "AGI"
    t.id.startsWith("study") || t.id.startsWith("school") ||
        t.id.startsWith("class") || t.id.startsWith("lecture") -> "INT"
    t.id.startsWith("wake") || t.id.startsWith("sleep") -> "VIT"
    else -> "SENSE"
}

private fun dayStat(store: Store, daysAgo: Int): DayStat {
    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
    val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    val dow = c.get(Calendar.DAY_OF_WEEK)
    val total = (schedule[dow]?.size ?: 1).coerceAtLeast(1)
    return DayStat(dayNames[dow], store.getSet("done_$key").size.toFloat() / total)
}

// أيام متتالية بإنجاز 70% فما فوق
private fun calcStreak(store: Store): Int {
    var n = 0
    for (i in 0..60) {
        if (dayStat(store, i).pct >= 0.7f) n++ else if (i > 0) break
    }
    return n
}

@Composable
fun Mn(
    text: String, size: TextUnit = 13.sp, color: Color = Cyan, weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier, align: TextAlign? = null
) {
    Text(
        text, modifier = modifier, color = color, fontSize = size, fontWeight = weight,
        fontFamily = Mono, textAlign = align, style = TextStyle(textDirection = TextDirection.Ltr)
    )
}

// نافذة النظام: إطار أزرق مع توهج
@Composable
fun Win(title: String, color: Color = Blue, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().drawBehind {
            val r = CornerRadius(4.dp.toPx())
            drawRoundRect(Panel, cornerRadius = r)
            for (i in 1..3) drawRoundRect(color.copy(alpha = 0.12f), cornerRadius = r, style = Stroke(i * 3.dp.toPx()))
            drawRoundRect(color, cornerRadius = r, style = Stroke(1.5.dp.toPx()))
        }.padding(16.dp)
    ) {
        Mn("[ $title ]", 15.sp, color, FontWeight.Bold, Modifier.fillMaxWidth(), TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = 0.5f)))
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun Bar(p: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(10.dp).border(1.dp, color.copy(alpha = 0.6f)).padding(2.dp)) {
        Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

// ================= الواجهة =================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val store = Store(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Color.Black, surface = Color.Black)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(Modifier.fillMaxSize(), color = Color.Black, contentColor = Color.White) {
                        SystemApp(store)
                    }
                }
            }
        }
    }
}

@Composable
fun SystemApp(store: Store) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val tasks = remember {
        (schedule[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)] ?: emptyList()).sortedBy { it.start }
    }
    var xp by remember { mutableIntStateOf(store.xp) }
    var done by remember { mutableStateOf(store.getSet("done_$today")) }
    var missed by remember { mutableStateOf(store.getSet("missed_$today")) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var tab by remember { mutableIntStateOf(1) }
    var popup by remember { mutableStateOf<Pop?>(Pop("SYSTEM", "A DAILY QUEST HAS ARRIVED.", Blue)) }
    val stats = remember {
        mutableStateMapOf<String, Int>().apply {
            listOf("STR", "AGI", "VIT", "INT", "SENSE").forEach { put(it, store.stat(it)) }
        }
    }
    val nowMin = remember(nowMs) { minutesNow() }

    // كل ثانية: المهام اللي فات وقتها وما تشاكتش = عقوبة
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            val nm = minutesNow()
            val late = tasks.filter { nm >= it.end && it.id !in done && it.id !in missed }
            if (late.isNotEmpty()) {
                val loss = late.sumOf { it.xp }
                late.forEach { store.add("missed_$today", it.id) }
                missed = missed + late.map { it.id }
                xp = (xp - loss).coerceAtLeast(0)
                store.xp = xp
                popup = Pop("PENALTY", "${late.size} QUEST(S) FAILED   -$loss EXP", Red)
            }
            delay(1000)
        }
    }
    LaunchedEffect(popup) {
        if (popup != null) {
            delay(3000)
            popup = null
        }
    }

    val streak = remember(done.size) { calcStreak(store) }
    val check: (Task) -> Unit = { task ->
        // إلا كملتها متأخر تاخذ نص الـ XP
        val gain = if (task.id in missed) task.xp / 2 else task.xp
        val oldLv = xp / 100
        store.add("done_$today", task.id)
        done = done + task.id
        xp += gain
        store.xp = xp
        val c = catOf(task)
        store.addStat(c)
        stats[c] = store.stat(c)
        popup = if (xp / 100 > oldLv) Pop("LEVEL UP!", "LEVEL ${xp / 100 + 1}", Gold)
        else Pop("QUEST COMPLETE", "+$gain EXP   $c +1", Cyan)
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF01040C), Color(0xFF071A33))))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            bottomBar = {
                Row(Modifier.fillMaxWidth().background(Color(0xF0050B18)).navigationBarsPadding()) {
                    listOf("STATUS", "QUEST", "LOG").forEachIndexed { i, name ->
                        Box(
                            Modifier.weight(1f).clickable { tab = i }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Mn(
                                name, 14.sp, if (tab == i) Cyan else Dim,
                                if (tab == i) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad)) {
                when (tab) {
                    0 -> StatusScreen(xp, streak, stats, missed.size)
                    1 -> QuestScreen(tasks, done, missed, nowMin, nowMs, check)
                    else -> LogScreen(store, xp, done.size)
                }
            }
        }
        AnimatedVisibility(
            visible = popup != null,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp),
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = ExitTransition.None
        ) {
            popup?.let { p ->
                Win("NOTIFICATION", p.color) {
                    Mn(p.title, 20.sp, p.color, FontWeight.Bold, Modifier.fillMaxWidth(), TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Mn(p.body, 13.sp, Color.White, FontWeight.Normal, Modifier.fillMaxWidth(), TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun StatusScreen(xp: Int, streak: Int, stats: Map<String, Int>, penalties: Int) {
    val level = xp / 100 + 1
    val ri = minOf((level - 1) / 5, ranks.lastIndex)
    val title = when {
        streak >= 14 -> "SHADOW MONARCH"
        streak >= 7 -> "IRON WILL"
        streak >= 3 -> "DISCIPLINED"
        else -> "BEGINNER"
    }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Win("STATUS") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Mn("LEVEL", 12.sp, Dim)
                    Mn("$level", 56.sp, Color.White, FontWeight.Bold)
                }
                Column(Modifier.weight(1.5f)) {
                    Mn("JOB: PLAYER")
                    Mn("TITLE: $title")
                    Mn("RANK: ${ranks[ri]}", 14.sp, rankColors[ri], FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Mn("EXP  ${xp % 100} / 100", 12.sp, Dim)
            Spacer(Modifier.height(4.dp))
            Bar((xp % 100) / 100f, Blue)
            Spacer(Modifier.height(16.dp))
            listOf("STR", "AGI", "VIT", "INT", "SENSE").forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Mn(s, 15.sp, Cyan, modifier = Modifier.weight(1f))
                    Mn("${10 + (stats[s] ?: 0)}", 15.sp, Color.White, FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Mn("PENALTY TODAY: $penalties", 12.sp, if (penalties > 0) Red else Dim)
        }
    }
}

@Composable
fun QuestScreen(
    tasks: List<Task>, done: Set<String>, missed: Set<String>,
    nowMin: Int, nowMs: Long, onCheck: (Task) -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(0.35f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "a")
    val c = Calendar.getInstance().apply { timeInMillis = nowMs }
    val left = 86400 - (c.get(Calendar.HOUR_OF_DAY) * 3600 + c.get(Calendar.MINUTE) * 60 + c.get(Calendar.SECOND))

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Win("DAILY QUEST") {
                Mn("GOAL: COMPLETE ALL OBJECTIVES", 13.sp, Cyan)
                Spacer(Modifier.height(6.dp))
                Mn("TIME LEFT  %02d:%02d:%02d".format(left / 3600, left % 3600 / 60, left % 60), 13.sp, Gold)
                Spacer(Modifier.height(6.dp))
                Mn("COMPLETED  ${done.size} / ${tasks.size}", 13.sp, Color.White)
            }
        }
        items(tasks) { task ->
            val isDone = task.id in done
            val isMissed = task.id in missed
            val active = !isDone && nowMin >= task.start && nowMin < task.end
            val col = when {
                isDone -> Cyan
                isMissed -> Red
                active -> Gold
                else -> Dim
            }
            val shape = RoundedCornerShape(4.dp)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(Panel)
                        .border(if (active) 1.5.dp else 1.dp, col.copy(alpha = if (active) pulse else 0.5f), shape)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(26.dp)
                            .border(1.5.dp, col, RoundedCornerShape(3.dp))
                            .background(if (isDone) col.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable(enabled = !isDone && (!STRICT || nowMin >= task.start)) { onCheck(task) },
                        contentAlignment = Alignment.Center
                    ) { if (isDone) Mn("✓", 16.sp, col, FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = if (isDone) Dim else Color.White, fontSize = 15.sp)
                        Mn("${fmt(task.start)} – ${fmt(task.end)}", 12.sp, col)
                    }
                    Mn(if (isDone) "[1/1]" else if (isMissed) "FAILED" else "[0/1]", 12.sp, col, FontWeight.Bold)
                }
            }
        }
        item {
            Mn("CAUTION! FAILURE TO COMPLETE THE DAILY QUEST WILL RESULT IN A PENALTY.", 11.sp, Red)
        }
    }
}

@Composable
fun LogScreen(store: Store, xp: Int, doneCount: Int) {
    val week = remember(doneCount) { (6 downTo 0).map { dayStat(store, it) } }
    val avg = (week.map { it.pct }.average() * 100).toInt()
    val best = (week.maxOf { it.pct } * 100).toInt()
    val streak = remember(doneCount) { calcStreak(store) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Win("LOG") {
            Mn("COMPLETION - LAST 7 DAYS", 12.sp, Dim)
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                val w = size.width
                val h = size.height
                val pad = 10.dp.toPx()
                val cell = w / week.size
                fun px(i: Int) = cell * (i + 0.5f)
                fun py(p: Float) = h - pad - p * (h - 2 * pad)

                listOf(0f, 0.5f, 1f).forEach {
                    drawLine(Dim.copy(alpha = 0.3f), Offset(0f, py(it)), Offset(w, py(it)), 1.dp.toPx())
                }
                val line = Path()
                week.forEachIndexed { i, d ->
                    if (i == 0) line.moveTo(px(0), py(d.pct))
                    else {
                        val mx = (px(i - 1) + px(i)) / 2
                        line.cubicTo(mx, py(week[i - 1].pct), mx, py(d.pct), px(i), py(d.pct))
                    }
                }
                val fill = Path()
                fill.addPath(line)
                fill.lineTo(px(week.size - 1), h)
                fill.lineTo(px(0), h)
                fill.close()
                drawPath(fill, brush = Brush.verticalGradient(listOf(Blue.copy(alpha = 0.35f), Color.Transparent)))
                drawPath(line, color = Blue, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                week.forEachIndexed { i, d ->
                    drawCircle(Color(0xFF071A33), 7.dp.toPx(), Offset(px(i), py(d.pct)))
                    drawCircle(Cyan, 4.dp.toPx(), Offset(px(i), py(d.pct)))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                week.forEach {
                    Text(
                        it.label, modifier = Modifier.weight(1f), color = Dim,
                        fontSize = 11.sp, textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            listOf("AVERAGE" to "$avg%", "BEST DAY" to "$best%", "STREAK" to "$streak", "TOTAL EXP" to "$xp")
                .forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Mn(k, 14.sp, Cyan, modifier = Modifier.weight(1f))
                        Mn(v, 14.sp, Color.White, FontWeight.Bold)
                    }
                }
        }
    }
}
