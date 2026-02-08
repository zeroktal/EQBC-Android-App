// ... inside EQBCClientApp ...

    fun smartSend(prefix: String) {
        if (inputText.isBlank()) {
            inputText = "$prefix "
            return
        }
        val cleanInput = inputText.trim()
        when (prefix) {
            // Trying BCAST (Broadcast) and TELL (Targeted)
            "/bct" -> sendData("TELL $cleanInput", true)
            "/bca", "/bcaa" -> sendData("BCAST $cleanInput", true)
            else -> sendData(cleanInput, false)
        }
        inputText = ""
    }

    // UPDATED HOTKEYS: Row 2
    val hotkeys = remember {
        val saved = sharedPrefs.getString("HOTKEYS", "connect 192.168.1.3 2112 bob|/bca //stand|/bcaa //follow")
        saved?.split("|")?.toMutableStateList() ?: mutableStateListOf("connect 192.168.1.3 2112 bob", "/bca //stand", "/bcaa //follow")
    }

    // Logic for Row 2 buttons
    // onClick = {
    //    if (hk.startsWith("connect")) { ... }
    //    else if (hk.contains("/bca")) { sendData("BCAST ${hk.removePrefix("/bca ")}", true) }
    //    else if (hk.contains("/bct")) { sendData("TELL ${hk.removePrefix("/bct ")}", true) }
    // }
