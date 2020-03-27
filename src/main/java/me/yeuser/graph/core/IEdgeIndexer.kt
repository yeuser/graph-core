package me.yeuser.graph.core

interface IEdgeIndexer<T> {
  fun addEdge(weight: Double, type: T, fromIdx: Int, toIdx: Int, biDirectional: Boolean)
  fun getEdgeTypeAndWeight(fromIdx: Int, toIdx: Int): Pair<T, Double>
  fun getConnectionsByType(type: T?, fromIdx: Int): List<Int>
  fun getEdgeCount(): Long
}