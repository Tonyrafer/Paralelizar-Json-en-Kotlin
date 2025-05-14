package com.example.paralelizarjson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.paralelizarjson.ui.theme.ParalelizarJsonTheme
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.concurrent.Executors
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

@Composable
fun JsonLoaderUI(modifier: Modifier = Modifier, readJson: () -> String) {
    var resultText by remember { mutableStateOf("Cargando datos...") }
    var isLoading by remember { mutableStateOf(false) }
    var threadCount by remember { mutableStateOf(1f) }
    val maxThreads = Runtime.getRuntime().availableProcessors()

    val scope = rememberCoroutineScope()

    fun loadJson() {
        scope.launch {
            isLoading = true
            val json = Json { ignoreUnknownKeys = true }
            var totalItems = 0
            val dispatcher = Executors.newFixedThreadPool(threadCount.toInt()).asCoroutineDispatcher()

            val time = measureTimeMillis {
                withContext(Dispatchers.Default) {
                    if (threadCount.toInt() == 1) {
                        try {
                            val jsonString = readJson()
                            val deferredLists = (1..30).map {
                                json.decodeFromString<List<WeatherData>>(jsonString)
                            }
                            totalItems = deferredLists.sumOf { it.size }
                        } catch (e: Exception) {
                            resultText = "Error al cargar los datos: ${e.localizedMessage}"
                        }
                    } else {
                        try {
                            val jsonString = withContext(Dispatchers.IO) { readJson() }
                            val deferredLists = (1..30).map {
                                async(dispatcher) {
                                    json.decodeFromString<List<WeatherData>>(jsonString)
                                }
                            }
                            val allResults = deferredLists.awaitAll()
                            totalItems = allResults.sumOf { it.size }
                        } catch (e: Exception) {
                            resultText = "Error al cargar los datos: ${e.localizedMessage}"
                        }
                    }
                }
            }
            resultText = "Datos cargados: $totalItems\nTiempo total: ${time}ms"
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadJson()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Resultado de la carga JSON:",
            style = MaterialTheme.typography.titleLarge
        )

        // Slider para seleccionar el número de hilos
        Text("Nº de hilos: ${threadCount.toInt()} / $maxThreads")
        Slider(
            value = threadCount,
            onValueChange = { threadCount = it },
            valueRange = 1f..maxThreads.toFloat(),
            steps = maxThreads - 2 // porque extremos no cuentan
        )

        // Botón para volver a ejecutar la carga
        Button(
            onClick = { loadJson() },
            enabled = !isLoading,
        ) {
            Text("Cargar JSON")
        }

        // Indicador de carga
        if (isLoading) {
            CircularProgressIndicator()
        }

        // Resultado final
        if (!isLoading) {
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun JsonLoaderUIPreview() {
    ParalelizarJsonTheme {
        JsonLoaderUI(readJson = { " {}" })
    }
}