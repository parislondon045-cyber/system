package com.example.system // ⚠️ بدّل هاد السطر بسطر package ديالك (اللي كاين فمشروعك)

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// خليها true من بعد: ما تقدرش تشيك المهمة قبل وقتها
const val STRICT = false

// ================= البرنامج =================
// الوقت بالدقائق: t(4, 30) = 04:30 | آخر رقم = XP
// أوقات الصلاة تقريبية، عدّلها حسب مدينتك والفصل
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

// الأحد، الاثنين، الأربعاء، الخميس
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
}

private fun minutesNow(): Int {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
}

private fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

// ================= الواجهة =================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize()) { SystemScreen(store) }
                }
            }
        }
    }
}

@Composable
fun SystemScreen(store: Store) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val tasks = remember {
        (schedule[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)] ?: emptyList())
            .sortedBy { it.start }
    }

    var xp by remember { mutableIntStateOf(store.xp) }
    var done by remember { mutableStateOf(store.getSet("done_$today")) }
    var missed by remember { mutableStateOf(store.getSet("missed_$today")) }
    var nowMin by remember { mutableIntStateOf(minutesNow()) }

    // كل 30 ثانية: المهام اللي فات وقتها وما تشاكتش = عقوبة (تخسر XP المهمة)
    LaunchedEffect(Unit) {
        while (true) {
            nowMin = minutesNow()
            val late = tasks.filter { nowMin >= it.end && it.id !in done && it.id !in missed }
            for (task in late) {
                store.add("missed_$today", task.id)
                missed = missed + task.id
                xp = (xp - task.xp).coerceAtLeast(0)
                store.xp = xp
            }
            delay(30_000)
        }
    }

    val level = xp / 100 + 1
    val ranks = listOf("E", "D", "C", "B", "A", "S")
    val rank = ranks[minOf((level - 1) / 5, ranks.lastIndex)]

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("النظام  •  رتبة $rank  •  مستوى $level", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("XP: $xp", fontSize = 14.sp)
        LinearProgressIndicator(
            progress = { (xp % 100) / 100f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks) { task ->
                val isDone = task.id in done
                val isMissed = task.id in missed
                val color = when {
                    isDone -> Color(0xFF2E7D32)
                    isMissed -> Color(0xFFC62828)
                    nowMin >= task.start -> Color(0xFFF9A825)
                    else -> Color.Gray
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDone,
                        enabled = !isDone && (!STRICT || nowMin >= task.start),
                        onCheckedChange = {
                            // إلا كملتها متأخر تاخذ نص الـ XP
                            val gain = if (isMissed) task.xp / 2 else task.xp
                            store.add("done_$today", task.id)
                            done = done + task.id
                            xp += gain
                            store.xp = xp
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Medium)
                        Text("${fmt(task.start)} – ${fmt(task.end)}  •  ${task.xp} XP", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
