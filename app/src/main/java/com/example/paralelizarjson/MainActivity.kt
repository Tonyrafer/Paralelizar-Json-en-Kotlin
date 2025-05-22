package com.example.paralelizarjson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.paralelizarjson.ui.theme.ParalelizarJsonTheme
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParalelizarJsonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JsonLoaderUI(
                        modifier = Modifier.padding(innerPadding),
                        readJson = { assets.open("data.json").bufferedReader().readText() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun JsonLoaderUI(modifier: Modifier = Modifier, readJson: () -> String) {
    var resultText by remember { mutableStateOf("Esperando acción del usuario...") }
    var isLoading by remember { mutableStateOf(false) }
    var threadCount by remember { mutableFloatStateOf(2f) }
    var useSequential by remember { mutableStateOf(false) }
    var useDefaultDispatchers by remember { mutableStateOf(false) }
    var jsonFileCountText by remember { mutableStateOf("1") }
    var parsedData by remember { mutableStateOf<List<WeatherData>>(emptyList()) }

    val jsonFileCount = jsonFileCountText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val maxThreads = Runtime.getRuntime().availableProcessors()
    val minThreads = 2
    val maxEvenThreads = if (maxThreads % 2 == 0) maxThreads else maxThreads - 1
    val numSteps = ((maxEvenThreads - minThreads) / 2)
    val scope = rememberCoroutineScope()

    fun loadJson() = scope.launch {
        isLoading = true
        resultText = ""
        parsedData = emptyList() // Limpia tabla para refrescar

        val json = Json { ignoreUnknownKeys = true }
        val effectiveThreads = if (useSequential) 1 else threadCount.toInt().coerceAtLeast(1)

        val dispatcherParser =
            if (useDefaultDispatchers) Dispatchers.Default else Dispatchers.Default.limitedParallelism(
                effectiveThreads
            )
        val dispatcherJson =
            if (useDefaultDispatchers) Dispatchers.IO else Dispatchers.IO.limitedParallelism(
                effectiveThreads
            )

        val time = measureTimeMillis {
            withContext(Dispatchers.Default) {
                try {
                    parsedData = if (useSequential) {
                        val jsonString = readJson()
                        (1..jsonFileCount).flatMap {
                            json.decodeFromString<List<WeatherData>>(
                                jsonString
                            )
                        }
                    } else {
                        val jsonStringDeferred = async(dispatcherJson) { readJson() }
                        val deferredLists = (1..jsonFileCount).map {
                            async(dispatcherParser) {
                                json.decodeFromString<List<WeatherData>>(
                                    jsonStringDeferred.await()
                                )
                            }
                        }
                        deferredLists.awaitAll().flatten()
                    }
                } catch (e: Exception) {
                    resultText = "Error al cargar datos: ${e.localizedMessage}"
                }
            }
        }
        resultText = "Datos cargados: ${parsedData.size}\nTiempo total: ${time}ms"

        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Resultado de la carga JSON:", style = MaterialTheme.typography.titleLarge)
        Row {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = useSequential,
                    onCheckedChange = {
                        useSequential = it
                        useDefaultDispatchers = false
                    }
                )
                Text("Secuencial")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = useDefaultDispatchers,
                    onCheckedChange = {
                        useDefaultDispatchers = it
                        useSequential = false
                    }
                )
                Text("Dispatchers kotlin")
            }
        }


        OutlinedTextField(
            value = jsonFileCountText,
            onValueChange = { jsonFileCountText = it },
            label = { Text("Nº de archivos JSON a procesar") },
            singleLine = true
        )

        if (!useSequential && !useDefaultDispatchers) {
            Text("Nº de hilos: ${threadCount.toInt()} / $maxEvenThreads")
            Slider(
                value = threadCount,
                onValueChange = {
                    val rounded = (it / 2).roundToInt() * 2
                    threadCount = rounded.toFloat()
                },
                valueRange = minThreads.toFloat()..maxEvenThreads.toFloat(),
                steps = numSteps - 1
            )
        }

        Button(
            onClick = { loadJson() },
            enabled = !isLoading
        ) {
            Text("Cargar JSON")
        }


        if (isLoading) {
            CircularProgressIndicator()
        }

        if (!isLoading) {
            Text(text = resultText, style = MaterialTheme.typography.bodyLarge)
        }

        if (parsedData.isNotEmpty()) {
            Text("Datos cargados:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(parsedData.take(parsedData.size)) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.first_name, modifier = Modifier.weight(1f))
                        Text(item.language, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JsonLoaderUIPreview() {
    ParalelizarJsonTheme {
        JsonLoaderUI(readJson = { "[]" })
    }
}