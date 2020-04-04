package me.yeuser.graph.core.test

import java.security.SecureRandom
import kotlin.streams.asSequence
import me.yeuser.graph.core.GraphInMem

/**
 * Manual Memory FootPrint Test
 */
fun main() {
    val nodesCount = 1_000_000
    val edgesCount = nodesCount * 100
    val random = SecureRandom()
    val edgeTypes = arrayOf("A", "M", "E")
    val graph = GraphInMem(nodesCount, 100, *edgeTypes)

    println("Preparing Nodes..")
    val nodes = random.longs().asSequence().distinct().take(nodesCount).toList()
    println("Nodes prepared")

    gc()

    val baseMemoryDatum = printMemory(graph.getNodeCount(), graph.getEdgeCount())

    println("Creating Edges!")

    var sumTimeI: Long = 0
    var sumTimeR: Long = 0
    var addCnt = 0
    var getCnt = 0
    val memoryData = mutableListOf<MemoryDatum>()
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

        if ((i1 + 1) % 50_000 == 0) {
            gc()
            printStatistics(addCnt, sumTimeI, "created")
            printStatistics(getCnt, sumTimeR, "read")
            memoryData += printMemory(graph.getNodeCount(), graph.getEdgeCount())
        }
    }

    println("All edges were created!")

    gc()

    printStatistics(addCnt, sumTimeI, "created")
    printStatistics(getCnt, sumTimeR, "read")
    memoryData += printMemory(graph.getNodeCount(), graph.getEdgeCount())

    // memory = a * nodes + b * edges + c

    // in general, in all data that we collect, "nodes" remain the same
    // so we can assume `mem1-mem2` ≈ `b * (edge1-edge2)`
    val b = memoryData.flatMap { md1 -> memoryData.map { md2 -> md2 to md1 } }
        .filter { (md1, md2) -> md2.second > md1.second && md2.third > md1.third }
        .map { (md1, md2) -> (md2.third - md1.third).toDouble() / (md2.second - md1.second) }
        // median
        .sorted().let { it[it.size / 2] }

    // `memory - b * edges ≈ a * nodes + m0` -> assuming `c ≈ m0`
    val a = memoryData.map { md ->
        ((md.third - baseMemoryDatum.third) - b * md.second) /
            (md.first - baseMemoryDatum.first)
    }
        // median
        .sorted().let { it[it.size / 2] }

    val c = memoryData.map { md -> md.third - b * md.second - a * md.first }.average()

    println("Deducted formula: `memory = ${formatMemory(a)} * nodes + ${formatMemory(b)} * edges + ${formatMemory(c)}`")
}

private fun printStatistics(cnt: Int, sumTime: Long, action: String) {
    print("Edges $action so far: ${"%,d".format(cnt)} took ${"%,d".format(sumTime)}ms in total.\t")
    println("\tAvg: ${"%,.2f".format(sumTime.toDouble() * 1e6 / cnt)}µs per entry!")
}

private fun printMemory(nodes: Int, edges: Int): MemoryDatum {
    val memory = usedMemory()
    println(
        """
    Used memory: ${formatMemory(memory.toDouble())}
      #nodes: ${"%,d".format(nodes)} 
      #edges: ${"%,d".format(edges)}
    """.trimIndent()
    )
    if (edges > 0)
        println("~used mem per edge: ${formatMemory(memory.toDouble() / edges)}")
    return Triple(nodes, edges, memory)
}
typealias MemoryDatum = Triple<Int, Int, Long>