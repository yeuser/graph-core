package me.yeuser.graph.blocks

import com.google.common.base.Preconditions.checkArgument
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import me.yeuser.graph.blocks.TypeWeightCompressor.checkOverflow
import me.yeuser.graph.blocks.TypeWeightCompressor.compress
import me.yeuser.graph.blocks.TypeWeightCompressor.extractType
import me.yeuser.graph.blocks.TypeWeightCompressor.extractWeight
import me.yeuser.graph.blocks.TypeWeightCompressor.roundToPrecision
import me.yeuser.graph.core.Edge
import me.yeuser.graph.core.EdgeIndexer

abstract class AbstractEdgeIndexer<T>(
    private var precision: Int,
    vararg edgeTypes: T
) : EdgeIndexer<T> {

    init {
        assert(precision > 1) { "Precision must be bigger than 1." }
        checkOverflow(precision, edgeTypes.size)
    }

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val edgeTypes: List<T> = edgeTypes.map { it }

    protected abstract fun add(from: Int, to: Int, value: Short)
    protected abstract fun valueOf(from: Int, to: Int): Short?
    protected abstract fun del(from: Int, to: Int)
    protected abstract fun connectionsFrom(from: Int): Sequence<Pair<Int, Short>>?
    protected abstract fun connectionsTo(to: Int): Sequence<Pair<Int, Short>>?
    protected abstract fun size(): Int

    protected open val minWeight: Double? = null // The lower boundary if given
    protected open val maxWeight: Double? = null // The upper boundary if given

    final override fun add(
        from: Int,
        to: Int,
        type: T,
        weight: Double,
        biDirectional: Boolean
    ) {
        val w = roundToPrecision(precision, weight)
        minWeight?.let {
            checkArgument(w >= it, "weight must be equal to/bigger than lower boundary: $it")
        }
        maxWeight?.let {
            checkArgument(w <= it, "weight must be less than/equal to upper boundary: $it")
        }
        val edgeType = edgeTypes.indexOfFirst { it == type }
        assert(edgeType >= 0) { "Given `type` is unknown!" }
        val typeWeight = compress(precision, edgeType, w)
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

    final override fun get(from: Int, to: Int): Edge<T>? =
        lock.readLock().withLock { valueOf(from, to) }
            ?.let { Edge(from, to, edgeTypes[extractType(precision, it)], extractWeight(precision, it)) }

    final override fun allFrom(from: Int, type: T?): Sequence<Edge<T>> {
        val edgeType = type?.let { edgeTypes.indexOfFirst { it == type } }
        assert(edgeType != -1) { "Given `type` ($type) is unknown!" }
        return lock.readLock().withLock {
            connectionsFrom(from).orEmpty()
                .run {
                    if (edgeType == null) this
                    else filter { (_, tw) -> extractType(precision, tw) == edgeType }
                }
                .map { (to, typeWeight) ->
                    Edge(from, to, edgeTypes[extractType(precision, typeWeight)], extractWeight(precision, typeWeight))
                }
        }.asSequence()
    }

    final override fun allTo(to: Int, type: T?): Sequence<Edge<T>> {
        val edgeType = type?.let { edgeTypes.indexOfFirst { it == type } }
        assert(edgeType != -1) { "Given `type` ($type) is unknown!" }
        return lock.readLock().withLock {
            connectionsTo(to).orEmpty()
                .run {
                    if (edgeType == null) this
                    else filter { (_, tw) -> extractType(precision, tw) == edgeType }
                }
                .map { (from, typeWeight) ->
                    Edge(from, to, edgeTypes[extractType(precision, typeWeight)], extractWeight(precision, typeWeight))
                }
        }.asSequence()
    }

    final override fun count(): Int = lock.readLock().withLock { size() }
}