package me.yeuser.graph.core

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.max
import kotlin.math.min

class VerticesMap(
  private val bufferOpsLimit: Int = 8 * 1024,
  private val nodeEdgeExpansionMax: Int = 1024 * 1024,
  private val nodeEdgeExpansionMin: Int = 1024
) {

  private val lock: ReadWriteLock = ReentrantReadWriteLock()
  private val vertices: Int2ObjectOpenHashMap<IntRBTreeSet> =
    Int2ObjectOpenHashMap(32 * 1024, 1f)
  private var verticesCompact: Array<BigIntSet?> = emptyArray()
  private var cOps = 0

  fun add(fromIdx: Int, toIdx: Int) {
    lock.writeLock().lock()
    vertices.computeIfAbsent(fromIdx) { IntRBTreeSet() }.add(toIdx)
    lock.writeLock().unlock()
    checkShrink()
  }

  private fun checkShrink() {
    lock.writeLock().lock()
    cOps++
    if (cOps > bufferOpsLimit) {
      makeCompact()
      cOps = 0
    }
    lock.writeLock().unlock()
  }

  private fun makeCompact() {
    vertices.forEach { (key, value) ->
      if (verticesCompact.size <= key) {
        val delta = min(
          nodeEdgeExpansionMax,
          max(nodeEdgeExpansionMin, key + 1 - verticesCompact.size) * 2
        )
        verticesCompact = verticesCompact.copyOf(verticesCompact.size + delta)
      }
      var v = verticesCompact[key]
      if (v == null) {
        v = BigIntSet()
        verticesCompact[key] = v
      }
      v.addAll(value.toIntArray())
    }
    vertices.clear()
  }

  fun get(fromIdx: Int): Iterable<Int>? {
    lock.readLock().lock()
    val rbTreeSet = listOfNotNull(
      vertices.get(fromIdx)?.asIterable(),
      verticesCompact.takeIf { it.size > fromIdx }?.get(fromIdx)?.asSequence()?.asIterable()
    ).flatten()
    lock.readLock().unlock()
    checkShrink()
    return rbTreeSet.takeUnless { it.isEmpty() }
  }
}