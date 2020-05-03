package me.yeuser.graph.core

import java.util.Random
import kotlin.streams.asSequence
import org.junit.jupiter.api.Test

class NodeIndexerTester {

    @Test
    fun testFunctionality() {
        val nodesCount = 1_000_000
        val random = Random()
        val nodeIndexer = NodeIndexer()
        val nodes =
            (
                random.longs(10_000_000_000L, 10_000_400_000L).asSequence().take(nodesCount / 2) +
                    random.longs(-1_000_400_000L, -1_000_000_000L).asSequence().take(nodesCount / 2)
                )
                .associateWith { nodeIndexer.indexOf(it) }
        // expecting duplicate keys and values in both positive and negative ranges
        assert(nodes.keys.distinct().size == nodes.values.distinct().size)
        nodes.forEach { (nd, index) ->
            assert(nodeIndexer.fromIndex(index) == nd) {
                "${nodeIndexer.fromIndex(index)} $nd"
            }
            assert(nodeIndexer.indexOf(nd) == index) {
                "${nodeIndexer.indexOf(nd)} $index"
            }
        }
    }
}