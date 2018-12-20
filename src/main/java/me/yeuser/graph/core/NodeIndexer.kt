package me.yeuser.graph.core

import it.unimi.dsi.fastutil.longs.Long2IntMap
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class NodeIndexer(expectedNumberOfNodes: Int) : INodeIndexer {
  private val lock: ReadWriteLock = ReentrantReadWriteLock()
  private val idx = AtomicInteger(0)
  private val node2idx: Long2IntMap = Long2IntOpenHashMap(expectedNumberOfNodes)
  private val idx2node: LongList = LongArrayList(expectedNumberOfNodes)

  override fun indexOf(node: Long): Int {
    lock.readLock().lock()
    val pos = node2idx.getOrDefault(node, -1)
    lock.readLock().unlock()
    if (pos > -1) return pos
    lock.writeLock().lock()
    val idx = node2idx.computeIfAbsent(node) {
      this.lock.writeLock().lock()
      val idx1 = this.idx.getAndIncrement()
      this.idx2node.add(it)
      this.lock.writeLock().unlock()
      idx1
    }
    lock.writeLock().unlock()
    return idx
  }

  override fun fromIndex(index: Int): Long {
    lock.readLock().lock()
    val node = idx2node.getLong(index)
    lock.readLock().unlock()
    return node
  }

  override fun size(): Long {
    return idx2node.size.toLong()
  }
}