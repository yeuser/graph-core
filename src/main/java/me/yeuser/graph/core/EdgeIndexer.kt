package me.yeuser.graph.core

import com.google.common.base.Preconditions
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import it.unimi.dsi.fastutil.longs.Long2ShortMap
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class EdgeIndexer<T>(
  expectedNumberOfEdges: Int,
  private var precision: Int,
  private val edgeTypes: Array<T>
) : IEdgeIndexer<T> {


  class VerticesMap {
    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val vertices: Int2ObjectOpenHashMap<IntRBTreeSet> = Int2ObjectOpenHashMap(32 * 1024, 1f)
    private var verticesCompact: Array<IntArray?> = emptyArray()
    private var cw = 0

    fun add(fromIdx: Int, toIdx: Int) {
      lock.writeLock().lock()
      vertices.computeIfAbsent(fromIdx) { IntRBTreeSet() }.add(toIdx)
      cw++
      if (cw > 8 * 1024) {
        vertices.forEach { (key, value) ->
          if (verticesCompact.size <= key) {
            val delta = min(1024 * 1024, max(1024, key + 1 - verticesCompact.size) * 2)
            verticesCompact = verticesCompact.copyOf(verticesCompact.size + delta)
          }
          when (val v = verticesCompact[key]) {
            null -> verticesCompact[key] = value.toIntArray()
            else -> {
              val originalSize = v.size
              val a = v.copyOf(originalSize + value.size)
              value.toIntArray().copyInto(a, originalSize)
              verticesCompact[key] = a
            }
          }

        }

        vertices.clear()
        cw = 0
      }
      lock.writeLock().unlock()
    }

    fun get(fromIdx: Int): Iterable<Int>? {
      lock.readLock().lock()
      val rbTreeSet = listOfNotNull(
        vertices[fromIdx].asIterable(), verticesCompact[fromIdx]?.asIterable()
      )
      lock.readLock().unlock()
      return rbTreeSet.flatten()
    }
  }

  private val lock: ReadWriteLock = ReentrantReadWriteLock()
  private val edges: Long2ShortMap = Long2ShortOpenHashMap(expectedNumberOfEdges, 1f)
  private val byType: Array<VerticesMap> =
    (1..edgeTypes.size).map { VerticesMap() }.toTypedArray()

  override fun addEdge(
    weight: Double, type: T, fromIdx: Int, toIdx: Int, biDirectional: Boolean
  ) {
    val edgeType = edgeTypes.indexOfFirst { it == type }
    val typeWeight = getTypeWeight(edgeType, weight)
    val connectionTypeMap = byType[edgeType]
    addEdgeInternal(fromIdx, toIdx, typeWeight, connectionTypeMap)
    if (biDirectional) {
      addEdgeInternal(toIdx, fromIdx, typeWeight, connectionTypeMap)
    }
  }

  private fun addEdgeInternal(
    fromIdx: Int, toIdx: Int, typeWeight: Short, connectionTypeMap: VerticesMap
  ) {
    lock.writeLock().lock()
    val fromTo = getFromTo(fromIdx, toIdx)
    edges[fromTo] = typeWeight
    connectionTypeMap.add(fromIdx, toIdx)
    lock.writeLock().unlock()
  }

  private fun getFromTo(fromIdx: Int, toIdx: Int): Long {
    return fromIdx.toLong() and 0xFFFFFFFFL shl 32 or (toIdx.toLong() and 0xFFFFFFFFL)
  }

  private fun getTypeWeight(t: Int, weight: Double): Short {
    val w = (weight * precision).roundToInt() - 1
    return (t * precision + w).toShort()
  }

  override fun getEdgeTypeAndWeight(fromIdx: Int, toIdx: Int): Pair<T, Double> {
    val fromTo = getFromTo(fromIdx, toIdx)
    lock.readLock().lock()
    Preconditions.checkState(edges.containsKey(fromTo), "Given `from->to` edge was not found!")
    val typeWeight = edges.get(fromTo)
    lock.readLock().unlock()
    return Pair(edgeTypes[getEdgeType(typeWeight)], getWeight(typeWeight))
  }

  private fun getEdgeType(typeWeight: Short): Int {
    return (typeWeight.toInt() and 0xFFFF) / precision
  }

  private fun getWeight(typeWeight: Short): Double {
    return ((typeWeight.toInt() and 0xFFFF) % precision + 1.0) / precision
  }

  override fun getConnectionsByType(type: T?, fromIdx: Int): List<Int> {
    Preconditions.checkState(type == null || edgeTypes.contains(type), "Given `type` is unknown!")
    lock.readLock().lock()
    val cons =
      byType
        .filterIndexed { index, _ -> type == null || type == edgeTypes[index] }
        .mapNotNull { it.get(fromIdx) }
        .flatten()
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

