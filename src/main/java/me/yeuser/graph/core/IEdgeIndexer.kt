package me.yeuser.graph.core

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntSet

interface IEdgeIndexer {
  fun addEdge(weight: Double, edgeType: Int, fromIdx: Int, toIdx: Int, biDirectional: Boolean)
  fun getFromTo(fromIdx: Int, toIdx: Int): Long
  fun getTypeWeight(t: Int, weight: Double): Short
  fun getEdgeTypeAndWeight(fromIdx: Int, toIdx: Int): Pair<Int, Double>
  fun getEdgeType(typeWeight: Short): Int
  fun getWeight(typeWeight: Short): Double
  fun getConnectionsByType(edgeType: Int, fromIdx: Int): List<Int2ObjectMap<IntSet>>
  fun getEdgeCount(): Long
  fun tw(fromIdx: Int, toIdx: Int, minWeight: Double, maxWeight: Double): Pair<Int, Double>?
}