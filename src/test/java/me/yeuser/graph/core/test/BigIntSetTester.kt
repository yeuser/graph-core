package me.yeuser.graph.core.test

import me.yeuser.graph.core.BigIntSet
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.streams.asSequence

class BigIntSetTester {
  val random = Random()

  @Test
  fun testFunctionality() {
    val testSets = (1..100).map {
      random.ints(50L + random.nextInt(700), 5, 30_000).distinct().toArray()
    }
    val allInts = mutableSetOf<Int>()

    val bigIntSet = BigIntSet()
    for ((index, testSet) in testSets.withIndex()) {
      allInts.addAll(testSet.asIterable())
      println("Test#${index + 1}: length:${testSet.size}, total size: ${allInts.size}")
      bigIntSet.addAll(testSet)

      testSet.forEach { testNumber -> assert(bigIntSet.has(testNumber)) }

      assert(
        bigIntSet.findArr(testSet).all { it != null }
      )

      Assert.assertEquals(allInts.size, bigIntSet.size())

      Assert.assertEquals(allInts, bigIntSet.asSequence().toHashSet())
    }

    (0..4).forEach { testNumber -> assert(!bigIntSet.has(testNumber)) }
    assert(
      bigIntSet.findArr((0..4).toList().toIntArray()).all { it == null }
    )
  }

  @Test
  fun testPerformance() {

    val padSize = 10
    val testCount = 200
    val setSize: Long = 5_000
    val upperBoundary = 100_000

    println(
      """
      Setup:
          testCount: $testCount
          setSize: $setSize
          upperBoundary: $upperBoundary

      method    | ${"time (ms)".padEnd(padSize)} | ${"memory (bytes)".padEnd(padSize)}
    """.trimIndent()
    )


    repeat(5) {
      val testSets = (1..testCount).map {
        random.ints(setSize, 0, upperBoundary).distinct().asSequence().toList()
      }
      val testSetsArray = testSets.map { it.toIntArray() }

      gc()
      var time0 = System.currentTimeMillis()
      var memory0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
      time0 = System.currentTimeMillis() - time0
      gc()
      memory0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory0

      println(
        """
      -NOOP-    | ${"%,d".format(time0).padStart(padSize)} | ${"%,d".format(memory0).padStart(
          padSize
        )}
    """.trimIndent()
      )

      gc()
      var time1 = System.currentTimeMillis()
      var memory1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
      val hashSet = mutableSetOf<Int>()
      for (testSet in testSets) {
        hashSet.addAll(testSet.asIterable())
      }
      time1 = System.currentTimeMillis() - time1
      gc()
      memory1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory1

      println(
        """
      hashset   | ${"%,d".format(time1).padStart(padSize)} | ${"%,d".format(memory1).padStart(
          padSize
        )}
    """.trimIndent()
      )

      gc()

      var time2 = System.currentTimeMillis()
      var memory2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
      val bigIntSet = BigIntSet()
      for (testSet in testSetsArray) {
        bigIntSet.addAll(testSet)
      }
      time2 = System.currentTimeMillis() - time2
      gc()
      memory2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - memory2

      println(
        """
      bigintset | ${"%,d".format(time2).padStart(padSize)} | ${"%,d".format(memory2).padStart(
          padSize
        )}
    """.trimIndent()
      )
    }

  }

  private fun gc() {
    print("---------------------------gc")
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
    println("---------------------------!")
  }

}

fun Number.format(pad: Int = 7): String {
  val s = (when (this) {
    is Float, is Double -> "%,d"
    else -> "%,.2f"
  })
    .format(Locale.US, this)
  val paddedZeros = (0..(pad - s.length)).joinToString { " " }
  return paddedZeros + s
}