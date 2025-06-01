package Paralelizado

import kotlinx.coroutines.*


suspend fun tarea1() {
    println("Inicio tarea1 en el hilo ${Thread.currentThread().name}")
    delay(1000)
    println("Fin tarea1 en el hilo ${Thread.currentThread().name}")
}

suspend fun subtarea2a() {
    println("Inicio subtarea2a en el hilo ${Thread.currentThread().name}")
    delay(1000)
    println("Fin subtarea2a en el hilo ${Thread.currentThread().name}")
}

suspend fun subtarea2b() {
    println("Inicio subtarea2b en el hilo ${Thread.currentThread().name}")
    delay(700)
    println("Fin subtarea2b en el hilo ${Thread.currentThread().name}")
}

suspend fun tarea2() = coroutineScope {
    println("Inicio tarea2 en el hilo ${Thread.currentThread().name}")
    val jobA = launch{ subtarea2a() }
    val jobB = launch{ subtarea2b() }
    jobA.join()
    jobB.join()
    println("Fin tarea2 en el hilo ${Thread.currentThread().name}")
}

fun main() {
    runBlocking(Dispatchers.Default) {
        val start = System.currentTimeMillis()
        val job1 = launch { tarea1() }
        val job2 = launch { tarea2() }
        job1.join()
        job2.join()
        val end = System.currentTimeMillis()
        println("Tiempo total: ${end - start} ms")
    }
}
