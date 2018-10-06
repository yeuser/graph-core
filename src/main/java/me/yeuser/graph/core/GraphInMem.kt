package me.yeuser.graph.core

import com.google.common.base.Preconditions
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.longs.Long2IntMap
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ShortMap
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class GraphInMem(
    expectedNumberOfNodes: Int,
    expectedNumberOfEdges: Int,
    private var precision: Int,
    vararg edgeTypes: String) : Graph {

  private val edgeTypeMap: Map<String, Int>
  private val edgeTypes: Array<String>
  private val connections: ObjectList<Int2ObjectMap<IntSet>>
  private val idx = AtomicInteger(0)
  private val idx2node: LongList
  private val node2idx: Long2IntMap
  private val edges: Long2ShortMap
  private val lock: ReadWriteLock

  init {
    assert(precision > 1) {
      "Precision must be bigger than 1."
    }
    assert(edgeTypes.size * precision <= 0xFFFF) {
      "Given number of precision and number of edgeTypes are more than acceptable range."
    }
    this.edgeTypeMap = (0 until edgeTypes.size).associateBy { i -> edgeTypes[i] }
    this.edgeTypes = edgeTypes.map { it }.toTypedArray()
    this.connections = ObjectArrayList(
        edgeTypes.map { Int2ObjectOpenHashMap<IntSet>(4096) as Int2ObjectMap<IntSet> })
    this.idx2node = LongArrayList(expectedNumberOfNodes)
    this.node2idx = Long2IntOpenHashMap(expectedNumberOfNodes)
    this.edges = Long2ShortOpenHashMap(expectedNumberOfEdges)
    this.lock = ReentrantReadWriteLock()
  }

  private fun indexNode(node: Long): Int {
    lock.writeLock().lock()
    val idx = this.idx.getAndIncrement()
    this.idx2node.add(node)
    lock.writeLock().unlock()
    return idx
  }

  override fun addEdge(from: Long, to: Long, type: String, weight: Double, biDirectional: Boolean) {
    assert(weight > 0 && weight <= 1) {
      "Weight value of an edge must always be in range (0,1]."
    }
    assert(edgeTypeMap.contains(type)) {
      "Given `type` is unknown!"
    }
    lock.writeLock().lock()
    val fromIdx = node2idx.computeIfAbsent(from) { this.indexNode(it) }
    val toIdx = node2idx.computeIfAbsent(to) { this.indexNode(it) }
    val typeWeight = getTypeWeight(type, weight)
    val connectionTypeMap = connections[edgeTypeMap[type]!!]

    addEdgeInternal(fromIdx, toIdx, typeWeight, connectionTypeMap)
    if (biDirectional) {
      addEdgeInternal(toIdx, fromIdx, typeWeight, connectionTypeMap)
    }
    lock.writeLock().unlock()
  }

  private fun addEdgeInternal(fromIdx: Int, toIdx: Int, typeWeight: Short,
                              connectionTypeMap: Int2ObjectMap<IntSet>) {
    val fromTo = getFromTo(fromIdx, toIdx)
    edges[fromTo] = typeWeight
    connectionTypeMap.computeIfAbsent(fromIdx) { IntAVLTreeSet() }.add(toIdx)
  }

  private fun getFromTo(fromIdx: Int, toIdx: Int): Long {
    return fromIdx.toLong() and 0xFFFFFFFFL shl 32 or (toIdx.toLong() and 0xFFFFFFFFL)
  }

  private fun getTypeWeight(type: String, weight: Double): Short {
    val w = Math.round(weight * precision).toInt() - 1
    val t = edgeTypeMap[type]!!
    return (t * precision + w).toShort()
  }

  override fun getEdge(from: Long, to: Long): GraphEdge {
    lock.readLock().lock()
    val fromIdx = node2idx.getOrDefault(from, -1)
    Preconditions.checkState(fromIdx >= 0, "Given `from` node was not found!")
    val toIdx = node2idx.getOrDefault(to, -1)
    Preconditions.checkState(toIdx >= 0, "Given `to` node was not found!")
    val fromTo = getFromTo(fromIdx, toIdx)
    Preconditions.checkState(edges.containsKey(fromTo), "Given `from->to` edge was not found!")
    val typeWeight = edges.get(fromTo)
    lock.readLock().unlock()
    return GraphEdge(from, to, getType(typeWeight), getWeight(typeWeight))
  }

  private fun getType(typeWeight: Short): String {
    return edgeTypes[(typeWeight.toInt() and 0xFFFF) / precision]
  }

  private fun getWeight(typeWeight: Short): Double {
    return ((typeWeight.toInt() and 0xFFFF) % precision + 1.0) / precision
  }

  override fun getEdgeConnectionsOfTypeAndWeightInRange(from: Long, type: String?,
                                                        minWeight: Double, maxWeight: Double): Iterator<GraphEdge> {
    lock.readLock().lock()
    val fromIdx = node2idx.getOrDefault(from, -1)
    lock.readLock().unlock()
    Preconditions.checkState(fromIdx >= 0, "Given `from` node was not found!")
    Preconditions.checkState(type == null || edgeTypeMap.contains(type), "Given `type` is unknown!")
    lock.readLock().lock()
    val cons = connections.asSequence()
        .filterIndexed { index, _ -> type == null || edgeTypeMap[type] == index }
        .filter { con -> con.containsKey(fromIdx) }
        .toList()
    lock.readLock().unlock()
    return cons.asSequence()
        .flatMap { it.get(fromIdx).asSequence() }
        .map { toIdx ->
          lock.readLock().lock()
          val typeWeight = edges.get(getFromTo(fromIdx, toIdx))

          val weight = getWeight(typeWeight)
          val graphEdge = if (weight < minWeight || weight > maxWeight)
            null
          else {
            val to = idx2node.getLong(toIdx)
            GraphEdge(from, to, getType(typeWeight), weight)
          }
          lock.readLock().unlock()
          graphEdge
        }
        .filterNotNull()
        .iterator()
  }

  override fun getNodeCount(): Long {
    return idx2node.size.toLong()
  }

  override fun getEdgeCount(): Long {
    return edges.size.toLong()
  }
}
