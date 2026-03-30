package io.github.xororz.localdream.service

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.xororz.localdream.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Foreground service that calls an OpenAI-compatible image-generation endpoint
 * (`POST /v1/images/generations`) and exposes the result through [generationState].
 */
class OpenAIGenerationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val CHANNEL_ID = "openai_generation_channel"
        private const val NOTIFICATION_ID = 2

        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_API_ENDPOINT = "api_endpoint"
        const val EXTRA_API_KEY = "api_key"
        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_SIZE = "size"
        const val EXTRA_N = "n"

        const val ACTION_STOP = "stop_openai_generation"

        private val _generationState =
            MutableStateFlow<GenerationState>(GenerationState.Idle)
        val generationState: StateFlow<GenerationState> = _generationState

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        fun resetState() {
            _generationState.value = GenerationState.Idle
        }

        fun clearCompleteState() {
            if (_generationState.value is GenerationState.Complete) {
                _generationState.value = GenerationState.Idle
            }
        }
    }

    sealed class GenerationState {
        object Idle : GenerationState()
        object Loading : GenerationState()
        data class Complete(val bitmaps: List<Bitmap>) : GenerationState()
        data class Error(val message: String) : GenerationState()
    }

    private fun updateState(state: GenerationState) {
        _generationState.value = state
    }

    override fun onCreate() {
        super.onCreate()
        _isServiceRunning.value = true
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val prompt = intent?.getStringExtra(EXTRA_PROMPT)
        if (prompt.isNullOrBlank()) {
            Log.e("OpenAIGenService", "No prompt provided")
            stopSelf()
            return START_NOT_STICKY
        }

        val apiEndpoint = intent.getStringExtra(EXTRA_API_ENDPOINT) ?: ""
        val apiKey = intent.getStringExtra(EXTRA_API_KEY) ?: ""
        val modelId = intent.getStringExtra(EXTRA_MODEL_ID) ?: ""
        val size = intent.getStringExtra(EXTRA_SIZE) ?: "512x512"
        val n = intent.getIntExtra(EXTRA_N, 1).coerceIn(1, 4)

        updateState(GenerationState.Loading)

        serviceScope.launch {
            runGeneration(
                prompt = prompt,
                apiEndpoint = apiEndpoint,
                apiKey = apiKey,
                modelId = modelId,
                size = size,
                n = n
            )
        }

        return START_NOT_STICKY
    }

    private suspend fun runGeneration(
        prompt: String,
        apiEndpoint: String,
        apiKey: String,
        modelId: String,
        size: String,
        n: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", modelId)
                put("prompt", prompt)
                put("n", n)
                put("size", size)
                put("response_format", "b64_json")
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val baseUrl = apiEndpoint.trimEnd('/')
            val request = Request.Builder()
                .url("$baseUrl/v1/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            Log.d("OpenAIGenService", "Calling $baseUrl/v1/images/generations model=$modelId")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    val errMsg = try {
                        JSONObject(errBody).optJSONObject("error")?.optString("message")
                            ?: errBody.take(200)
                    } catch (_: Exception) {
                        errBody.take(200)
                    }
                    throw IOException(
                        getString(R.string.error_request_failed, "${response.code}: $errMsg")
                    )
                }

                val responseBody = response.body?.string()
                    ?: throw IOException(getString(R.string.unknown_error))

                val json = JSONObject(responseBody)
                val data = json.optJSONArray("data")
                    ?: throw IOException(getString(R.string.unknown_error))

                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val b64 = item.optString("b64_json")
                    if (b64.isNotEmpty()) {
                        val bytes = Base64.getDecoder().decode(b64)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) bitmaps.add(bmp)
                    }
                }

                if (bitmaps.isEmpty()) {
                    throw IOException(getString(R.string.unknown_error))
                }

                updateState(GenerationState.Complete(bitmaps))
                Log.d("OpenAIGenService", "Generation complete: ${bitmaps.size} image(s)")
            }
        } catch (e: Exception) {
            Log.e("OpenAIGenService", "Generation error: ${e.message}", e)
            updateState(GenerationState.Error(e.message ?: getString(R.string.unknown_error)))
        } finally {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.openai_generating_notify),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, OpenAIGenerationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.openai_generating_notify))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, getString(R.string.cancel), stopPendingIntent)
            .build()
    }
}
