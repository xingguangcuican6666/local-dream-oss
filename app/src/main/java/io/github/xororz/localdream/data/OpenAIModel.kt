package io.github.xororz.localdream.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Represents a remote image-generation model accessed via an OpenAI-compatible API.
 */
data class OpenAIModel(
    /** Unique local UUID identifier for this configuration entry. */
    val id: String,
    /** Display name shown in the UI. */
    val name: String,
    /** Model identifier sent to the API (e.g. "dall-e-3"). */
    val modelId: String,
    /** Base URL of the API endpoint (e.g. "https://api.openai.com"). */
    val apiEndpoint: String,
    /** API key used for authentication (Bearer token). */
    val apiKey: String,
    /** Human-readable description. */
    val description: String = "",
    /** Default sizes supported by the model, e.g. ["256x256", "512x512", "1024x1024"]. */
    val supportedSizes: List<String> = listOf("512x512", "1024x1024")
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("modelId", modelId)
        put("apiEndpoint", apiEndpoint)
        put("apiKey", apiKey)
        put("description", description)
        val sizesArray = JSONArray()
        supportedSizes.forEach { sizesArray.put(it) }
        put("supportedSizes", sizesArray)
    }

    companion object {
        fun fromJson(json: JSONObject): OpenAIModel {
            val sizesArray = json.optJSONArray("supportedSizes")
            val sizes = mutableListOf<String>()
            if (sizesArray != null) {
                for (i in 0 until sizesArray.length()) {
                    sizes.add(sizesArray.getString(i))
                }
            }
            return OpenAIModel(
                id = json.getString("id"),
                name = json.getString("name"),
                modelId = json.getString("modelId"),
                apiEndpoint = json.getString("apiEndpoint"),
                apiKey = json.getString("apiKey"),
                description = json.optString("description", ""),
                supportedSizes = sizes.ifEmpty { listOf("512x512", "1024x1024") }
            )
        }
    }
}

/**
 * Manages a list of [OpenAIModel] configurations persisted in DataStore.
 */
class OpenAIModelRepository(private val context: Context) {
    private val prefs = OpenAIPreferences(context)

    var models by mutableStateOf<List<OpenAIModel>>(emptyList())
        private set

    /** Remote model IDs fetched from the API (for model picker). */
    var remoteModelIds by mutableStateOf<List<String>>(emptyList())
        private set

    var isFetchingModels by mutableStateOf(false)
        private set

    init {
        CoroutineScope(Dispatchers.Main).launch {
            models = prefs.loadModels()
        }
    }

    fun addModel(model: OpenAIModel) {
        val updated = models + model
        models = updated
        CoroutineScope(Dispatchers.IO).launch {
            prefs.saveModels(updated)
        }
    }

    fun removeModel(modelId: String) {
        val updated = models.filter { it.id != modelId }
        models = updated
        CoroutineScope(Dispatchers.IO).launch {
            prefs.saveModels(updated)
        }
    }

    fun updateModel(model: OpenAIModel) {
        val updated = models.map { if (it.id == model.id) model else it }
        models = updated
        CoroutineScope(Dispatchers.IO).launch {
            prefs.saveModels(updated)
        }
    }

    /**
     * Fetch available model IDs from the `/v1/models` endpoint.
     * Results are stored in [remoteModelIds].
     */
    fun fetchRemoteModels(apiEndpoint: String, apiKey: String) {
        isFetchingModels = true
        remoteModelIds = emptyList()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val baseUrl = apiEndpoint.trimEnd('/')
                val request = Request.Builder()
                    .url("$baseUrl/v1/models")
                    .get()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    val ids = mutableListOf<String>()
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val obj = data.optJSONObject(i)
                            val id = obj?.optString("id") ?: continue
                            ids.add(id)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        remoteModelIds = ids
                    }
                } else {
                    Log.w("OpenAIModelRepo", "fetchRemoteModels failed: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("OpenAIModelRepo", "fetchRemoteModels error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isFetchingModels = false
                }
            }
        }
    }
}
