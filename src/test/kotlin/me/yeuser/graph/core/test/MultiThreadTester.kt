package me.yeuser.graph.core.test

import java.util.Random
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.yeuser.graph.core.GraphInMem
import org.junit.jupiter.api.Test

class MultiThreadTester {

    @Test
    fun testFunctionalityMultiThreaded() {
        val graph = GraphInMem(100, 100, "A", "B", "C")
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