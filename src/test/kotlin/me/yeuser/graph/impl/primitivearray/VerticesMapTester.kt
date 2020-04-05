package me.yeuser.graph.impl.primitivearray

import java.util.Random
import me.yeuser.graph.impl.primitivearray.VerticesMap
import org.junit.jupiter.api.Test
import me.yeuser.testutil.*

class VerticesMapTester {

    @Test
    fun testFunctionality() {
        val nodesCount = 100
        val random = Random()
        val vertices = VerticesMap(bufferOpsLimit = 200)
        val nodeEdges = (0 until nodesCount).associateWith { i ->
            (0 until nodesCount).filter { j ->
                i != j && random.nextBoolean()
            }.map { it to random.nextInt(Short.MAX_VALUE.toInt()).toShort() }
        }

        nodeEdges.forEach { (from, toN) -> toN.forEach { vertices.add(from, it.first, it.second) } }

        nodeEdges.forEach { (from, tos) ->
            val expected = tos.toSet()
            val actual = vertices.get(from)!!.toSet()
            assert(actual == expected) { "$actual != $expected" }
        }
    }
}