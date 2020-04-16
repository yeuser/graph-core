package me.yeuser.graph.core

import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

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

    protected abstract fun add(fromIdx: Int, toIdx: Int, value: Short)
    protected abstract fun valueOf(fromIdx: Int, toIdx: Int): Short?
    protected abstract fun del(fromIdx: Int, toIdx: Int)
    protected abstract fun connectionsFrom(fromIdx: Int): Sequence<Pair<Int, Short>>?
    protected abstract fun connectionsTo(toIdx: Int): Sequence<Pair<Int, Short>>?
    protected abstract fun size(): Int

    final override fun add(
        fromIdx: Int,
        toIdx: Int,
        type: T,
        weight: Double,
        biDirectional: Boolean
    ) {
        val edgeType = edgeTypes.indexOfFirst { it == type }
        assert(edgeType >= 0) { "Given `type` is unknown!" }
        val typeWeight = getTypeWeight(edgeType, weight)
        lock.writeLock().withLock {
            add(fromIdx, toIdx, typeWeight)
            if (biDirectional) add(toIdx, fromIdx, typeWeight)
        }
    }

    final override fun remove(
        fromIdx: Int,
        toIdx: Int,
        biDirectional: Boolean
    ) {
        lock.writeLock().withLock {
            del(fromIdx, toIdx)
            if (biDirectional) del(toIdx, fromIdx)
        }
    }

    private fun getTypeWeight(t: Int, weight: Double): Short {
        val w = (weight * precision).roundToInt() - 1
        return (t * precision + w).toShort()
    }

    final override fun get(fromIdx: Int, toIdx: Int): Edge<T>? =
        lock.readLock().withLock {
            valueOf(fromIdx, toIdx)
        }?.let {
            Edge(fromIdx, toIdx, edgeTypes[getEdgeType(it)], getWeight(it))
        }

    private fun getEdgeType(typeWeight: Short) =
        (typeWeight.toInt() and 0xFFFF) / precision

    private fun getWeight(typeWeight: Short) =
        ((typeWeight.toInt() and 0xFFFF) % precision + 1.0) / precision

    final override fun allFrom(fromIdx: Int, type: T?): Sequence<Edge<T>> {
        val edgeType = type?.let { edgeTypes.indexOfFirst { it == type } }
        assert(edgeType != -1) { "Given `type` ($type) is unknown!" }
        return lock.readLock().withLock {
            connectionsFrom(fromIdx).orEmpty()
                .run { if (edgeType == null) this else filter { (_, tw) -> getEdgeType(tw) == edgeType } }
                .map { (toIdx, typeWeight) ->
                    Edge(fromIdx, toIdx, edgeTypes[getEdgeType(typeWeight)], getWeight(typeWeight))
                }
        }.asSequence()
    }

    final override fun allTo(toIdx: Int, type: T?): Sequence<Edge<T>> {
        val edgeType = type?.let { edgeTypes.indexOfFirst { it == type } }
        assert(edgeType != -1) { "Given `type` ($type) is unknown!" }
        return lock.readLock().withLock {
            connectionsTo(toIdx).orEmpty()
                .run { if (edgeType == null) this else filter { (_, tw) -> getEdgeType(tw) == edgeType } }
                .map { (fromIdx, typeWeight) ->
                    Edge(fromIdx, toIdx, edgeTypes[getEdgeType(typeWeight)], getWeight(typeWeight))
                }
        }.asSequence()
    }

    final override fun count(): Int = lock.readLock().withLock { size() }
}