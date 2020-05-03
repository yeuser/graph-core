package me.yeuser.graph.blocks.primitivearray

import java.util.Random
import kotlin.streams.toList
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class BigIntSetTester {
    private val random = Random()

    @Test
    fun `testing Overwriting`() {
        val i2s = BigIntSet()
        i2s.addAll(setOf(6088))
        i2s.addAll(setOf(6088))
        assertEquals(6088, i2s.asSequence().toList()[0])
        i2s.removeAll(setOf(6088))
        assertEquals(0, i2s.asSequence().count())
    }

    @Test
    fun testFunctionality() {
        val allInts = mutableSetOf<Int>()
        val bigIntSet = BigIntSet()

        (1..100).map {
            random.ints(50L + random.nextInt(700), 5, 30_000).distinct().toList() to
                random.ints(50L + random.nextInt(700), 5, 30_000).distinct().toList()
        }.withIndex().forEach { (index, testSet) ->
            val (insertSet, deleteSet) = testSet
            allInts.addAll(insertSet)
            allInts.removeAll(deleteSet)
            print("Test#${index + 1}=> ")
            print("length inserts:${insertSet.size}, ")
            print("length deletes:${deleteSet.size}, ")
            print("final size: ${allInts.size}")

            bigIntSet.addAll(insertSet)
            insertSet.forEach { testNumber ->
                assert(bigIntSet.has(testNumber)) {
                    "$bigIntSet, $testNumber"
                }
            }
            bigIntSet.removeAll(deleteSet)

            assertEquals(allInts.size, bigIntSet.size)
            println(", dirty items: ${bigIntSet.dirtyCells}")

            assertEquals(
                allInts.toSet(),
                bigIntSet.asSequence().toSet(),
                """
                `allInts` has extra: ${allInts.minus(bigIntSet.asSequence())} 
                `bigIntSet` has extra: ${bigIntSet.asSequence().minus(allInts).toSet()}
                """.trimIndent()
            )
        }
    }
}