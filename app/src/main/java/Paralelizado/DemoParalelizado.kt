package Paralelizado

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

suspend fun tarea2() = coroutineScope {
    println("Inicio tarea2")
    val jobA = async{ subtarea2a() }
    val jobB = async{ subtarea2b() }
    jobA.await()
    jobB.await()
    println("Fin tarea2")
}

fun main() {
    runBlocking {
        val start = System.currentTimeMillis()
        val job1 = async(Dispatchers.Default) { tarea1() }
        val job2 = async(Dispatchers.Default) { tarea2() }
        job1.await()
        job2.await()
        val end = System.currentTimeMillis()
        println("Tiempo total: ${end - start} ms")
    }
}
