package me.yeuser.graph.routing

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntComparator
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import me.yeuser.graph.core.EdgeIndexer
import me.yeuser.graph.core.GraphRouter

class BiDirGraphRouter<T>(edges: EdgeIndexer<T>) : GraphRouter<T>(edges) {

    override fun route(
        from: Int,
        to: Int,
        type: T?,
        maxW: Double
    ): List<Pair<Int, Double>> {
        if (from == to) return listOf(from to 0.0)

        val seenForward =
            Int2ObjectOpenHashMap<Pair<Double, Int?>>().apply { defaultReturnValue(Double.MAX_VALUE to null) }
        val queueForward = IntHeapPriorityQueue(
            10_000,
            IntComparator { n1, n2 -> seenForward[n1].first.compareTo(seenForward[n2].first) }
        )
        val seenBackward =
            Int2ObjectOpenHashMap<Pair<Double, Int?>>().apply { defaultReturnValue(Double.MAX_VALUE to null) }
        val queueBackward = IntHeapPriorityQueue(
            10_000,
            IntComparator { n1, n2 -> seenBackward[n1].first.compareTo(seenBackward[n2].first) }
        )
        seenForward[from] = 0.0 to null
        queueForward.enqueue(from)
        seenBackward[to] = 0.0 to null
        queueBackward.enqueue(to)

        val seenBothWays = IntOpenHashSet()

        while (!(queueForward.isEmpty && queueBackward.isEmpty)) {
            if (!queueForward.isEmpty) {
                val nodeForward = queueForward.dequeueInt()
                edges.allFrom(nodeForward, type).forEach { (from, to, _, ew) ->
                    val w = seenForward[from].first + ew
                    if (w < seenForward[to].first) {
                        seenForward[to] = w to from
                        if (w < maxW) {
                            if (seenBackward.containsKey(to)) seenBothWays += to
                            else queueForward.enqueue(to)
                        }
                    }
                }
            }
            if (!queueBackward.isEmpty) {
                val nodeBackward = queueBackward.dequeueInt()
                edges.allTo(nodeBackward, type).forEach { (from, to, _, ew) ->
                    val w = seenBackward[from].first + ew
                    if (w < seenBackward[from].first) {
                        seenBackward[from] = w to to
                        if (w < maxW) {
                            if (seenBackward.containsKey(to)) seenBothWays += from
                            else queueBackward.enqueue(from)
                        }
                    }
                }
            }
        }

        val connectingNode = seenBothWays.minBy { seenForward[it].first + seenBackward[it].first }
            ?: return emptyList()
        val fl = connectingNode.let {
            var node = it
            val ret = mutableListOf<Pair<Int, Double>>()
            while (node != from) {
                val (w, f) = seenForward[node]
                ret.add(node to w)
                node = f!!
            }
            ret.add(from to 0.0)
            ret.reversed()
        }
        val flw = fl.last().second
        val bl = connectingNode.let {
            var node = it
            val ret = mutableListOf<Pair<Int, Double>>()
            if (node != to) {
                val (w0, t0) = seenBackward[node]
                node = t0!!
                while (node != to) {
                    val (w, t) = seenBackward[node]
                    ret.add(node to ((w0 - w) + flw))
                    node = t!!
                }
                ret.add(to to (w0 + flw))
            }
            ret
        }

        return fl + bl
    }
}