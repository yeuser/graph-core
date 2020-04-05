package me.yeuser.graph

import kotlin.math.floor
import kotlin.math.log
import me.yeuser.graph.core.NodeIndexer
import me.yeuser.testutil.gc
import me.yeuser.testutil.usedMemory

/**
 * Manual Memory FootPrint Test
 */
fun main() {
    val nodesCount = 1_000_000

    val nodeIndexer = NodeIndexer()

    gc()

    println("Adding Nodes!")

    var sumTimeI: Long = 0
    var sumTimeR: Long = 0
    var addCnt = 0
    var getCnt = 0
    val memoryData = mutableListOf<NIMemoryDatum>()

    memoryData += printMemory(nodeIndexer.size())

    var i1 = -1
    while (++i1 < nodesCount) {
        val l1 = i1.toLong() + 1_000_300L

        var time = System.currentTimeMillis()
        val idx = nodeIndexer.indexOf(l1)
        addCnt++
        time = System.currentTimeMillis() - time
        sumTimeI += time

        time = System.currentTimeMillis()
        nodeIndexer.fromIndex(idx)
        getCnt++
        time = System.currentTimeMillis() - time
        sumTimeR += time

        if ((i1 + 1) % 100_000 == 0) {
            gc()
            printStatistics(addCnt, sumTimeI, "created")
            printStatistics(getCnt, sumTimeR, "read")
            memoryData += printMemory(nodeIndexer.size())
        }
    }

    println("All nodes were indexed!")

    gc()
    printStatistics(addCnt, sumTimeI, "created")
    printStatistics(getCnt, sumTimeR, "read")
    memoryData += printMemory(nodeIndexer.size())

    // memory = a * nodes + b

    // `mem1-mem2` = `a * (nodes1-nodes2)`
    val a = memoryData.flatMap { md1 -> memoryData.map { md2 -> md2 to md1 } }
        .filter { (md1, md2) -> md2.first > md1.first && md2.second > md1.second }
        .map { (md1, md2) -> (md2.second - md1.second).toDouble() / (md2.first - md1.first) }
        // median
        .sorted().let { it[it.size / 2] }

    val b = memoryData.map { md -> md.second - a * md.first }.average()

    println("Deducted formula: `memory = ${formatMemory(a)} * nodes + ${formatMemory(
        b
    )}`")
}

private fun printStatistics(cnt: Int, sumTime: Long, action: String) {
    print("Nodes $action so far: ${"%,d".format(cnt)} took ${"%,d".format(sumTime)}ms in total.\t")
    println("\tAvg: ${"%,.2f".format(sumTime.toDouble() * 1e6 / cnt)}µs per entry!")
}

private fun printMemory(nodes: Int): NIMemoryDatum {
    val memory = usedMemory()
    println(
        """
    Used memory: ${formatMemory(memory.toDouble())}
      #nodes: ${"%,d".format(nodes)}
    """.trimIndent()
    )
    if (nodes > 0)
        println("~used mem per edge: ${formatMemory(memory.toDouble() / nodes)}")
    return Pair(nodes, memory)
}

internal fun formatMemory(memory: Double): String {
    val d = floor(log(memory, 1024.0)).toInt()
    return when {
        d >= 3 -> "${"%,.2f".format(memory / 1024 / 1024)}GB"
        d == 2 -> "${"%,.2f".format(memory / 1024 / 1024)}MB"
        d == 1 -> "${"%,.2f".format(memory / 1024)}KB"
        else -> "${"%,.2f".format(memory)}B"
    }
}

internal typealias NIMemoryDatum = Pair<Int, Long>