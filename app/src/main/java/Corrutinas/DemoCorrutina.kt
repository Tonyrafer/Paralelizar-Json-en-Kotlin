package Corrutinas

import kotlinx.coroutines.*

suspend fun tarea1() {
    println("Inicio tarea1")
    delay(1000)
    println("Fin tarea1")
}

suspend fun subtarea2a() {
    println("Inicio subtarea2a")
    delay(1000)
    println("Fin subtarea2a")
}

suspend fun subtarea2b() {
    println("Inicio subtarea2b")
    delay(700)
    println("Fin subtarea2b")
}

suspend fun tarea2() {
    println("Inicio tarea2")
    subtarea2a()
    subtarea2b()
    println("Fin tarea2")
}

fun main() = runBlocking {
    val start = System.currentTimeMillis()

    val job1 = launch { tarea1() }
    val job2 = launch { tarea2() }

    job1.join()
    job2.join()

    val end = System.currentTimeMillis()
    println("Tiempo total: ${end - start} ms")
}