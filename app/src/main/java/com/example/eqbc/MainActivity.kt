package com.example.eqbc

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EQBCClientApp() {
    var messages by remember { mutableStateOf(listOf<String>()) }
    var inputText by remember { mutableStateOf("") }
    var writer by remember { mutableStateOf<PrintWriter?>(null) }
    val scope = rememberCoroutineScope()
    
    // Auto-scroll logic
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Hotkeys (Persistent in memory for now)
    val hotkeys = remember { mutableStateListOf("/bcaa //sit", "/bcaa //stand", "/bcaa //follow") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var tempHotkeyText by remember { mutableStateOf("") }

    fun connectToServer(ip: String, port: Int, name: String) {
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

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Hotkey") },
            text = { TextField(value = tempHotkeyText, onValueChange = { tempHotkeyText = it }) },
            confirmButton = {
                TextButton(onClick = {
                    if (editingIndex != -1) hotkeys[editingIndex] = tempHotkeyText
                    showEditDialog = false
                }) { Text("Save") }
            }
        )
    }

    // Main Layout - Use consumeWindowInsets/imePadding to handle keyboard shrinking
    Column(modifier = Modifier.fillMaxSize().padding(8.dp).imePadding()) {
        
        // 1. Output Window (Fills available space, shrinks for keyboard)
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = Color(0xFF1E1E1E), // Dark terminal feel
            shape = RoundedCornerShape(4.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.Bottom // Keeps text anchored to bottom
            ) {
                items(messages) { msg ->
                    Text(
                        text = msg, 
                        color = Color.Green, 
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Row 1: Smaller/Tighter Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("/bct ", "/bca ", "/bcaa ").forEach { label ->
                Button(
                    onClick = { send(label) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(label.trim(), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Row 2: Configurable Hotkeys (Tighter)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            hotkeys.forEachIndexed { index, hk ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
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
                    Text(
                        text = if(hk.length > 8) hk.take(8) + ".." else hk,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Input Area
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                placeholder = { Text("Command...", fontSize = 14.sp) },
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
            ) {
                Text("Send")
            }
        }
    }
}
