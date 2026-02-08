// ... inside EQBCClientApp ...

    fun sendPacket(text: String, isCommand: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                if (isCommand) {
                    outputStream?.write(9) // ASCII Tab
                }
                // Most servers expect keywords like TELL or MSGALL in UPPERCASE
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
        when (prefix) {
            // Trying UPPERCASE keywords as expected by strict EQBCS builds
            "/bct" -> sendPacket("TELL $cleanInput", true)
            "/bca", "/bcaa" -> sendPacket("MSGALL $cleanInput", true)
            else -> sendPacket(cleanInput, false)
        }
        inputText = ""
    }

    // ... updated Grey Hotkey Logic ...
    onClick = { 
        if (hk.startsWith("connect")) {
            val p = hk.split(" ")
            if (p.size >= 4) connectToServer(p[1], p[2].toInt(), p[3])
        } else if (hk.contains("/bc")) {
            // Convert /bca //stand -> MSGALL //stand
            val cmd = hk.replace("/bct ", "TELL ")
                        .replace("/bca ", "MSGALL ")
                        .replace("/bcaa ", "MSGALL ")
                        .removePrefix("/")
            sendPacket(cmd.trim(), true)
        } else {
            sendPacket(hk, false)
        }
    }
