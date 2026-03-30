package io.github.xingguangcuican.localdreamoss.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.xingguangcuican.localdreamoss.R
import io.github.xingguangcuican.localdreamoss.data.OpenAIModel
import io.github.xingguangcuican.localdreamoss.data.OpenAIModelRepository
import io.github.xingguangcuican.localdreamoss.service.OpenAIGenerationService
import io.github.xingguangcuican.localdreamoss.utils.saveImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAIModelRunScreen(
    modelId: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { OpenAIModelRepository(context) }

    val model = remember(modelId, repository.models) {
        repository.models.find { it.id == modelId }
    }

    if (model == null) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    val serviceState by OpenAIGenerationService.generationState.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf(model.supportedSizes.firstOrNull() ?: "512x512") }
    var nImages by remember { mutableIntStateOf(1) }
    var resultBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var selectedBitmapIndex by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val isGenerating = serviceState is OpenAIGenerationService.GenerationState.Loading

    // Observe generation state
    LaunchedEffect(serviceState) {
        when (val state = serviceState) {
            is OpenAIGenerationService.GenerationState.Complete -> {
                resultBitmaps = state.bitmaps
                selectedBitmapIndex = 0
                OpenAIGenerationService.clearCompleteState()
            }
            is OpenAIGenerationService.GenerationState.Error -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.error_generation_failed, state.message)
                )
                OpenAIGenerationService.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(model.name)
                        Text(
                            stringResource(R.string.openai_run_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (resultBitmaps.isNotEmpty()) {
                        IconButton(onClick = {
                            val bmp = resultBitmaps.getOrNull(selectedBitmapIndex)
                            if (bmp != null) {
                                scope.launch {
                                    saveImage(
                                        context = context,
                                        bitmap = bmp,
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.image_saved)
                                                )
                                            }
                                        },
                                        onError = { err ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(err)
                                            }
                                        }
                                    )
                                }
                            }
                        }) {
                            Icon(Icons.Default.Save, stringResource(R.string.save_image))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Result image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isGenerating -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                stringResource(R.string.api_generating),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    resultBitmaps.isNotEmpty() -> {
                        val bmp = resultBitmaps.getOrNull(selectedBitmapIndex)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Text(
                                stringResource(R.string.no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Image selector when multiple results
            if (resultBitmaps.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    resultBitmaps.indices.forEach { idx ->
                        FilterChip(
                            selected = idx == selectedBitmapIndex,
                            onClick = { selectedBitmapIndex = idx },
                            label = { Text("${idx + 1}") },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Prompt field
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(R.string.image_prompt)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                enabled = !isGenerating
            )

            // Size selector
            if (model.supportedSizes.size > 1) {
                Column {
                    Text(
                        stringResource(R.string.api_model_size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        model.supportedSizes.forEach { size ->
                            FilterChip(
                                selected = size == selectedSize,
                                onClick = { selectedSize = size },
                                label = { Text(size) },
                                enabled = !isGenerating
                            )
                        }
                    }
                }
            }

            // N images selector
            Column {
                Text(
                    stringResource(R.string.n_images, nImages),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = nImages.toFloat(),
                    onValueChange = { nImages = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2,
                    enabled = !isGenerating
                )
            }

            // Generate button
            Button(
                onClick = {
                    if (prompt.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.prompt_empty_hint)
                            )
                        }
                        return@Button
                    }
                    val intent = Intent(context, OpenAIGenerationService::class.java).apply {
                        putExtra(OpenAIGenerationService.EXTRA_PROMPT, prompt)
                        putExtra(OpenAIGenerationService.EXTRA_API_ENDPOINT, model.apiEndpoint)
                        putExtra(OpenAIGenerationService.EXTRA_API_KEY, model.apiKey)
                        putExtra(OpenAIGenerationService.EXTRA_MODEL_ID, model.modelId)
                        putExtra(OpenAIGenerationService.EXTRA_SIZE, selectedSize)
                        putExtra(OpenAIGenerationService.EXTRA_N, nImages)
                    }
                    OpenAIGenerationService.resetState()
                    context.startForegroundService(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating && prompt.isNotBlank()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.api_generating))
                } else {
                    Icon(Icons.Default.AutoFixHigh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.api_generate))
                }
            }

            // Model info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        model.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (model.description.isNotEmpty()) {
                        Text(
                            model.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        model.apiEndpoint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
