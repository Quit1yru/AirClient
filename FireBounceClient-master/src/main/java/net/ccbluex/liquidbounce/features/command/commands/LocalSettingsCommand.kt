package net.ccbluex.liquidbounce.features.command.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.FireBounce.clientVersionText
import net.ccbluex.liquidbounce.config.SettingsUtils
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.file.FileManager.settingsDir
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object LocalSettingsCommand : Command("localsettings", "localsetting", "config","localconfig") {

    private val SECURE_RANDOM = SecureRandom()
    private const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    private val ENCRYPTION_KEY = generateFixedKey("FireBounce-$clientVersionText")

    private fun generateFixedKey(input: String): SecretKeySpec {
        val keyMaterial = input.toByteArray(Charsets.UTF_8)
        val keyData = ByteArray(32)

        for (i in keyData.indices) {
            keyData[i] = (keyMaterial[i % keyMaterial.size].toInt() xor (i * 31)).toByte()
        }

        return SecretKeySpec(keyData, "AES")
    }

    private fun generateKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec)
        return SecretKeySpec(key.encoded, "AES")
    }

    private fun encryptWithPassword(data: String, password: String, author: String = "Unknown"): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SECURE_RANDOM.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH)
        SECURE_RANDOM.nextBytes(iv)

        val key = generateKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val payload = "$author|$clientVersionText|$data"
        val encrypted = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))

        val result = ByteArray(salt.size + iv.size + encrypted.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(encrypted, 0, result, salt.size + iv.size, encrypted.size)

        return result
    }

    private fun decryptWithPassword(encryptedData: ByteArray, password: String): Triple<String, String, String> {
        val salt = encryptedData.copyOfRange(0, SALT_LENGTH)
        val iv = encryptedData.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val actualEncryptedData = encryptedData.copyOfRange(SALT_LENGTH + IV_LENGTH, encryptedData.size)

        val key = generateKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        val decrypted = cipher.doFinal(actualEncryptedData)
        val payload = String(decrypted, Charsets.UTF_8)

        val author = payload.substringBefore("|")
        val dataWithVersion = payload.substringAfter("|")
        val version = dataWithVersion.substringBefore("|")
        val realData = dataWithVersion.substringAfter("|")
        return Triple(realData, author, version)
    }

    private fun encrypt(data: String, author: String = "Unknown"): ByteArray {
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val iv = ByteArray(IV_LENGTH)
        SECURE_RANDOM.nextBytes(iv)

        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, ENCRYPTION_KEY, gcmSpec)

        val payload = "$author|$clientVersionText|$data"
        val encrypted = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))

        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)

        return result
    }

    private fun decrypt(encryptedData: ByteArray): Triple<String, String, String> {
        val iv = encryptedData.copyOfRange(0, IV_LENGTH)
        val actualEncryptedData = encryptedData.copyOfRange(IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, ENCRYPTION_KEY, gcmSpec)

        val decrypted = cipher.doFinal(actualEncryptedData)
        val payload = String(decrypted, Charsets.UTF_8)

        val parts = payload.split("|")
        val author = parts.getOrNull(0) ?: "Unknown"
        val version = parts.getOrNull(1) ?: "Unknown"
        val data = parts.drop(2).joinToString("|")

        return Triple(data, author, version)
    }

    private fun detectEncryptionType(file: File): String {
        return try {
            val data = file.readBytes()

            try {
                val (decryptedData, _, _) = decrypt(data)
                if (decryptedData.length > 100 && decryptedData.matches(Regex("^[A-Za-z0-9+/]*={0,2}$"))) {
                    try {
                        Base64.getDecoder().decode(decryptedData)
                        return "double"
                    } catch (_: Exception) {
                        return "client"
                    }
                } else {
                    return "client"
                }
            } catch (_: Exception) {
                if (data.size >= SALT_LENGTH + IV_LENGTH + 16) {
                    try {
                        data.copyOfRange(0, SALT_LENGTH)
                        data.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
                        return "password"
                    } catch (_: Exception) {
                        return "invalid"
                    }
                }
                return "invalid"
            }
        } catch (_: Exception) {
            "error"
        }
    }

    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <load/save/list/delete/folder/loadmodule>")
            return
        }

        SharedScopes.IO.launch {
            when (args[1].lowercase()) {
                "load" -> loadSettings(args)
                "forceload" -> {
                    if(args.size <= 2) {
                        chatSyntax("${args[0]} forceload <name>")
                        return@launch
                    }
                    val configName = args[2]
                    val configFile = File(settingsDir, "$configName.config")
                    if(!configFile.exists()) {
                        chat("§cSettings file '$configName' not found!")
                        return@launch
                    }
                    val (finalSettings, author, _) = decrypt(configFile.readBytes())
                    SettingsUtils.applyScript(finalSettings)
                    chat("§6Settings force loaded. §bAuthor: $author")
                }
                "decrypt" -> {
                    if (args.size <= 2) {
                        chatSyntax("${args[0]} decrypt <name> [password]")
                        return@launch
                    }

                    val configName = args[2]
                    val configFile = File(settingsDir, "$configName.config")
                    val txtFile = File(settingsDir, "$configName.txt")

                    val settingsFile = when {
                        configFile.exists() -> configFile
                        txtFile.exists() -> txtFile
                        else -> {
                            chat("§cSettings file '$configName' not found!")
                            return@launch
                        }
                    }

                    try {
                        val decryptedContent: String = when {
                            settingsFile.name.endsWith(".config") -> {
                                val encryptionType = detectEncryptionType(settingsFile)

                                when (encryptionType) {
                                    "double" -> {
                                        if (args.size <= 3) {
                                            chat("§aDetected double encrypted settings. Please provide password.")
                                            return@launch
                                        }
                                        val password = args[3]
                                        val fileData = settingsFile.readBytes()
                                        val (base64Data, _, _) = decrypt(fileData)
                                        val passwordEncryptedData = Base64.getDecoder().decode(base64Data)
                                        val (finalSettings, _, _) = decryptWithPassword(passwordEncryptedData, password)
                                        finalSettings
                                    }
                                    "password" -> {
                                        if (args.size <= 3) {
                                            chat("§aDetected password-protected settings. Please provide password.")
                                            return@launch
                                        }
                                        val password = args[3]
                                        val (finalSettings, _, _) = decryptWithPassword(settingsFile.readBytes(), password)
                                        finalSettings
                                    }
                                    "client" -> {
                                        val (finalSettings, _, _) = decrypt(settingsFile.readBytes())
                                        finalSettings
                                    }
                                    else -> settingsFile.readText()
                                }
                            }
                            else -> settingsFile.readText()
                        }

                        val outFile = File(settingsDir, "${configName}-decrypt.txt")
                        outFile.writeText(decryptedContent)
                        chat("§6Decrypted file created: ${outFile.name}")

                    } catch (e: Exception) {
                        chat("§cFailed to decrypt settings: ${e.message}")
                        LOGGER.error("Decryption failed", e)
                    }
                }

                "save" -> saveSettings(args)
                "delete" -> deleteSettings(args)
                "list" -> listSettings()
                "folder" -> openSettingsFolder()
                "loadmodule" -> loadModuleSettings(args)

                else -> chatSyntax("$usedAlias <load/save/list/delete/folder/loadmodule>")
            }
        }
    }

    private suspend fun loadModuleSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 3) {
                chatSyntax("${args[0].lowercase()} loadmodule <moduleName> <configName> [password]")
                return@withContext
            }

            val moduleName = args[2]
            val configName = args[3]

            val module = ModuleManager[moduleName]
            if (module == null) {
                chat("§cModule '$moduleName' not found!")
                return@withContext
            }

            val configFile = File(settingsDir, "$configName.config")
            val txtFile = File(settingsDir, "$configName.txt")

            val settingsFile = when {
                configFile.exists() -> configFile
                txtFile.exists() -> txtFile
                else -> {
                    chat("§cSettings file '$configName' does not exist!")
                    return@withContext
                }
            }

            try {
                chat("§9Loading settings for module §e$moduleName§9 from ${settingsFile.name}...")

                val moduleSettings = when {
                    settingsFile.name.endsWith(".config") -> {
                        val encryptionType = detectEncryptionType(settingsFile)

                        when (encryptionType) {
                            "double" -> {
                                if (args.size > 4) {
                                    val password = args[4]
                                    val fileData = settingsFile.readBytes()
                                    val (base64Data, _, _) = decrypt(fileData)
                                    val passwordEncryptedData = Base64.getDecoder().decode(base64Data)
                                    val (finalSettings, _, _) = decryptWithPassword(passwordEncryptedData, password)
                                    finalSettings
                                } else {
                                    chat("§aDetected double encrypted settings.")
                                    chat("§eUsage: §6.localsettings loadmodule $moduleName $configName <yourPassword>")
                                    return@withContext
                                }
                            }
                            "password" -> {
                                if (args.size > 4) {
                                    val password = args[4]
                                    val (finalSettings, _, _) = decryptWithPassword(settingsFile.readBytes(), password)
                                    finalSettings
                                } else {
                                    chat("§aDetected password-protected settings.")
                                    chat("§eUsage: §6.localsettings loadmodule $moduleName $configName <yourPassword>")
                                    return@withContext
                                }
                            }
                            "client" -> {
                                val (finalSettings, _, _) = decrypt(settingsFile.readBytes())
                                finalSettings
                            }
                            else -> {
                                settingsFile.readText()
                            }
                        }
                    }
                    else -> settingsFile.readText()
                }

                applyModuleSettings(module, moduleSettings)
                chat("§6Module §e$moduleName§6 settings applied successfully.")

                addNotification(Notification.informative("Local Settings Command", "Updated Module: $moduleName"))
                playEdit()

            } catch (e: IOException) {
                chat("§cFailed to load module settings: ${e.message}")
                LOGGER.error("Failed to load module settings", e)
            } catch (e: Exception) {
                chat("§cFailed to decrypt module settings. Wrong password or corrupted file.")
                LOGGER.error("Module settings decryption failed", e)
            }
        }
    }

    private fun applyModuleSettings(module: Module, settingsScript: String) {
        try {
            val relevantLines = settingsScript.split("\n")
                .filter { it.trim().startsWith("${module.name} ") }

            if (relevantLines.isEmpty()) {
                chat("§cNo settings found for module ${module.name} in the configuration")
                return
            }

            chat("§aFound ${relevantLines.size} settings for ${module.name}")

            val tempScript = relevantLines.joinToString("\n")
            chat("§7Applying script:\n§f$tempScript")

            SettingsUtils.applyScript(tempScript)
            chat("§6Successfully applied ${module.name} settings")

        } catch (e: Exception) {
            LOGGER.error("Failed to apply module settings for ${module.name}", e)
            chat("§cFailed to apply settings for ${module.name}: ${e.message}")
        }
    }

    private suspend fun loadSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} load <name> [password]")
                return@withContext
            }

            val configName = args[2]
            val configFile = File(settingsDir, "$configName.config")
            val txtFile = File(settingsDir, "$configName.txt")

            val settingsFile = when {
                configFile.exists() -> configFile
                txtFile.exists() -> txtFile
                else -> {
                    chat("§cSettings file '$configName' does not exist!")
                    return@withContext
                }
            }

            try {
                chat("§9Loading settings from ${settingsFile.name}...")

                if (settingsFile.name.endsWith(".config")) {
                    val encryptionType = detectEncryptionType(settingsFile)

                    when (encryptionType) {
                        "double" -> {
                            if (args.size > 3 && args[3].equals("password", ignoreCase = true) && args.size > 4) {
                                val password = args[4]
                                val fileData = settingsFile.readBytes()

                                val (base64Data, _, _) = decrypt(fileData)
                                val passwordEncryptedData = Base64.getDecoder().decode(base64Data)
                                val (finalSettings, author, _) = decryptWithPassword(passwordEncryptedData, password)

                                SettingsUtils.applyScript(finalSettings)
                                chat("§6Configuration '$configName' applied successfully. §bAuthor: $author")
                            } else {
                                chat("§aDetected double encrypted settings.")
                                chat("§eUsage: §6.localsettings load $configName password <yourPassword>")
                                return@withContext
                            }
                        }
                        "password" -> {
                            if (args.size > 3 && args[3].equals("password", ignoreCase = true) && args.size > 4) {
                                val password = args[4]
                                val (finalSettings, author, _) = decryptWithPassword(settingsFile.readBytes(), password)
                                SettingsUtils.applyScript(finalSettings)
                                chat("§6Configuration '$configName' applied successfully. §bAuthor: $author")
                            } else {
                                chat("§aDetected password-protected settings.")
                                chat("§eUsage: §6.localsettings load $configName password <yourPassword>")
                                return@withContext
                            }
                        }
                        "client" -> {
                            chat("§aDetected encrypted settings, decrypting...")
                            val (finalSettings, author, _) = decrypt(settingsFile.readBytes())
                            SettingsUtils.applyScript(finalSettings)
                            chat("§6Configuration '$configName' applied successfully. §bAuthor: $author")
                        }
                        "invalid", "error" -> {
                            try {
                                val settingsContent = settingsFile.readText()
                                handlePlainTextSettings(settingsContent, configName)
                            } catch (e: Exception) {
                                chat("§cFailed to load settings: Invalid or corrupted file")
                                LOGGER.error("Failed to load settings", e)
                            }
                        }
                        else -> {
                            val settingsContent = settingsFile.readText()
                            handlePlainTextSettings(settingsContent, configName)
                        }
                    }
                } else {
                    val settingsContent = settingsFile.readText()
                    handlePlainTextSettings(settingsContent, configName)
                }

                addNotification(Notification.informative("Local Settings Command","Updated Settings: $configName"))
                playEdit()
            } catch (e: IOException) {
                chat("§cFailed to load settings '$configName': ${e.message}")
                LOGGER.error("Failed to load settings", e)
            } catch (e: Exception) {
                chat("§cFailed to decrypt settings '$configName'. Wrong password or corrupted file.")
                LOGGER.error("Decryption failed", e)
            }
        }
    }

    private fun handlePlainTextSettings(content: String, configName: String) {
        var finalSettings = content
        var authorName = "Unknown"
        var version = "Unknown"

        val lines = content.lines()
        var metadataEndIndex = 0

        for (line in lines) {
            when {
                line.startsWith("Author:") -> {
                    authorName = line.substringAfter("Author:").trim()
                    metadataEndIndex++
                }
                line.startsWith("Version:") -> {
                    version = line.substringAfter("Version:").trim()
                    metadataEndIndex++
                }
                line.startsWith("//") || line.startsWith("#") -> {
                    metadataEndIndex++
                }
                line.isBlank() -> {
                    metadataEndIndex++
                }
                else -> {
                    break
                }
            }
        }

        if (metadataEndIndex > 0) {
            finalSettings = lines.drop(metadataEndIndex).joinToString("\n").trim()
        }

        chat("§aLoading plain text settings '$configName'...")

        if (authorName != "Unknown") {
            chat("§7Author: §f$authorName")
        }
        if (version != "Unknown") {
            chat("§7Version: §f$version")
        }

        SettingsUtils.applyScript(finalSettings)

        if (authorName != "Unknown") {
            chat("§6Configuration '$configName' applied successfully. §bAuthor: $authorName §7(Version: $version)")
        } else {
            chat("§6Configuration '$configName' applied successfully. §e(Plain Text)")
        }
    }

    private suspend fun saveSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} save <name> [encrypt] [password:<password>] [author:<name>] [all/values/binds/states]...")
                return@withContext
            }

            val hasEncrypt = args.any { it.equals("encrypt", ignoreCase = true) }
            val passwordArg = args.find { it.startsWith("password:", ignoreCase = true) }
            val password = passwordArg?.substringAfter("password:", "")
            val hasPassword = password != null

            val encryptType = when {
                hasEncrypt && hasPassword -> "double"
                hasPassword -> "password"
                hasEncrypt -> "encrypt"
                else -> "none"
            }

            val fileExtension = if (encryptType != "none") ".config" else ".txt"
            val settingsFile = File(settingsDir, args[2] + fileExtension)

            try {
                if (settingsFile.exists())
                    settingsFile.delete()

                val authorArg = args.find { it.startsWith("author:", ignoreCase = true) }
                val author = authorArg?.substringAfter("author:", "Unknown") ?: "Unknown"

                val option = if (args.size > 3) {
                    args.drop(3)
                        .filterNot { it.equals("encrypt", ignoreCase = true) }
                        .filterNot { it.startsWith("password:", ignoreCase = true) }
                        .filterNot { it.startsWith("author:", ignoreCase = true) }
                        .joinToString(" ").lowercase()
                } else "default"

                val all = "all" in option
                val default = "default" in option
                val values = all || default || "values" in option
                val binds = all || "binds" in option
                val states = all || default || "states" in option

                if (!values && !binds && !states) {
                    chatSyntaxError()
                    return@withContext
                }

                chat("§9Creating settings...")
                val settingsScript = try {
                    SettingsUtils.generateScript(values, binds, states)
                } catch (throwable: Throwable) {
                    chat("§cFailed to generate settings script: §3${throwable.javaClass.simpleName}: ${throwable.message ?: "Unknown error"}")
                    LOGGER.error("Failed to generate settings script.", throwable)
                    return@withContext
                }

                chat("§9Saving settings...")

                when (encryptType) {
                    "double" -> {
                        chat("§aApplying double encryption...")
                        val passwordEncrypted = encryptWithPassword(settingsScript, password!!, author)
                        val base64Encoded = Base64.getEncoder().encodeToString(passwordEncrypted)
                        val finalEncrypted = encrypt(base64Encoded, author)
                        settingsFile.writeBytes(finalEncrypted)
                        chat("§6Settings saved with double encryption. §bAuthor: $author")
                    }
                    "password" -> {
                        val encryptedData = encryptWithPassword(settingsScript, password!!, author)
                        settingsFile.writeBytes(encryptedData)
                        chat("§6Settings saved with password protection. §bAuthor: $author")
                    }
                    "encrypt" -> {
                        val encryptedData = encrypt(settingsScript, author)
                        settingsFile.writeBytes(encryptedData)
                        chat("§6Settings saved successfully. §a(Encrypted) §bAuthor: $author")
                    }
                    else -> {
                        val content = buildString {
                            append("Author:").append(author).append("\n")
                            append("Version:").append(clientVersionText).append("\n")
                            append(settingsScript)
                        }

                        settingsFile.writeText(content)
                        chat("§6Settings saved successfully. §e(Plain Text) §bAuthor: $author")
                    }

                }
            } catch (throwable: Throwable) {
                chat("§cFailed to create local config: §3${throwable.message}")
                LOGGER.error("Failed to create local config.", throwable)
            }
        }
    }

    private suspend fun deleteSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} delete <name>")
                return@withContext
            }

            val configFile = File(settingsDir, args[2] + ".config")
            val txtFile = File(settingsDir, args[2] + ".txt")

            val deletedConfig = configFile.exists() && configFile.delete()
            val deletedTxt = txtFile.exists() && txtFile.delete()

            if (!deletedConfig && !deletedTxt) {
                chat("§cSettings file does not exist!")
                return@withContext
            }

            chat("§6Settings file deleted successfully.")
        }
    }

    private suspend fun listSettings() {
        withContext(Dispatchers.IO) {
            val settings = settingsDir.listFiles() ?: return@withContext

            if (settings.isEmpty()) {
                chat("§cNo settings files found.")
                return@withContext
            }

            chat("§6Available Settings:")
            for (file in settings) {
                if (file.name.endsWith(".config") || file.name.endsWith(".txt")) {
                    try {
                        val encryptionType = detectEncryptionType(file)
                        val isEncrypted = when (encryptionType) {
                            "double" -> "§6[Double]"
                            "password" -> "§c[Password]"
                            "client" -> "§a[Encrypted]"
                            "invalid" -> "§c[Invalid]"
                            "error" -> "§c[Error]"
                            else -> "§e[Plain]"
                        }
                        val nameWithoutExt = file.name.removeSuffix(".config").removeSuffix(".txt")
                        chat("> §f$nameWithoutExt $isEncrypted")
                    } catch (_: Exception) {
                        val nameWithoutExt = file.name.removeSuffix(".config").removeSuffix(".txt")
                        chat("> §f$nameWithoutExt §c[Error]")
                    }
                }
            }
        }
    }

    private suspend fun openSettingsFolder() {
        withContext(Dispatchers.IO) {
            try {
                Desktop.getDesktop().open(settingsDir)
            } catch (e: IOException) {
                LOGGER.error("Failed to open settings folder.", e)
                chat("§cFailed to open settings folder.")
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("delete", "list", "load", "save", "folder", "loadmodule").filter { it.startsWith(args[0], true) }

            2 -> {
                when (args[0].lowercase()) {
                    "delete", "load", "save" -> {
                        val settings = settingsDir.listFiles() ?: return emptyList()
                        settings.map {
                            it.name.removeSuffix(".config").removeSuffix(".txt")
                        }.distinct().filter { it.startsWith(args[1], true) }
                    }
                    "loadmodule" -> {
                        ModuleManager.map { it.name }
                            .filter { it.startsWith(args[1], true) }
                    }
                    else -> emptyList()
                }
            }

            3 -> {
                when (args[0].lowercase()) {
                    "loadmodule" -> {
                        val settings = settingsDir.listFiles() ?: return emptyList()
                        settings.map {
                            it.name.removeSuffix(".config").removeSuffix(".txt")
                        }.distinct().filter { it.startsWith(args[2], true) }
                    }
                    "load" -> {
                        if ("password".startsWith(args[2], true)) {
                            listOf("password")
                        } else {
                            emptyList()
                        }
                    }
                    "save" -> {
                        val suggestions = mutableListOf<String>()
                        if ("encrypt".startsWith(args[2], true)) suggestions.add("encrypt")
                        if ("password:".startsWith(args[2], true)) suggestions.add("password:")
                        if ("author:".startsWith(args[2], true)) suggestions.add("author:")
                        suggestions.addAll(listOf("all", "default", "values", "binds", "states")
                            .filter { it.startsWith(args[2], true) })
                        suggestions
                    }
                    else -> emptyList()
                }
            }

            4 -> {
                when (args[0].lowercase()) {
                    "save" -> {
                        val hasEncrypt = args.any { it.equals("encrypt", ignoreCase = true) }
                        val hasPassword = args.any { it.startsWith("password:", ignoreCase = true) }
                        val hasAuthor = args.any { it.startsWith("author:", ignoreCase = true) }

                        val suggestions = mutableListOf<String>()

                        if (!hasPassword && "password:".startsWith(args[3], true)) suggestions.add("password:")
                        if (!hasAuthor && "author:".startsWith(args[3], true)) suggestions.add("author:")
                        if (!hasEncrypt && "encrypt".startsWith(args[3], true)) suggestions.add("encrypt")

                        suggestions.addAll(listOf("all", "default", "values", "binds", "states")
                            .filter { it.startsWith(args[3], true) })

                        suggestions
                    }
                    "loadmodule" -> {
                        if ("password".startsWith(args[3], true)) {
                            listOf("password")
                        } else {
                            emptyList()
                        }
                    }
                    else -> emptyList()
                }
            }

            5 -> {
                when (args[0].lowercase()) {
                    "save" -> {
                        val hasAuthor = args.any { it.startsWith("author:", ignoreCase = true) }
                        val hasPassword = args.any { it.startsWith("password:", ignoreCase = true) }

                        val suggestions = mutableListOf<String>()

                        if (!hasAuthor && "author:".startsWith(args[4], true)) suggestions.add("author:")
                        if (!hasPassword && "password:".startsWith(args[4], true)) suggestions.add("password:")

                        suggestions.addAll(listOf("all", "default", "values", "binds", "states")
                            .filter { it.startsWith(args[4], true) })

                        suggestions
                    }
                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }
}