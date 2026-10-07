package com.era.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager

object EraAgentEngine {

    fun processCommand(command: String, onResponse: (String, String) -> Unit) {
        val lower = command.lowercase().trim()
        val service = EraAccessibilityService.instance

        // 1. Greetings (Hira / ERA)
        if (lower.contains("hira") || lower.contains("era") || lower.startsWith("hello") || lower.startsWith("hi")) {
            onResponse("ERA", "Hello! I am ERA, your personal super AI. Tell me what to do!")
            return
        }

        // 2. Real App Launching (WhatsApp, YouTube, Camera, Chrome, etc.)
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.contains("whatsapp") || lower.contains("youtube") || lower.contains("camera")) {
            val appQuery = lower.removePrefix("open ").removePrefix("launch ").trim()
            val targetName = when {
                lower.contains("whatsapp") -> "whatsapp"
                lower.contains("youtube") -> "youtube"
                lower.contains("chrome") -> "chrome"
                lower.contains("camera") -> "camera"
                lower.contains("instagram") -> "instagram"
                else -> appQuery
            }

            service?.let { ctx ->
                val launched = launchApp(ctx, targetName)
                if (launched) {
                    onResponse("System", "Opening $targetName.")
                    return
                }
            }
        }

        // 3. Torch / Flashlight
        if (lower.contains("torch") || lower.contains("flashlight")) {
            val enable = !lower.contains("off")
            service?.let { toggleTorch(it, enable) }
            onResponse("System", if (enable) "Torch turned ON." else "Torch turned OFF.")
            return
        }

        // 4. System Navigation
        when {
            lower.contains("home") -> {
                service?.pressHome()
                onResponse("System", "Going to home screen.")
                return
            }
            lower.contains("back") -> {
                service?.pressBack()
                onResponse("System", "Navigated back.")
                return
            }
            lower.contains("screenshot") -> {
                service?.takeScreenshot()
                onResponse("System", "Screen captured.")
                return
            }
            lower.contains("notification") -> {
                service?.openNotifications()
                onResponse("System", "Notification panel opened.")
                return
            }
        }

        // 5. Sub-Agents for Complex Tasks
        when {
            lower.contains("deal") || lower.contains("price") || lower.contains("buy") || lower.contains("सस्ता") -> {
                onResponse("DealHunter", "Searching Amazon and Flipkart for the lowest price.")
            }
            lower.contains("travel") || lower.contains("ticket") || lower.contains("train") || lower.contains("flight") -> {
                onResponse("TravelVoyager", "Planning itinerary, checking train and flight timings.")
            }
            lower.contains("read") || lower.contains("pdf") || lower.contains("summary") -> {
                onResponse("DocuSense", "Analyzing document and preparing key highlights.")
            }
            else -> {
                onResponse("ERA", "I understood: '$command'. Processing task.")
            }
        }
    }

    private fun launchApp(context: Context, appName: String): Boolean {
        val pm = context.packageManager
        val knownPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "camera" to "com.google.android.GoogleCamera",
            "instagram" to "com.instagram.android",
            "settings" to "com.android.settings"
        )

        val pkgName = knownPackages[appName]
        if (pkgName != null) {
            val intent = pm.getLaunchIntentForPackage(pkgName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }

        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installed) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(appName, ignoreCase = true)) {
                val intent = pm.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            }
        }
        return false
    }

    private fun toggleTorch(context: Context, enable: Boolean) {
        try {
            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = camManager.cameraIdList[0]
            camManager.setTorchMode(cameraId, enable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
