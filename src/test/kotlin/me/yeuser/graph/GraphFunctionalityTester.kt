package me.yeuser.graph

import io.kotest.matchers.shouldBe
import java.security.SecureRandom
import java.util.Random
import me.yeuser.graph.core.Graph
import me.yeuser.graph.core.GraphEdge
import me.yeuser.graph.core.GraphEdgeNotFound
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class GraphFunctionalityTester {

    @Test
    fun `test typeWeight in Graph with primitives`() {
        val typesCount = 0xFF
        val precision = 0xFFFF / typesCount
        val random = SecureRandom()
        val edgeTypes = (1..typesCount).map { "type-$it" }.toTypedArray()
        for (graph in listOf(
            Graph.createWithPrimitiveArrays(precision, *edgeTypes),
            Graph.createWithFastMap(precision, *edgeTypes)
        )) {
            val weight1 = (1).toDouble() / precision
            val type1 = "type-1"
            graph.addEdge(1L, 2L, type1, weight1, false)
            val weight2 = (1 + random.nextInt(precision - 1)).toDouble() / precision
            val type2 = "type-${random.nextInt(typesCount)}"
            graph.addEdge(2L, 3L, type2, weight2, false)
            val weight3 = (precision).toDouble() / precision
            val type3 = "type-$typesCount"
            graph.addEdge(3L, 4L, type3, weight3, false)
            graph.getEdge(1L, 2L) shouldBe GraphEdge(1L, 2L, type1, weight1)
            graph.getEdge(2L, 3L) shouldBe GraphEdge(2L, 3L, type2, weight2)
            graph.getEdge(3L, 4L) shouldBe GraphEdge(3L, 4L, type3, weight3)
        }
    }

    @Test
    fun `test edge removal at Graph with primitives`() {
        val precision = 100
        val edgeTypes = arrayOf("A", "B", "C")
        for (graph in listOf(
            Graph.createWithPrimitiveArrays(precision, *edgeTypes),
            Graph.createWithFastMap(precision, *edgeTypes)
        )) {

            val (from, to) = 1L to 2L
            graph.addEdge(from, to, "A", 0.5, true)
            graph.removeEdge(from, to, false)
            assertThrows<GraphEdgeNotFound> {
                graph.getEdge(from, to)
            }
            assertDoesNotThrow {
                graph.getEdge(to, from)
            }
            assertDoesNotThrow {
                graph.removeEdge(from, to, true)
            }
            assertThrows<GraphEdgeNotFound> {
                graph.getEdge(to, from)
            }
        }
    }

    @Test
    fun testFunctionality() {
        val random = Random()
        val precision = 100
        val edgeTypes = arrayOf("A", "B", "C")
        for (graph in listOf(
            Graph.createWithPrimitiveArrays(precision, *edgeTypes),
            Graph.createWithFastMap(precision, *edgeTypes)
        )) {

            val nodeEdges = (0 until 10).associateWith { i ->
                (0 until 10).filter { j ->
                    i != j && random.nextBoolean()
                }
            }

            val nodes = random.longs(10).toArray()
            val edges =
                nodeEdges.flatMap { (from, toN) -> toN.map { nodes[from] to nodes[it] } }
                    .associateWith { (random.nextInt(100) + 1) / 100.0 }

            edges.forEach { (fromTo, weight) ->
                graph.addEdge(fromTo.first, fromTo.second, "A", weight, false)
            }

            nodeEdges.keys.forEach { fromIndex ->
                val from = nodes[fromIndex]
                val expected = edges.filter { it.key.first == from }.mapKeys { it.key.second }
                val actual = graph.getEdgeConnectionsFrom(from).associate { it.to to it.weight }
                assert(actual == expected) { "$actual != $expected" }
            }

            nodeEdges.values.flatten().toSet().forEach { toIndex ->
                val to = nodes[toIndex]
                val expected = edges.filter { it.key.second == to }.mapKeys { it.key.first }
                val actual = graph.getEdgeConnectionsTo(to).associate { it.from to it.weight }
                assert(actual == expected) { "$actual != $expected" }
            }
        }
    }

    @Test
    fun testFunctionalityBidirectional() {
        val random = Random()
        val precision = 100
        val edgeTypes = arrayOf("A", "B", "C")
        for (graph in listOf(
            Graph.createWithPrimitiveArrays(precision, *edgeTypes),
            Graph.createWithFastMap(precision, *edgeTypes)
        )) {

            val nodeEdges = (0 until 9).associateWith { (it + 1 until 10).filter { random.nextBoolean() } }

            val nodes = random.longs(10).toArray()
            val edges =
                nodeEdges.flatMap { (from, toN) -> toN.map { nodes[from] to nodes[it] } }
                    .associateWith { (random.nextInt(100) + 1) / 100.0 }

            edges.forEach { (fromTo, weight) ->
                graph.addEdge(fromTo.first, fromTo.second, "A", weight, true)
            }

            nodeEdges.keys.forEach { fromIndex ->
                val from = nodes[fromIndex]
                val expected = (
                    edges.filter { it.key.first == from }
                        .mapKeys { it.key.second }.entries union
                        edges.filter { it.key.second == from }
                            .mapKeys { it.key.first }.entries
                    ).associate { it.toPair() }
                val actual = graph.getEdgeConnectionsFrom(from).associate { it.to to it.weight }
                assert(actual == expected) { "$actual != $expected" }

                val actualRev = graph.getEdgeConnectionsTo(from).associate { it.from to it.weight }
                assert(actualRev == expected) { "$actualRev != $expected" }
            }
        }
    }
}