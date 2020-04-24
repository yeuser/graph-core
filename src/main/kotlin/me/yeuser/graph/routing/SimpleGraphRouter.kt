package me.yeuser.graph.routing

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntComparator
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue
import me.yeuser.graph.core.EdgeIndexer
import me.yeuser.graph.core.GraphRouter

class SimpleGraphRouter<T>(edges: EdgeIndexer<T>) : GraphRouter<T>(edges) {

    override fun route(
        from: Int,
        to: Int,
        type: T?,
        maxW: Double
    ): List<Pair<Int, Double>> {
        val seen = Int2ObjectOpenHashMap<Pair<Double, Int?>>().apply { defaultReturnValue(Double.MAX_VALUE to null) }
        val queue = IntHeapPriorityQueue(
            10_000,
            IntComparator { n1, n2 -> seen[n1].first.compareTo(seen[n2].first) })
        seen[from] = 0.0 to null
        var node = from
        while (node != to) {
            edges.allFrom(node, type).forEach { (from, to, _, ew) ->
                val w = seen[from].first + ew
                if (w < seen[to].first) {
                    seen[to] = w to from
                    if (w < maxW) queue.enqueue(to)
                }
            }
            if (queue.size() == 0) return emptyList()
            node = queue.dequeueInt()
        }
        val ret = mutableListOf<Pair<Int, Double>>()
        while (node != from) {
            val (w, f) = seen[node]
            ret.add(node to w)
            node = f!!
        }
        ret.add(from to 0.0)
        return ret.reversed()
    }
}