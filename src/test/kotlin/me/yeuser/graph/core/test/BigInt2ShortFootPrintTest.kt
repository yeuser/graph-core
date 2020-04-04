package me.yeuser.graph.core.test

import java.util.Random
import kotlin.streams.asSequence
import me.yeuser.graph.core.BigInt2Short

fun main() {
    val random = Random()
    val padSize = 9 // 7 digits + 2 commas
    val testCount = 500
    val setSize: Long = 50_000
    val upperBoundary = 2_000_000

    println(
        """
      Setup:
          testCount: $testCount
          setSize: $setSize
          upperBoundary: $upperBoundary

    """.trimIndent()
    )
    val titles = listOf(
        "method".padStart(20),
        "time (ms)".padEnd(padSize),
        "memory (bytes)".padEnd(padSize),
        "bytes per item".padEnd(padSize)
    )
    println(
        titles.joinToString(" | ")
    )
    val pads = titles.map { it.length }

    repeat(2) {
        val testSets = (1..testCount).map {
            random.ints(setSize, 0, upperBoundary).distinct().asSequence().toList()
                .associateWith { random.nextInt(Short.MAX_VALUE.toInt()).toShort() }
        }

        gc()
        var time0 = System.currentTimeMillis()
        var memory0 = usedMemory()
        time0 = System.currentTimeMillis() - time0
        gc()
        memory0 = usedMemory() - memory0

        println(
            listOf(
                "-NOOP-".padStart(pads[0]),
                "%,d".format(time0).padStart(pads[1]),
                "%,d".format(memory0).padStart(pads[2]),
                "".padEnd(pads[3], '-')
            ).joinToString(" | ")
        )

        gc()
        var time1 = System.currentTimeMillis()
        var memory1 = usedMemory()
        val map = mutableMapOf<Int, Short>()
        for (testSet in testSets) {
            map.putAll(testSet)
        }
        time1 = System.currentTimeMillis() - time1
        gc()
        memory1 = usedMemory() - memory1

        println(
            listOf(
                map.javaClass.simpleName.padStart(pads[0]),
                "%,d".format(time1).padStart(pads[1]),
                "%,d".format(memory1).padStart(pads[2]),
                "%,.2f".format(memory1.toFloat() / map.size).padStart(pads[3])
            ).joinToString(" | ")
        )

        gc()
        time0 = System.currentTimeMillis()
        memory0 = usedMemory()
        time0 = System.currentTimeMillis() - time0
        gc()
        memory0 = usedMemory() - memory0

        println(
            listOf(
                "-NOOP-".padStart(pads[0]),
                "%,d".format(time0).padStart(pads[1]),
                "%,d".format(memory0).padStart(pads[2]),
                "".padEnd(pads[3], '-')
            ).joinToString(" | ")
        )

        gc()
        var time2 = System.currentTimeMillis()
        var memory2 = usedMemory()
        val bigInt2Short = BigInt2Short()
        for (testSet in testSets) {
            bigInt2Short.addAll(testSet)
        }
        time2 = System.currentTimeMillis() - time2
        gc()
        memory2 = usedMemory() - memory2

        println(
            listOf(
                bigInt2Short.javaClass.simpleName.padStart(pads[0]).padStart(pads[0]),
                "%,d".format(time2).padStart(pads[1]),
                "%,d".format(memory2).padStart(pads[2]),
                "%,.2f".format(memory2.toFloat() / bigInt2Short.size).padStart(pads[3])
            ).joinToString(" | ")
        )
    }
}