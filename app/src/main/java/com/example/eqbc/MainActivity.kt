package com.example.eqbc

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EQBCClientApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EQBCClientApp() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("EQBC_PREFS", Context.MODE_PRIVATE) }
    
    var messages by remember { mutableStateOf(listOf<String>()) }
    var inputText by remember { mutableStateOf("") }
    var outputStream by remember { mutableStateOf<OutputStream?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val hotkeys = remember {
        val saved = sharedPrefs.getString("HOTKEYS", "connect 192.168.1.3 2112 bob|/bca //stand|/bcaa //follow")
        saved?.split("|")?.toMutableStateList() ?: mutableStateListOf("connect 192.168.1.3 2112 bob", "/bca //stand", "/bcaa //follow")
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var tempHotkeyText by remember { mutableStateOf("") }

    fun connectToServer(ip: String, port: Int, name: String) {
        sharedPrefs.edit().apply {
            putString("LAST_IP", ip)
            putInt("LAST_PORT", port)
            putString("LAST_NAME", name)
        }.apply()

        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket(ip, port)
                outputStream = s.getOutputStream()
                outputStream?.write("LOGIN=$name;\n".toByteArray())
                outputStream?.flush()
                
                isConnected = true
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    withContext(Dispatchers.Main) { messages = messages + line }
                }
            } catch (e: Exception) {
                isConnected = false
                withContext(Dispatchers.Main) { messages = messages + "Error: ${e.message}" }
            }
        }
    }

    fun sendData(text: String, useTab: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                if (useTab) outputStream?.write(9) 
                outputStream?.write((text + "\n").toByteArray())
                outputStream?.flush()
            } catch (e: Exception) { }
        }
    }

    fun smartSend(prefix: String) {
        if (inputText.isBlank()) {
            inputText = "$prefix "
            return
        }
        val cleanInput = inputText.trim()
        // Switching to Tab + Literal Slash Command (Common for standalone EQBCS)
        when (prefix) {
            "/bct" -> sendData("bct $cleanInput", true)
            "/bca" -> sendData("bca $cleanInput", true)
            "/bcaa" -> sendData("bcaa $cleanInput", true)
            else -> sendData(cleanInput, false)
        }
        inputText = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("EQBC Mobile", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(10.dp).background(if(isConnected) Color.Green else Color.Red, RoundedCornerShape(5.dp)))
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Connect to Last") },
                            onClick = {
                                menuExpanded = false
                                val savedIp = sharedPrefs.getString("LAST_IP", "") ?: ""
                                val savedPort = sharedPrefs.getInt("LAST_PORT", 2112)
                                val savedName = sharedPrefs.getString("LAST_NAME", "bob") ?: "bob"
                                if (savedIp.isNotEmpty()) connectToServer(savedIp, savedPort, savedName)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Output") },
                            onClick = { menuExpanded = false; messages = emptyList() }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(8.dp).imePadding()) {
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(4.dp)
            ) {
                LazyColumn(state = listState, modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.Bottom) {
                    items(messages) { msg ->
                        Text(text = msg, color = Color.Green, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("/bct", "/bca", "/bcaa").forEach { label ->
                    Button(
                        onClick = { smartSend(label) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) { Text(label, fontSize = 12.sp) }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                hotkeys.forEachIndexed { index, hk ->
                    Box(
                        modifier = Modifier
                            .weight(1f).height(36.dp)
                            .background(Color.Gray, RoundedCornerShape(4.dp))
                            .combinedClickable(
                                onClick = { 
                                    if (hk.startsWith("connect")) {
                                        val p = hk.split(" ")
                                        if (p.size >= 4) connectToServer(p[1], p[2].toInt(), p[3])
                                    } else {
                                        // If hotkey has /bc, send it with a Tab
                                        val isCmd = hk.contains("/bc")
                                        sendData(if(isCmd) hk.removePrefix("/") else hk, isCmd)
                                    }
                                },
                                onLongClick = {
                                    editingIndex = index
                                    tempHotkeyText = hk
                                    showEditDialog = true
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if(hk.length > 10) hk.take(10) + ".." else hk, color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Command...", fontSize = 12.sp) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        if (inputText.startsWith("connect")) {
                            val p = inputText.split(" ")
                            if (p.size >= 4) connectToServer(p[1], p[2].toInt(), p[3])
                        } else {
                            // Secret Probe: If you type \t manually, it sends as Command
                            val isManualCmd = inputText.startsWith("\\t")
                            val textToSend = if(isManualCmd) inputText.removePrefix("\\t") else inputText
                            sendData(textToSend, isManualCmd)
                        }
                        inputText = ""
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(4.dp)
                ) { Text("Send") }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Hotkey") },
            text = { TextField(value = tempHotkeyText, onValueChange = { tempHotkeyText = it }) },
            confirmButton = {
                TextButton(onClick = {
                    if (editingIndex != -1) {
                        hotkeys[editingIndex] = tempHotkeyText
                        sharedPrefs.edit().putString("HOTKEYS", hotkeys.joinToString("|")).apply()
                    }
                    showEditDialog = false
                }) { Text("Save") }
            }
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
}
