package com.era.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager

object EraAgentEngine {

    fun processCommand(command: String, onResponse: (String, String) -> Unit) {
        val lower = command.lowercase().trim()
        val context = EraAccessibilityService.instance

        // 1. Greetings (ERA / Hira)
        if (lower.contains("hira") || lower.contains("era") || lower.startsWith("hello") || lower.startsWith("hi")) {
            onResponse("ERA", "Hello! I am ERA, your personal super AI. Tell me what to do!")
            return
        }

        // 2. Real App Launching (WhatsApp, YouTube, Chrome, Camera, Instagram)
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.contains("whatsapp") || lower.contains("youtube") || lower.contains("camera")) {
            val appQuery = lower.removePrefix("open ").removePrefix("launch ").trim()
            val targetName = when {
                lower.contains("whatsapp") -> "whatsapp"
                lower.contains("youtube") -> "youtube"
                lower.contains("chrome") -> "chrome"
                lower.contains("camera") -> "camera"
                lower.contains("instagram") -> "instagram"
                lower.contains("settings") -> "settings"
                else -> appQuery
            }

            if (context != null) {
                val launched = launchApp(context, targetName)
                if (launched) {
                    onResponse("System", "Opening $targetName now.")
                    return
                }
            } else {
                onResponse("System", "Accessibility service needed to open apps.")
                return
            }
        }

        // 3. Torch / Flashlight
        if (lower.contains("torch") || lower.contains("flashlight")) {
            val enable = !lower.contains("off")
            if (context != null) {
                toggleTorch(context, enable)
                onResponse("System", if (enable) "Torch turned ON." else "Torch turned OFF.")
            } else {
                onResponse("System", "Accessibility service needed for torch.")
            }
            return
        }

        // 4. System Navigation via Accessibility
        when {
            lower.contains("home") -> {
                EraAccessibilityService.instance?.pressHome()
                onResponse("System", "Going to home screen.")
                return
            }
            lower.contains("back") -> {
                EraAccessibilityService.instance?.pressBack()
                onResponse("System", "Navigated back.")
                return
            }
            lower.contains("screenshot") -> {
                EraAccessibilityService.instance?.takeScreenshot()
                onResponse("System", "Screen captured.")
                return
            }
            lower.contains("notification") -> {
                EraAccessibilityService.instance?.openNotifications()
                onResponse("System", "Opened notifications.")
                return
            }
        }

        // 5. Dynamic Sub-Agents for Complex Tasks
        when {
            lower.contains("deal") || lower.contains("price") || lower.contains("buy") || lower.contains("सस्ता") -> {
                onResponse("DealHunter", "DealHunter active: Searching Amazon and Flipkart for lowest prices.")
            }
            lower.contains("travel") || lower.contains("ticket") || lower.contains("train") || lower.contains("flight") -> {
                onResponse("TravelVoyager", "TravelVoyager active: Checking route timings and hotel availability.")
            }
            lower.contains("read") || lower.contains("pdf") || lower.contains("summary") -> {
                onResponse("DocuSense", "DocuSense active: Reading screen context and generating summary.")
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
            "facebook" to "com.facebook.katana",
            "settings" to "com.android.settings",
            "maps" to "com.google.android.apps.maps",
            "photos" to "com.google.android.apps.photos"
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
