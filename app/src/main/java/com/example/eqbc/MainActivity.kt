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
import java.io.PrintWriter
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
    var writer by remember { mutableStateOf<PrintWriter?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val initialHotkeys = remember {
        val saved = sharedPrefs.getString("HOTKEYS", "/bcaa //sit|/bcaa //stand|/bcaa //follow")
        saved?.split("|")?.toMutableStateList() ?: mutableStateListOf("/bcaa //sit", "/bcaa //stand", "/bcaa //follow")
    }
    val hotkeys = initialHotkeys

    var showEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var tempHotkeyText by remember { mutableStateOf("") }

    fun saveHotkeys(list: List<String>) {
        sharedPrefs.edit().putString("HOTKEYS", list.joinToString("|")).apply()
    }

    fun connectToServer(ip: String, port: Int, name: String) {
        sharedPrefs.edit().apply {
            putString("LAST_IP", ip)
            putInt("LAST_PORT", port)
            putString("LAST_NAME", name)
        }.apply()

        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket(ip, port)
                val out = PrintWriter(s.getOutputStream(), true)
                writer = out
                out.print("LOGIN=$name;\n")
                out.flush()
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    withContext(Dispatchers.Main) { messages = messages + line }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { messages = messages + "Error: ${e.message}" }
            }
        }
    }

    fun send(cmd: String) {
        scope.launch(Dispatchers.IO) { writer?.println(cmd) }
    }

    // New Smart Send helper for row 1 buttons
    fun smartSend(prefix: String) {
        val finalCmd = if (inputText.isNotBlank()) {
            val combined = "$prefix $inputText".trim()
            inputText = "" // Clear the input box after sending
            combined
        } else {
            prefix.trim()
        }
        send(finalCmd)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EQBC Mobile", fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Connect to Last") },
                            onClick = {
                                menuExpanded = false
                                val ip = sharedPrefs.getString("LAST_IP", "")
                                val port = sharedPrefs.getInt("LAST_PORT", 2112)
                                val name = sharedPrefs.getString("LAST_NAME", "")
                                if (!ip.isNullOrEmpty() && !name.isNullOrEmpty()) connectToServer(ip, port, name)
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
                        Text(text = msg, color = Color.Green, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 1: Fixed Buttons (Now combines with Input Text)
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

            // Row 2: Configurable Hotkeys
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                hotkeys.forEachIndexed { index, hk ->
                    Box(
                        modifier = Modifier
                            .weight(1f).height(36.dp)
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                            .combinedClickable(
                                onClick = { send(hk) },
                                onLongClick = {
                                    editingIndex = index
                                    tempHotkeyText = hk
                                    showEditDialog = true
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if(hk.length > 8) hk.take(8) + ".." else hk, color = Color.White, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Input Area
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
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
                            send(inputText)
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
                        saveHotkeys(hotkeys)
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
