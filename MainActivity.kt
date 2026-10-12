package com.nexusroot.manager

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.*
import java.io.IOException

private val Background = Color(0xFF17191C)
private val SurfaceDark = Color(0xFF22252A)
private val SurfaceRaised = Color(0xFF2B2F35)
private val Accent = Color(0xFF8DA5B8)
private val TextMain = Color(0xFFE7E9ED)
private val TextMuted = Color(0xFF969DA7)
private val TerminalGreen = Color(0xFF9AC7A0)
private val httpClient = OkHttpClient()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexusRootApp() }
    }
}

private enum class Screen { HOME, LOGS, SETTINGS }
private enum class Language { RU, EN }

@Composable
private fun NexusRootApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var language by remember { mutableStateOf(Language.RU) }
    var logs by remember { mutableStateOf(listOf(
        "[SYSTEM] NexusRoot initialized",
        "[INFO] UI subsystem ready",
        "[INFO] Waiting for user action"
    )) }
    var kernelStatus by remember { mutableStateOf("Не проверено") }
    var checking by remember { mutableStateOf(false) }
    fun addLog(message: String) { logs = (logs + message).takeLast(200) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, onPrimary = Background, background = Background,
        onBackground = TextMain, surface = SurfaceDark, onSurface = TextMain,
        secondary = TerminalGreen
    )) {
        Scaffold(
            containerColor = Background,
            topBar = { NexusTopBar(language) },
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(screen == Screen.HOME, { screen = Screen.HOME },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(if (language == Language.RU) "Главная" else "Home") },
                        colors = navColors())
                    NavigationBarItem(screen == Screen.LOGS, { screen = Screen.LOGS },
                        icon = { Icon(Icons.Default.List, null) },
                        label = { Text(if (language == Language.RU) "Логи" else "Logs") },
                        colors = navColors())
                    NavigationBarItem(screen == Screen.SETTINGS, { screen = Screen.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text(if (language == Language.RU) "Настройки" else "Settings") },
                        colors = navColors())
                }
            }
        ) { padding ->
            when (screen) {
                Screen.HOME -> HomeScreen(language, kernelStatus, checking, logs, padding,
                    onCheck = {
                        if (!checking) {
                            checking = true
                            kernelStatus = if (language == Language.RU) "Проверка..." else "Checking..."
                            addLog("[NETWORK] Starting kernel support check")
                            checkKernelSupport { success, message ->
                                checking = false
                                kernelStatus = message
                                addLog(if (success) "[NETWORK] $message" else "[ERROR] $message")
                            }
                        }
                    },
                    onOpenLogs = { screen = Screen.LOGS })
                Screen.LOGS -> LogsScreen(language, logs, padding) {
                    logs = listOf("[SYSTEM] Log cleared")
                }
                Screen.SETTINGS -> SettingsScreen(language, padding) { newLanguage ->
                    language = newLanguage
                    kernelStatus = if (newLanguage == Language.RU) "Не проверено" else "Not checked"
                    addLog("[SETTINGS] Language changed to ${newLanguage.name}")
                }
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Accent, selectedTextColor = Accent,
    indicatorColor = SurfaceRaised, unselectedIconColor = TextMuted,
    unselectedTextColor = TextMuted
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexusTopBar(language: Language) {
    TopAppBar(title = {
        Column {
            Text("NEXUSROOT", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 19.sp)
            Text(if (language == Language.RU) "Управление устройством" else "Device management",
                color = TextMuted, fontSize = 11.sp)
        }
    }, colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Background, titleContentColor = TextMain))
}

@Composable
private fun HomeScreen(
    language: Language, kernelStatus: String, checking: Boolean, logs: List<String>,
    padding: PaddingValues, onCheck: () -> Unit, onOpenLogs: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 18.dp)) {
        item { Text(if (language == Language.RU) "Панель состояния" else "Status overview",
            fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TextMain) }
        item { StatusCard(if (language == Language.RU) "Система активна" else "System online",
            if (language == Language.RU) "Интерфейс готов к работе" else "Interface ready",
            "ONLINE", TerminalGreen) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    ParameterCard(if (language == Language.RU) "Платформа" else "Platform",
                        "Android", "Jetpack Compose")
                }
                Box(Modifier.weight(1f)) {
                    ParameterCard(if (language == Language.RU) "Соединение" else "Network",
                        "HTTPS", "OkHttp 4.12")
                }
            }
        }
        item { StatusCard(if (language == Language.RU) "Поддержка ядра" else "Kernel support",
            kernelStatus, if (checking) "WAIT" else "CHECK", Accent) }
        item {
            Button(onClick = onCheck, enabled = !checking, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = TextMain)) {
                Text(if (checking) {
                    if (language == Language.RU) "Проверка..." else "Checking..."
                } else {
                    if (language == Language.RU) "Проверить поддержку" else "Check compatibility"
                })
            }
        }
        item { Text(if (language == Language.RU) "Последние события" else "Recent events",
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        items(logs.takeLast(3).reversed()) { line ->
            Text(line, fontSize = 12.sp, color = TerminalGreen,
                modifier = Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(8.dp)).padding(12.dp))
        }
        item { TextButton(onClick = onOpenLogs) {
            Text(if (language == Language.RU) "Все логи →" else "All logs →")
        } }
    }
}

@Composable
private fun StatusCard(title: String, subtitle: String, value: String, valueColor: Color) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TextMuted, fontSize = 13.sp)
            Text(value, color = valueColor, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMain, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ParameterCard(title: String, value: String, subtitle: String) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = TextMuted, fontSize = 12.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LogsScreen(language: Language, logs: List<String>, padding: PaddingValues, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(if (language == Language.RU) "Системный терминал" else "System terminal",
                fontWeight = FontWeight.Bold, fontSize = 20.sp)
            TextButton(onClick = onClear) { Text(if (language == Language.RU) "Очистить" else "Clear") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxSize().background(Color(0xFF101214), RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { line ->
                Text("> $line", color = if (line.contains("[ERROR]")) Color(0xFFE69A9A) else TerminalGreen,
                    fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun SettingsScreen(language: Language, padding: PaddingValues, onLanguageChange: (Language) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(if (language == Language.RU) "Настройки" else "Settings",
            fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(if (language == Language.RU) "Язык интерфейса" else "Interface language",
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(language == Language.RU, { onLanguageChange(Language.RU) },
                        label = { Text("Русский") })
                    FilterChip(language == Language.EN, { onLanguageChange(Language.EN) },
                        label = { Text("English") })
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("NexusRoot", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Version 1.0.0", color = TextMuted)
                Text("GhostLock UI", color = TextMuted)
            }
        }
    }
}

/** Network placeholder only: HTTP 200 does not prove kernel compatibility. */
private fun checkKernelSupport(onResult: (Boolean, String) -> Unit) {
    val url = "https://raw.githubusercontent.com/OWNER/REPOSITORY/main/kernel-support.json"
    val request = Request.Builder().url(url).get().build()
    httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post {
                onResult(false, "Network error: ${e.message ?: "unknown"}")
            }
        }
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val message = if (it.isSuccessful)
                    "HTTP ${it.code}: endpoint reachable; compatibility not verified"
                else "HTTP ${it.code}: support data unavailable"
                Handler(Looper.getMainLooper()).post { onResult(it.isSuccessful, message) }
            }
        }
    })
}
