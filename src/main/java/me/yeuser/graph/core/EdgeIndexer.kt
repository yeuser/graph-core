package me.yeuser.graph.core

import com.google.common.base.Preconditions
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.longs.Long2ShortMap
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class EdgeIndexer(
  expectedNumberOfEdges: Int,
  private var precision: Int,
  edgeTypes: Int
) : IEdgeIndexer {

  private val lock: ReadWriteLock = ReentrantReadWriteLock()
  private val edges: Long2ShortMap = Long2ShortOpenHashMap(expectedNumberOfEdges, 1f)
  private val byType: Array<Int2ObjectMap<IntSet>> = (1..edgeTypes)
    .map { Int2ObjectOpenHashMap<IntSet>(4096, 1f) as Int2ObjectMap<IntSet> }
    .toTypedArray()

  override fun addEdge(
    weight: Double,
    edgeType: Int,
    fromIdx: Int,
    toIdx: Int,
    biDirectional: Boolean
  ) {
    val typeWeight = getTypeWeight(edgeType, weight)
    val connectionTypeMap = byType[edgeType]
    addEdgeInternal(fromIdx, toIdx, typeWeight, connectionTypeMap)
    if (biDirectional) {
      addEdgeInternal(toIdx, fromIdx, typeWeight, connectionTypeMap)
    }
  }

  private fun addEdgeInternal(
    fromIdx: Int, toIdx: Int, typeWeight: Short,
    connectionTypeMap: Int2ObjectMap<IntSet>
  ) {
    lock.writeLock().lock()
    val fromTo = getFromTo(fromIdx, toIdx)
    edges[fromTo] = typeWeight
    connectionTypeMap.computeIfAbsent(fromIdx) { IntRBTreeSet() }.add(toIdx)
    lock.writeLock().unlock()
  }

  override fun getFromTo(fromIdx: Int, toIdx: Int): Long {
    return fromIdx.toLong() and 0xFFFFFFFFL shl 32 or (toIdx.toLong() and 0xFFFFFFFFL)
  }

  override fun getTypeWeight(t: Int, weight: Double): Short {
    val w = Math.round(weight * precision).toInt() - 1
    return (t * precision + w).toShort()
  }

  override fun getEdgeTypeAndWeight(fromIdx: Int, toIdx: Int): Pair<Int, Double> {
    val fromTo = getFromTo(fromIdx, toIdx)
    lock.readLock().lock()
    Preconditions.checkState(edges.containsKey(fromTo), "Given `from->to` edge was not found!")
    val typeWeight = edges.get(fromTo)
    lock.readLock().unlock()
    return Pair(getEdgeType(typeWeight), getWeight(typeWeight))
  }

  override fun getEdgeType(typeWeight: Short): Int {
    return (typeWeight.toInt() and 0xFFFF) / precision
  }

  override fun getWeight(typeWeight: Short): Double {
    return ((typeWeight.toInt() and 0xFFFF) % precision + 1.0) / precision
  }

  override fun tw(
    fromIdx: Int,
    toIdx: Int,
    minWeight: Double,
    maxWeight: Double
  ): Pair<Int, Double>? {
    val fromTo = getFromTo(fromIdx, toIdx)
    lock.readLock().lock()
    val typeWeight = edges.get(fromTo)
    lock.readLock().unlock()
    val weight = getWeight(typeWeight)
    return if (weight < minWeight || weight > maxWeight)
      null
    else {
      Pair(getEdgeType(typeWeight), weight)
    }
  }

  override fun getConnectionsByType(edgeType: Int, fromIdx: Int): List<Int2ObjectMap<IntSet>> {
    lock.readLock().lock()
    val cons = byType.filterIndexed { index, _ -> edgeType < 0 || edgeType == index }
      .filter { con -> con.containsKey(fromIdx) }
    lock.readLock().unlock()
    return cons
  }

  override fun getEdgeCount(): Long {
    lock.readLock().lock()
    val size = edges.size.toLong()
    lock.readLock().unlock()
    return size
  }
}