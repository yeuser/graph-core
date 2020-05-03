package me.yeuser.graph.core

import java.util.Random
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class GraphMultiThreadTester {

    @Test
    fun testFunctionalityMultiThreaded() {
        val precision = 100
        val edgeTypes = arrayOf("A", "B", "C")
        val graphs = listOf(
            Graph.createWithPrimitiveArrays(precision, *edgeTypes),
            Graph.createWithFastMap(precision, *edgeTypes)
        )
        for (graph in graphs) {
            val nodeEdges = arrayOf(
                0 to arrayOf(1, 2),
                1 to arrayOf(3, 4, 5),
                2 to arrayOf(3, 4, 5),
                3 to arrayOf(6, 7),
                4 to arrayOf(6, 7),
                5 to arrayOf(6, 7),
                6 to arrayOf(8),
                7 to arrayOf(8)
            )
            val nodeEdgesFlattened = nodeEdges.flatMap { (from, toN) -> toN.map { from to it } }
            val random = Random()
            val deferredRuns = (1..200).map {
                GlobalScope.async {
                    delay(100)
                    val nodes = random.longs(9).toArray()
                    val edges = nodeEdgesFlattened.associate { (from, to) ->
                        nodes[from] to nodes[to] to (random.nextInt(100) + 1) / 100.0
                    }
                    edges.forEach { (fromTo, weight) ->
                        graph.addEdge(
                            fromTo.first, fromTo.second, "A", weight, true
                        )
                    }

                    delay(100)

                    edges.forEach { (fromTo, weight) ->
                        val (from, to) = fromTo
                        val edge = graph.getEdge(from, to)
                        assert(edge.weight == weight) { "$edge has not the weight = $weight" }
                    }
                }
            }

            runBlocking { deferredRuns.awaitAll() }
        }
    }
}