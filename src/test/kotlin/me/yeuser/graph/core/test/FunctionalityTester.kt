package me.yeuser.graph.core.test

import io.kotest.matchers.shouldBe
import java.security.SecureRandom
import java.util.Random
import me.yeuser.graph.core.GraphEdge
import me.yeuser.graph.core.GraphInMem
import org.junit.jupiter.api.Test

class FunctionalityTester {

    @Test
    fun `test typeWeight in GraphInMem`() {
        val typesCount = 0xFF
        val precision = 0xFFFF / typesCount
        val graphInMem = GraphInMem(100, precision, *(1..typesCount).map { "type-$it" }.toTypedArray())
        val random = SecureRandom()
        val weight1 = (1).toDouble() / precision
        val type1 = "type-1"
        graphInMem.addEdge(1L, 2L, type1, weight1, false)
        val weight2 = (random.nextInt(precision - 1)).toDouble() / precision
        val type2 = "type-${random.nextInt(typesCount)}"
        graphInMem.addEdge(2L, 3L, type2, weight2, false)
        val weight3 = (precision).toDouble() / precision
        val type3 = "type-$typesCount"
        graphInMem.addEdge(3L, 4L, type3, weight3, false)
        graphInMem.getEdge(1L, 2L) shouldBe GraphEdge(1L, 2L, type1, weight1)
        graphInMem.getEdge(2L, 3L) shouldBe GraphEdge(2L, 3L, type2, weight2)
        graphInMem.getEdge(3L, 4L) shouldBe GraphEdge(3L, 4L, type3, weight3)
    }

    @Test
    fun testFunctionality() {
        val random = Random()
        val graph = GraphInMem(100, 100, "A", "B", "C")
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
            val actual = graph.getEdgeConnections(from).associate { it.to to it.weight }
            assert(actual == expected) { "$actual != $expected" }
        }
    }

    @Test
    fun testFunctionalityBidirectional() {
        val random = Random()
        val graph = GraphInMem(100, 100, "A", "B", "C")
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
            val actual = graph.getEdgeConnections(from).associate { it.to to it.weight }
            assert(actual == expected) { "$actual != $expected" }
        }
    }
}