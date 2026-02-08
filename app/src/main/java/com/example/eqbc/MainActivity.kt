package com.example.eqbc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

@Composable
fun EQBCClientApp() {
    var messages by remember { mutableStateOf(listOf<String>()) }
    var inputText by remember { mutableStateOf("") }
    var writer by remember { mutableStateOf<PrintWriter?>(null) }
    val scope = rememberCoroutineScope()
    val hotkeys = remember { mutableStateListOf("/bct MyHealer //cast 1", "/bcaa //sit", "/bcaa //stand") }

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
                    withContext(Dispatchers.Main) {
                        messages = messages + line
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages = messages + "Error: ${e.message}"
                }
            }
        }
    }

    fun send(cmd: String) {
        scope.launch(Dispatchers.IO) {
            writer?.println(cmd)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("EQBC Server Output", style = MaterialTheme.typography.labelSmall)
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(messages) { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { send("/bct ") }) { Text("/bct") }
            Button(onClick = { send("/bca ") }) { Text("/bca") }
            Button(onClick = { send("/bcaa ") }) { Text("/bcaa") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            hotkeys.forEach { hk ->
                Button(onClick = { send(hk) }) {
                    Text(hk.take(5)) 
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("connect [IP] [PORT] [NAME]") }
            )
            Button(onClick = {
                if (inputText.startsWith("connect")) {
                    val p = inputText.split(" ")
                    if (p.size >= 4) connectToServer(p[1], p[2].toInt(), p[3])
                } else {
                    send(inputText)
                }
                inputText = ""
            }) {
                Text("Send")
            }
        }
    }
}
