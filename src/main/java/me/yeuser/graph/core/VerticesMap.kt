package me.yeuser.graph.core

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.max
import kotlin.math.min

class VerticesMap {
  private val lock: ReadWriteLock =
    ReentrantReadWriteLock()
  private val vertices: Int2ObjectOpenHashMap<IntRBTreeSet> =
    Int2ObjectOpenHashMap(32 * 1024, 1f)
  private var verticesCompact: Array<BigIntSet?> = emptyArray()
  private var cw = 0

  fun add(fromIdx: Int, toIdx: Int) {
    lock.writeLock().lock()
    vertices.computeIfAbsent(fromIdx) { IntRBTreeSet() }.add(toIdx)
    cw++
    if (cw > 8 * 1024) {
      vertices.forEach { (key, value) ->
        if (verticesCompact.size <= key) {
          val delta = min(
            1024 * 1024,
            max(1024, key + 1 - verticesCompact.size) * 2
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
      cw = 0
    }
    lock.writeLock().unlock()
  }

  fun get(fromIdx: Int): Iterable<Int>? {
    lock.readLock().lock()
    val rbTreeSet = listOfNotNull(
      vertices.get(fromIdx)?.asIterable(),
      verticesCompact.takeIf { it.size > fromIdx }?.get(fromIdx)?.asSequence()?.asIterable()
    ).flatten()
    lock.readLock().unlock()
    return rbTreeSet.takeUnless { it.isEmpty() }
  }
}