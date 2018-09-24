package me.yeuser.graph.core.test

import me.yeuser.graph.core.GraphInMem
import org.junit.Test
import java.io.IOException
import java.security.SecureRandom
import java.util.stream.IntStream

class FootPrintTester {

  @Test
  @Throws(InterruptedException::class, IOException::class)
  fun testFunctionalitySingleThreaded() {
    // Get the Java runtime
    val runtime = Runtime.getRuntime()
    val edgesCount = 1000000
    val nodesCount = 10000
    val random = SecureRandom()
    val edgeTypes = arrayOf("A", "M", "E")
    val graph = GraphInMem(nodesCount, edgesCount, 100, *edgeTypes)
    println("Creating Nodes!")
    val nodes = IntStream.range(0, nodesCount).mapToLong { i -> random.nextLong() }.distinct().toArray()
    println("Nodes created!")
    println("Creating Edges!")
    var sumTimeI: Long = 0
    var sumTimeR: Long = 0
    var addCnt = 0
    var getCnt = 0
    for (i1 in 0 until nodesCount) {
      val l1 = nodes[i1]
      val i2s = IntStream.range(0, nodesCount).map { random.nextInt(nodesCount) }.distinct().filter { i -> i != i1 }.toArray()
      val i2sEdgeTypes = (0 until i2s.size).map { edgeTypes[random.nextInt(edgeTypes.size)] }.toTypedArray()
      val i2sWeights = (0 until i2s.size).map { (random.nextInt(100) + 1) / 100.0 }.toDoubleArray()
      var time = System.currentTimeMillis()
      for (i in i2s.indices) {
        val i2 = i2s[i]
        val l2 = nodes[i2]
        val edgeType = i2sEdgeTypes[i]
        val weight = i2sWeights[i]
        graph.addEdge(l1, l2, edgeType, weight, false)
        addCnt++
      }
      time = System.currentTimeMillis() - time
      sumTimeI += time

      // Calculate the used memory
      var memory = runtime.totalMemory() - runtime.freeMemory()
      println("Edges created so far: $addCnt took ${sumTimeI}ms in total. Avg: ${"%.2f".format(sumTimeI.toDouble() * 1e6 / addCnt)}µs per entry!\tUsed memory: ${memory / (1024L * 1024L)}MB")

      time = System.currentTimeMillis()
      for (i in i2s.indices) {
        val i2 = i2s[i]
        val l2 = nodes[i2]
        val edgeType = i2sEdgeTypes[i]
        val weight = i2sWeights[i]
        val edge = graph.getEdge(l1, l2)
        getCnt++
        assert(Math.abs(edge.weight - weight) < 0.01) {
          edge.weight.toString() + " != " + weight
        }
        assert(edge.edgeType == edgeType) {
          edge.edgeType + " != " + edgeType
        }
      }
      time = System.currentTimeMillis() - time
      sumTimeR += time
      // Calculate the used memory
      memory = runtime.totalMemory() - runtime.freeMemory()
      println("Edges read so far: $getCnt took ${sumTimeR}ms in total. Avg: ${"%.2f".format(sumTimeR.toDouble() * 1e6 / getCnt)}µs per entry!\tUsed memory: ${memory / (1024L * 1024L)}MB")
    }

    print("All edges were created!")
    Thread.yield()

    // Run the garbage collector
    println("Going to do GC!")
    runtime.gc()
    println("Going to sleep!")
    Thread.sleep(100_000)
    // Calculate the used memory
    val memory = runtime.totalMemory() - runtime.freeMemory()
    println("Used memory: ${memory / (1024L * 1024L)}MB")
  }
}