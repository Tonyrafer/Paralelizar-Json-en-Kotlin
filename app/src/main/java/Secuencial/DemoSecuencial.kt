fun tarea1() {
    println("Inicio tarea1")
    Thread.sleep(1000)
    println("Fin tarea1")
}

fun subtarea2a() {
    println("Inicio subtarea2a")
    Thread.sleep(1000)
    println("Fin subtarea2a")
}

fun subtarea2b() {
    println("Inicio subtarea2b")
    Thread.sleep(700)
    println("Fin subtarea2b")
}

fun tarea2() {
    println("Inicio tarea2")
    subtarea2a()
    subtarea2b()
    println("Fin tarea2")
}

fun main() {
    val start = System.currentTimeMillis()
    tarea1()
    tarea2()
    val end = System.currentTimeMillis()
    println("Tiempo total: ${end - start} ms")
}
