package me.yeuser.graph.core.test

import java.util.Random
import me.yeuser.graph.core.VerticesMap
import org.junit.Test

class VerticesMapTester {

    @Test
    fun testFunctionality() {
        val nodesCount = 100
        val random = Random()
        val vertices = VerticesMap(bufferOpsLimit = 200)
        val nodeEdges = (0 until nodesCount).associateWith { i ->
            (0 until nodesCount).filter { j ->
                i != j && random.nextBoolean()
            }
        }

        nodeEdges.forEach { (from, toN) -> toN.forEach { vertices.add(from, it) } }

        nodeEdges.forEach { (from, tos) ->
            val expected = tos.toSet()
            val actual = vertices.get(from)!!.toSet()
            assert(actual == expected) { "$actual != $expected" }
        }
    }
}