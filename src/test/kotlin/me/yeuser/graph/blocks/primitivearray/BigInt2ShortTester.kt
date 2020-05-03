package me.yeuser.graph.blocks.primitivearray

import java.util.Random
import kotlin.streams.toList
import kotlin.test.assertEquals
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
                bigIntSet.asSequence().sortedBy { it.first }.toList(),
                """
                `allInts` has extra: ${allInts.entries.map { it.toPair() }.minus(bigIntSet.asSequence())} 
                `bigIntSet` has extra: ${bigIntSet.asSequence().minus(allInts.entries.map { it.toPair() }.toSet())}
                """.trimIndent()
            )
        }

        (0..4).forEach { testNumber -> assert(!bigIntSet.has(testNumber)) }
        assert(bigIntSet.getValues((0..4).toList().toIntArray()).all { it == null })
    }
}