package me.yeuser.graph.blocks.primitivearray

import java.util.Random
import org.junit.jupiter.api.Test

class VerticesSetTester {

    @Test
    fun testFunctionality() {
        val nodesCount = 100
        val random = Random()
        val vertices = VerticesSet(bufferOpsLimit = 200)
        val nodeEdges = (0 until nodesCount).associateWith { i ->
            (0 until nodesCount).filter { j -> i != j && random.nextBoolean() }
        }

        nodeEdges.forEach { (from, toN) -> toN.map { vertices.add(from, it) } }

        nodeEdges.forEach { (from, tos) ->
            val expected = tos.toSet()
            val actual = vertices.get(from)!!.toSet()
            assert(actual == expected) { "$actual != $expected" }
        }

        val nodeEdges2Delete = nodeEdges.mapValues { (_, value) ->
            value.filter { random.nextBoolean() }
        }

        nodeEdges2Delete.forEach { (from, toN) -> toN.map { vertices.remove(from, it) } }

        nodeEdges.forEach { (from, tos) ->
            val expected = tos.minus(nodeEdges2Delete.getValue(from)).toSet()
            val actual = vertices.get(from)!!.toSet()
            assert(actual == expected) { "$actual != $expected" }
        }
    }
}