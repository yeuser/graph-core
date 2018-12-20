package me.yeuser.graph.core.test

import me.yeuser.graph.core.GraphInMem
import java.io.IOException
import java.security.SecureRandom
import java.util.stream.IntStream
import java.util.stream.LongStream

/**
 * Manual Memory FootPrint Test
 */
@Throws(InterruptedException::class, IOException::class)
fun main(args: Array<String>) {
  val runtime = Runtime.getRuntime()
  // Get the Java runtime
  val edgesCount = 10_000_000
  val nodesCount = 1_000_000
  val random = SecureRandom()
  val edgeTypes = arrayOf("A", "M", "E")
  val graph = GraphInMem(nodesCount, edgesCount, 100, *edgeTypes)

  println("Creating Nodes!")
  val nodes = IntStream.range(0, nodesCount)
    .mapToLong { random.nextLong() }
    .distinct()
    .toArray()
  println("Nodes created!")

  println("Creating Edges!")

  var sumTimeI: Long = 0
  var sumTimeR: Long = 0
  var addCnt = 0
  var getCnt = 0
  for (i1 in 0 until nodesCount) {
    val l1 = nodes[i1]
    val i2s = LongStream.range(0, (edgesCount - graph.getEdgeCount()) / (nodesCount - i1))
      .map { nodes[random.nextInt(nodesCount)] }
      .distinct()
      .filter { it != l1 }
      .toArray()
    val i2sEdgeTypes = (0 until i2s.size)
      .map { edgeTypes[random.nextInt(edgeTypes.size)] }
      .toTypedArray()
    val i2sWeights = (0 until i2s.size)
      .map { (random.nextInt(100) + 1) / 100.0 }
      .toDoubleArray()
    var time = System.currentTimeMillis()
    for (i in i2s.indices) {
      val l2 = i2s[i]
      val edgeType = i2sEdgeTypes[i]
      val weight = i2sWeights[i]
      graph.addEdge(l1, l2, edgeType, weight, false)
      addCnt++
    }
    time = System.currentTimeMillis() - time
    sumTimeI += time

    time = System.currentTimeMillis()
    for (i in i2s.indices) {
      val l2 = i2s[i]
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

    if (i1 % 100_000 == 0) {
      printStatistics(runtime, addCnt, sumTimeI, "created")
      printStatistics(runtime, getCnt, sumTimeR, "read")
      printMemory(runtime)
    }
  }

  println("All edges were created!")
  Thread.yield()

  // Run the garbage collector
  println("Going to do GC!")
  runtime.gc()
  Thread.yield()
  // Calculate the used memory
  printMemory(runtime)
  println(
    "Statistics: number of nodes: ${"%,d".format(graph.getNodeCount())} - number of edges: ${"%,d".format(
      graph.getEdgeCount()
    )}"
  )
  printStatistics(runtime, addCnt, sumTimeI, "created")
  printStatistics(runtime, getCnt, sumTimeR, "read")
}

private fun printStatistics(runtime: Runtime, cnt: Int, sumTime: Long, action: String): Long {
  // Calculate the used memory
  val memory = runtime.totalMemory() - runtime.freeMemory()
  print("Edges $action so far: ${"%,d".format(cnt)} took ${"%,d".format(sumTime)}ms in total.\t")
  println("\tAvg: ${"%,.2f".format(sumTime.toDouble() * 1e6 / cnt)}µs per entry!")
  return memory
}

private fun printMemory(runtime: Runtime) {
  val memory = runtime.totalMemory() - runtime.freeMemory()
  println("Used memory: ${"%,.2f".format(memory / (1024.0 * 1024.0))}MB")
}