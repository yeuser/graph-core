package me.yeuser.graph.blocks

import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt
import me.yeuser.graph.core.Edge
import me.yeuser.graph.core.IEdgeIndexer

abstract class AbstractEdgeIndexer<T>(
    private var precision: Int,
    vararg edgeTypes: T
) : IEdgeIndexer<T> {

    init {
        assert(precision > 1) { "Precision must be bigger than 1." }
        assert(edgeTypes.size * precision <= 0xFFFF) {
            "Given number of precision and number of edgeTypes are more than acceptable range."
        }
    }

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val edgeTypes: List<T> = edgeTypes.map { it }

    protected abstract fun add(from: Int, to: Int, value: Short)
    protected abstract fun valueOf(from: Int, to: Int): Short?
    protected abstract fun del(from: Int, to: Int)
    protected abstract fun connectionsFrom(from: Int): Sequence<Pair<Int, Short>>?
    protected abstract fun connectionsTo(to: Int): Sequence<Pair<Int, Short>>?
    protected abstract fun size(): Int

    final override fun add(
        from: Int,
        to: Int,
        type: T,
        weight: Double,
        biDirectional: Boolean
    ) {
        val edgeType = edgeTypes.indexOfFirst { it == type }
        assert(edgeType >= 0) { "Given `type` is unknown!" }
        val typeWeight = getTypeWeight(edgeType, weight)
        lock.writeLock().withLock {
            add(from, to, typeWeight)
            if (biDirectional) add(to, from, typeWeight)
        }
    }

    final override fun remove(
        from: Int,
        to: Int,
        biDirectional: Boolean
    ) {
        lock.writeLock().withLock {
            del(from, to)
            if (biDirectional) del(to, from)
        }
    }

    private fun getTypeWeight(t: Int, weight: Double): Short {
        val w = (weight * precision).roundToInt() - 1
        return (t * precision + w).toShort()
    }

    final override fun get(from: Int, to: Int): Edge<T>? =
        lock.readLock().withLock {
            valueOf(from, to)
        }?.let {
            Edge(from, to, edgeTypes[getEdgeType(it)], getWeight(it))
        }

    private fun getEdgeType(typeWeight: Short) =
        (typeWeight.toInt() and 0xFFFF) / precision

    private fun getWeight(typeWeight: Short) =
        ((typeWeight.toInt() and 0xFFFF) % precision + 1.0) / precision

    final override fun allFrom(from: Int, type: T?): Sequence<Edge<T>> {
        val edgeType = type?.let { edgeTypes.indexOfFirst { it == type } }
        assert(edgeType != -1) { "Given `type` ($type) is unknown!" }
        return lock.readLock().withLock {
            connectionsFrom(from).orEmpty()
                .run { if (edgeType == null) this else filter { (_, tw) -> getEdgeType(tw) == edgeType } }
                .map { (to, typeWeight) ->
                    Edge(from, to, edgeTypes[getEdgeType(typeWeight)], getWeight(typeWeight))
                }
        }.asSequence()
    }

    final override fun allTo(to: Int, type: T?): Sequence<Edge<T>> {
        val edgeType = type?.let { edgeTypes.indexOfFirst { it == type } }
        assert(edgeType != -1) { "Given `type` ($type) is unknown!" }
        return lock.readLock().withLock {
            connectionsTo(to).orEmpty()
                .run { if (edgeType == null) this else filter { (_, tw) -> getEdgeType(tw) == edgeType } }
                .map { (from, typeWeight) ->
                    Edge(from, to, edgeTypes[getEdgeType(typeWeight)], getWeight(typeWeight))
                }
        }.asSequence()
    }

    final override fun count(): Int = lock.readLock().withLock { size() }
}