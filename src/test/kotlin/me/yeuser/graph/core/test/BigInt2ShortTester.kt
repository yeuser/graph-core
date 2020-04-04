package me.yeuser.graph.core.test

import java.util.Random
import kotlin.streams.asSequence
import kotlin.streams.toList
import kotlin.test.assertEquals
import me.yeuser.graph.core.BigInt2Short
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BigInt2ShortTester {
    private val random = Random()

    @Test
    fun `testing Overwriting the weights`() {
        val i2s = BigInt2Short()
        i2s.addAll(mapOf(6088 to 16.toShort()))
        i2s.addAll(mapOf(6088 to (-1356).toShort()))
        i2s.addAll(mapOf(6088 to 16356.toShort()))
        assertEquals(6088 to 16356.toShort(), i2s.asSequence().toList()[0])
    }

    @Test
    fun testFunctionality() {

        val allInts = mutableMapOf<Int, Short>()
        val bigIntSet = BigInt2Short()

        (1..100).map {
            random.ints(50L + random.nextInt(700), 5, 30_000).distinct().toList()
                .associateWith { random.nextInt(Short.MAX_VALUE.toInt()).toShort() } to
                random.ints(50L + random.nextInt(700), 5, 30_000).distinct().toList()
        }.withIndex().forEach { (index, testSet) ->
            val (insertSet, deleteSet) = testSet
            allInts.putAll(insertSet)
            deleteSet.forEach { allInts.remove(it) }
            print("Test#${index + 1}: length:${insertSet.size}, total size: ${allInts.size}")

            bigIntSet.addAll(insertSet)
            insertSet.keys.forEach { testNumber -> assert(bigIntSet.has(testNumber)) }

            assert(bigIntSet.getValues(insertSet.keys.toIntArray()).all { it != null })

            bigIntSet.removeAll(deleteSet)
            assertEquals(allInts.size, bigIntSet.size)
            println(", dirty items: ${bigIntSet.dirtyCells}")

            assertEquals(
                allInts.entries.map { it.toPair() }.sortedBy { it.first }.toList(),
                bigIntSet.asSequence().sortedBy { it.first }.toList(), """
                `allInts` has extra: ${allInts.entries.map { it.toPair() }.minus(bigIntSet.asSequence())} 
                `bigIntSet` has extra: ${bigIntSet.asSequence().minus(allInts.entries.map { it.toPair() }.toSet())}
                """.trimIndent()
            )
        }

        (0..4).forEach { testNumber -> assert(!bigIntSet.has(testNumber)) }
        assert(bigIntSet.getValues((0..4).toList().toIntArray()).all { it == null })
    }

    @Disabled
    @Test
    fun testPerformance() {

        val padSize = 9 // 7 digits + 2 commas
        val testCount = 200
        val setSize: Long = 5_000
        val upperBoundary = 100_000

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

        repeat(5) {
            val testSets = (1..testCount).map {
                random.ints(setSize, 0, upperBoundary).distinct().asSequence().toList()
                    .associateWith { random.nextInt(Short.MAX_VALUE.toInt()).toShort() }
            }

            gc()
            var time0 = System.currentTimeMillis()
            var memory0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            time0 = System.currentTimeMillis() - time0
            gc()
            memory0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory0

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
            var memory1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val map = mutableMapOf<Int, Short>()
            for (testSet in testSets) {
                map.putAll(testSet)
            }
            time1 = System.currentTimeMillis() - time1
            gc()
            memory1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory1

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
            memory0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            time0 = System.currentTimeMillis() - time0
            gc()
            memory0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory0

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
            var memory2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val bigInt2Short = BigInt2Short()
            for (testSet in testSets) {
                bigInt2Short.addAll(testSet)
            }
            time2 = System.currentTimeMillis() - time2
            gc()
            memory2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory2

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

    private fun gc() {
        print(" - gc")
        System.out.flush()
        // Run the garbage collector
        System.gc()
        Thread.yield()
        System.gc()
        Thread.sleep(1000)
        System.gc()
        Thread.yield()
        System.gc()
        Thread.sleep(1000)
        System.gc()
        Thread.yield()
        // Hoping that garbage collector is called after so many yields and sleeps
        println(" - !")
    }
}