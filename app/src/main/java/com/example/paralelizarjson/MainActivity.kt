package com.example.paralelizarjson

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
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
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingInterior ->
                    CargadorJsonUI(
                        modifier = Modifier.padding(paddingInterior)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CargadorJsonUI(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    var textoResultado by remember { mutableStateOf("Esperando acción del usuario...") }
    var estaCargando by remember { mutableStateOf(false) }
    var numeroHilos by remember { mutableFloatStateOf(2f) }
    var usarSecuencial by remember { mutableStateOf(false) }
    var usarDispatchersPorDefecto by remember { mutableStateOf(false) }
    var textoCantidadArchivosJson by remember { mutableStateOf("1") }
    var datosProcesados by remember { mutableStateOf<List<Datos>>(emptyList()) }

    val cantidadArchivosJson = textoCantidadArchivosJson.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val maximoHilos = Runtime.getRuntime().availableProcessors()
    val minimoHilos = 2
    val hilosParesMaximos = if (maximoHilos % 2 == 0) maximoHilos else maximoHilos - 1
    val pasos = ((hilosParesMaximos - minimoHilos) / 2)
    val alcanceCorrutina = rememberCoroutineScope()


    fun leerJson(contexto: Context, indice: Int): String {
        val nombreArchivo = "data$indice.json"
        return contexto.assets.open(nombreArchivo).bufferedReader().readText()
    }

    suspend fun cargarJson() {
        estaCargando = true
        textoResultado = ""


        val json = Json { ignoreUnknownKeys = true }
        val hilosEfectivos = if (usarSecuencial) 1 else numeroHilos.toInt().coerceAtLeast(1)

        val dispatcherParser =
            if (usarDispatchersPorDefecto) Dispatchers.Default else Dispatchers.Default.limitedParallelism(
                hilosEfectivos
            )

        val dispatcherJson =
            if (usarDispatchersPorDefecto) Dispatchers.IO else Dispatchers.IO.limitedParallelism(
                hilosEfectivos
            )

        val tiempo = measureTimeMillis {
            try {
                if (usarSecuencial) {
                    datosProcesados = (1..cantidadArchivosJson).flatMap {
                        val contenidoJson = leerJson(contexto, it % 5)
                        json.decodeFromString<List<Datos>>(contenidoJson)
                    }
                } else {
                    withContext(Dispatchers.Default) {
                        val contenidosJson = (1..cantidadArchivosJson).map {
                            async(dispatcherJson) {
                                leerJson(contexto, it % 5)
                            }
                        }

                        val listasJson = contenidosJson.map { contenidoJson ->
                            async(dispatcherParser) {
                                json.decodeFromString<List<Datos>>(contenidoJson.await())
                            }
                        }
                        datosProcesados = listasJson.awaitAll().flatten()
                    }

                }
            } catch (e: Exception) {
                textoResultado = "Error al cargar datos: ${e.localizedMessage}"
            }
        }

        textoResultado = "Datos cargados: ${datosProcesados.size}\nTiempo total: ${tiempo}ms"
        estaCargando = false

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
                    checked = usarSecuencial,
                    onCheckedChange = {
                        usarSecuencial = it
                        usarDispatchersPorDefecto = false
                    }
                )
                Text("Secuencial")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = usarDispatchersPorDefecto,
                    onCheckedChange = {
                        usarDispatchersPorDefecto = it
                        usarSecuencial = false
                    }
                )
                Text("Dispatchers Kotlin")
            }
        }

        OutlinedTextField(
            value = textoCantidadArchivosJson,
            onValueChange = { textoCantidadArchivosJson = it },
            label = { Text("Nº de archivos JSON a procesar") },
            singleLine = true
        )

        if (!usarSecuencial && !usarDispatchersPorDefecto) {
            Text("Nº de hilos: ${numeroHilos.toInt()} / $hilosParesMaximos")
            Slider(
                value = numeroHilos,
                onValueChange = {
                    val redondeado = (it / 2).roundToInt() * 2
                    numeroHilos = redondeado.toFloat()
                },
                valueRange = minimoHilos.toFloat()..hilosParesMaximos.toFloat(),
                steps = pasos - 1
            )
        }

        Button(
            onClick = {
                alcanceCorrutina.launch {
                    datosProcesados = emptyList()
                    cargarJson()
                }
            },
            enabled = !estaCargando
        ) {
            Text("Cargar JSON")
        }

        if (estaCargando) {
            CircularProgressIndicator()
        }

        if (!estaCargando) {
            Text(textoResultado, style = MaterialTheme.typography.bodyLarge)
        }

        if (datosProcesados.isNotEmpty()) {
            Text("Datos cargados:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(datosProcesados) { item ->
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
