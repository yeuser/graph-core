package me.yeuser.graph.core.test

import java.io.IOException
import java.security.SecureRandom
import kotlin.streams.asSequence
import me.yeuser.graph.core.GraphInMem

/**
 * Manual Memory FootPrint Test
 */
@Throws(InterruptedException::class, IOException::class)
fun main() {
    // Get the Java runtime
    val nodesCount = 1_000_000
    val edgesCount = nodesCount * 100
    val random = SecureRandom()
    val edgeTypes = arrayOf("A", "M", "E")
    val graph = GraphInMem(nodesCount, edgesCount, 100, *edgeTypes)

    println("Preparing Nodes..")
    val nodes = random.longs().asSequence().distinct().take(nodesCount).toList()
    println("Nodes prepared")

    println("Creating Edges!")

    var sumTimeI: Long = 0
    var sumTimeR: Long = 0
    var addCnt = 0
    var getCnt = 0
    for (i1 in nodes.indices) {
        val l1 = nodes[i1]
        val l2s =
            random
                .ints(0, nodesCount)
                .asSequence()
                .filter { it != i1 }
                .distinct()
                .map { nodes[it] }
                .take(edgesCount / nodesCount)
                .toList()

        var time = System.currentTimeMillis()
        l2s.forEach { l2 ->
            val edgeType = edgeTypes[random.nextInt(edgeTypes.size)]
            val weight = (random.nextInt(100) + 1) / 100.0
            graph.addEdge(l1, l2, edgeType, weight, false)
        }
        addCnt += l2s.size
        time = System.currentTimeMillis() - time
        sumTimeI += time

        time = System.currentTimeMillis()
        l2s.forEach { l2 ->
            val edge = graph.getEdge(l1, l2)
            edge.edgeType
            edge.from
            edge.to
            edge.weight
        }
        getCnt += l2s.size
        time = System.currentTimeMillis() - time
        sumTimeR += time

        if ((i1 + 1) % 100_000 == 0) {
            // Run the garbage collector
            Runtime.getRuntime().gc()
            Thread.yield()
            // Hoping garbage collector runs after yield
            printStatistics(addCnt, sumTimeI, "created")
            printStatistics(getCnt, sumTimeR, "read")
            printMemory(graph.getNodeCount(), graph.getEdgeCount())
        }
    }

    println("All edges were created!")
    Thread.yield()

    // Run the garbage collector
    Runtime.getRuntime().gc()
    Thread.yield()

    printStatistics(addCnt, sumTimeI, "created")
    printStatistics(getCnt, sumTimeR, "read")
    printMemory(graph.getNodeCount(), graph.getEdgeCount())
}

private fun printStatistics(cnt: Int, sumTime: Long, action: String) {
    print("Edges $action so far: ${"%,d".format(cnt)} took ${"%,d".format(sumTime)}ms in total.\t")
    println("\tAvg: ${"%,.2f".format(sumTime.toDouble() * 1e6 / cnt)}µs per entry!")
}

private fun printMemory(nodes: Int, edges: Int) {
    val memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    println(
        """
    Used memory: ${"%,.2f".format(memory / (1024.0 * 1024.0))}MB
      #nodes: ${"%,d".format(nodes)} 
      #edges: ${"%,d".format(edges)}
      ~bytes per edge: ${"%.2f".format(memory.toDouble() / edges)}    
    """.trimIndent()
    )
}