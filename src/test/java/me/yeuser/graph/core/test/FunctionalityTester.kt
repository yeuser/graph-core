package me.yeuser.graph.core.test

import me.yeuser.graph.core.GraphInMem
import org.junit.Test
import java.io.IOException
import java.util.Random


class FunctionalityTester {

  @Test
  @Throws(InterruptedException::class, IOException::class)
  fun testFunctionality() {
    val random = Random()
    val graph = GraphInMem(100, 1000, 100, "A", "B", "C")
    val nodeEdges = (0 until 10).map { it to (0 until it union (it + 1 until 10)).filter { _ -> random.nextBoolean() } }.toMap()

    val nodes = random.longs(10).toArray()
    val edges = nodeEdges
        .flatMap { (from, toN) -> toN.map { nodes[from] to nodes[it] } }
        .associate { it to (random.nextInt(100) + 1) / 100.0 }

    edges.forEach { (fromTo, weight) -> graph.addEdge(fromTo.first, fromTo.second, "A", weight, false) }

    nodeEdges.keys.forEach { fromIndex ->
      val from = nodes[fromIndex]
      val expected = edges.filter { it.key.first == from }.mapKeys { it.key.second }
      val actual = graph.getEdgeConnections(from).asSequence().associate { it.to to it.weight }
      assert(actual == expected) { "$actual != $expected" }
    }
  }

  @Test
  @Throws(InterruptedException::class, IOException::class)
  fun testFunctionalityBidirectional() {
    val random = Random()
    val graph = GraphInMem(100, 1000, 100, "A", "B", "C")
    val nodeEdges = (0 until 9).map { it to (it + 1 until 10).filter { _ -> random.nextBoolean() } }.toMap()

    val nodes = random.longs(10).toArray()
    val edges = nodeEdges
        .flatMap { (from, toN) -> toN.map { nodes[from] to nodes[it] } }
        .associate { it to (random.nextInt(100) + 1) / 100.0 }

    edges.forEach { (fromTo, weight) -> graph.addEdge(fromTo.first, fromTo.second, "A", weight, true) }

    nodeEdges.keys.forEach { fromIndex ->
      val from = nodes[fromIndex]
      val expected = (edges.filter { it.key.first == from }.mapKeys { it.key.second }.entries union
          edges.filter { it.key.second == from }.mapKeys { it.key.first }.entries)
          .map { it.toPair() }
          .toMap()
      val actual = graph.getEdgeConnections(from).asSequence().associate { it.to to it.weight }
      assert(actual == expected) { "$actual != $expected" }
    }
  }
}