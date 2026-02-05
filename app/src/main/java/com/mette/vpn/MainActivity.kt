package com.mette.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mette.vpn.service.MetteVpnService
import com.mette.vpn.utils.ConfigParser
import com.mette.vpn.utils.VpnProfile

val MetteBlue = Color(0xFF2196F3)
val MetteWhite = Color(0xFFFAFAFA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MetteApp() }
    }
}

@Composable
fun MetteApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.fetchAndSortConfigs()
        viewModel.startStatsUpdate()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val navBackStackEntry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(selected = currentRoute == "home", onClick = { navController.navigate("home") }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = currentRoute == "config", onClick = { navController.navigate("config") }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Servers") })
                NavigationBarItem(selected = currentRoute == "logs", onClick = { navController.navigate("logs") }, icon = { Icon(Icons.Default.Code, null) }, label = { Text("Logs") })
                NavigationBarItem(selected = currentRoute == "settings", onClick = { navController.navigate("settings") }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { p ->
        NavHost(navController, "home", Modifier.padding(p)) {
            composable("home") { HomeScreen(viewModel) }
            composable("config") { ConfigScreen(viewModel) }
            composable("logs") { LogScreen(viewModel) }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isConnected by viewModel.isConnected

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val intent = Intent(context, MetteVpnService::class.java).apply {
                putExtra("V2RAY_JSON", viewModel.selectedProfile.value?.fullJson)
            }
            context.startService(intent)
            viewModel.isConnected.value = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MetteWhite).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            val transition = rememberInfiniteTransition(label = "")
            val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "")

            if (!isConnected) {
                Canvas(Modifier.size(180.dp)) {
                    drawArc(Brush.sweepGradient(listOf(Color.Transparent, MetteBlue, Color.Transparent)), angle, 180f, false, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                }
            }

            val btnColor by animateColorAsState(if (isConnected) MetteBlue else Color.White, label = "")

            Button(
                onClick = {
                    if (isConnected) {
                        context.startService(Intent(context, MetteVpnService::class.java).apply { action = "STOP" })
                        viewModel.isConnected.value = false
                    } else {
                        val intent = VpnService.prepare(context)
                        if (intent != null) vpnLauncher.launch(intent)
                        else {
                            val startIntent = Intent(context, MetteVpnService::class.java).apply {
                                putExtra("V2RAY_JSON", viewModel.selectedProfile.value?.fullJson)
                            }
                            context.startService(startIntent)
                            viewModel.isConnected.value = true
                        }
                    }
                },
                modifier = Modifier.size(140.dp).shadow(10.dp, CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = btnColor)
            ) {
                Icon(Icons.Rounded.Bolt, null, Modifier.size(70.dp), tint = if (isConnected) Color.Yellow else MetteBlue)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(if (isConnected) "Connected" else "Tap to Connect", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(viewModel.selectedProfile.value?.name ?: "No Server Selected", color = Color.Gray)
        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("Down", viewModel.downloadSpeed.value)
            StatItem("Up", viewModel.uploadSpeed.value)
            StatItem("Ping", viewModel.pingStats.value)
        }
    }
}

@Composable
fun ConfigScreen(viewModel: MainViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var inputLink by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text("Server List", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MetteBlue)
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.vpnProfiles) { profile ->
                    ConfigItem(
                        p = profile,
                        sel = viewModel.selectedProfile.value == profile,
                        onSel = { viewModel.selectedProfile.value = profile },
                        onDel = { viewModel.vpnProfiles.remove(profile) }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MetteBlue
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Manual Import") },
            text = { TextField(value = inputLink, onValueChange = { inputLink = it }, placeholder = { Text("vless://...") }) },
            confirmButton = {
                Button(onClick = {
                    ConfigParser.parse(inputLink)?.let { viewModel.vpnProfiles.add(0, it) }
                    showDialog = false
                    inputLink = ""
                }) { Text("Add") }
            }
        )
    }
}

@Composable
fun LogScreen(viewModel: MainViewModel) {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Debug Logs", color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = { clipboard.setText(AnnotatedString(viewModel.logBuffer.joinToString("\n"))) }) {
                Icon(Icons.Default.ContentCopy, null, tint = Color.White)
            }
        }
        HorizontalDivider(color = Color.DarkGray)
        LazyColumn {
            items(viewModel.logBuffer) { log ->
                Text(log, color = Color(0xFF00FF00), fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MetteBlue)
        Spacer(Modifier.height(20.dp))
        SettingRow("Auto-Update Servers", "Fetch from Google Sheet on start")
        SettingRow("Background Keep-Alive", "Maintain connection when app is closed")
        SettingRow("Smart Ping", "Auto-test every 15 minutes")
        Spacer(Modifier.weight(1f))
        Text("Version 2.0.0-PRO", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun ConfigItem(p: VpnProfile, sel: Boolean, onSel: () -> Unit, onDel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSel() },
        colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFFE3F2FD) else Color.White),
        border = if (sel) BorderStroke(1.dp, MetteBlue) else null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p.flag, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(p.type, fontSize = 11.sp, color = MetteBlue)
            }
            Text("${if(p.ping > 0) p.ping else "--"} ms", color = if(p.ping in 1..400) Color(0xFF2E7D32) else Color.Red, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDel) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) }
        }
    }
}

@Composable
fun SettingRow(t: String, s: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(t, fontWeight = FontWeight.Medium)
            Text(s, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = true, onCheckedChange = {})
    }
}

@Composable
fun StatItem(l: String, v: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(l, fontSize = 11.sp, color = Color.Gray)
    }
}