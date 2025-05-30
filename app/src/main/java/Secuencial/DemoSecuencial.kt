fun tarea1() {
    println("Inicio tarea1")
    Thread.sleep(1000)
    println("Fin tarea1")
}

fun subTarea2A() {
    println("Inicio subTarea2A")
    Thread.sleep(1000)
    println("Fin subTarea2A")
}

fun subTarea2B() {
    println("Inicio subTarea2B")
    Thread.sleep(700)
    println("Fin subTarea2B")
}

fun tarea2() {
    println("Inicio tarea2")
    subTarea2A()
    subTarea2B()
    println("Fin tarea2")
}

fun main() {
    val start = System.currentTimeMillis()
    tarea1()
    tarea2()
    val end = System.currentTimeMillis()
    println("Tiempo total: ${end - start} ms")
}
