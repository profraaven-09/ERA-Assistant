package com.era.assistant

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private val isListening = mutableStateOf(false)
    private val terminalLogs = mutableStateListOf<String>()
    private val lastCommand = mutableStateOf("Ready for commands...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Shake Service
        startService(Intent(this, ShakeDetectorService::class.java))

        // Initialize TTS (Female voice setup)
        textToSpeech = TextToSpeech(this, this)

        // Speech Recognizer
        setupSpeechRecognizer()

        setContent {
            EraHudDashboard(
                isListening = isListening.value,
                logs = terminalLogs,
                statusText = lastCommand.value,
                onMicClick = { toggleListening() }
            )
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening.value = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening.value = false }
                override fun onError(error: Int) { isListening.value = false }
                override fun onResults(results: Bundle?) {
                    isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val command = matches[0]
                        lastCommand.value = command
                        terminalLogs.add("User: $command")
                        handleCommand(command)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun toggleListening() {
        if (isListening.value) {
            speechRecognizer?.stopListening()
            isListening.value = false
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer?.startListening(intent)
            isListening.value = true
        }
    }

    private fun handleCommand(command: String) {
        EraAgentEngine.processCommand(command) { agentName, response ->
            terminalLogs.add("[$agentName]: $response")
            speakOut(response)
        }
    }

    private fun speakOut(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ERA_VOICE_ID")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.ENGLISH
            // Try selecting a female voice if available
            val voices = textToSpeech?.voices
            voices?.firstOrNull { it.name.contains("female", ignoreCase = true) }?.let {
                textToSpeech?.voice = it
            }
            speakOut("ERA Super AI initialized. Standing by.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}

@Composable
fun EraHudDashboard(
    isListening: Boolean,
    logs: List<String>,
    statusText: String,
    onMicClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cyanColor = Color(0xFF00E5FF)
    val darkBlueBg = Color(0xFF060A12)

    val timeString = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) }
    val dateString = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date()) }

    // Memory info
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)
    val availRamGb = (memInfo.availMem / (1024 * 1024 * 1024.0)).let { "%.1f".format(it) }

    // Battery info
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
        context.registerReceiver(null, filter)
    }
    val batteryPct = batteryStatus?.let {
        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        level * 100 / scale
    } ?: 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlueBg)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HUD BAR: Clock, RAM, Battery
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HudStatBox(title = "DATE & TIME", value = "$timeString\n$dateString", cyanColor)
            HudStatBox(title = "RAM AVAIL", value = "${availRamGb} GB", cyanColor)
            HudStatBox(title = "BATTERY", value = "$batteryPct%", cyanColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CENTER: Animated Glowing AI Orb
        Box(
            modifier = Modifier
                .size(240.dp)
                .clickable { onMicClick() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedAiOrb(isListening = isListening, color = cyanColor)
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Microphone",
                tint = if (isListening) Color.White else cyanColor,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isListening) "ERA LISTENING..." else "ERA STANDBY (TAP OR SHAKE PHONE)",
            color = cyanColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // TERMINAL LOGS FEED
        Text(
            text = "AGENT TERMINAL & FEED",
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0D1424), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E2E4E), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                logs.takeLast(6).forEach { log ->
                    Text(
                        text = "> $log",
                        color = Color(0xFF80D8FF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HudStatBox(title: String, value: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1829))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
            Text(text = value, fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun AnimatedAiOrb(isListening: Boolean, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = Modifier.fillMaxSize().scale(scale)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 3

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.2f), Color.Transparent),
                center = center,
                radius = radius * 1.5f
            ),
            radius = radius * 1.4f
        )

        drawCircle(
            color = color,
            radius = radius,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
