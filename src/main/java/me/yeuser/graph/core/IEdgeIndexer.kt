package me.yeuser.graph.core

interface IEdgeIndexer<T> {
    fun addEdge(weight: Double, type: T, fromIdx: Int, toIdx: Int, biDirectional: Boolean)
    fun getEdgeTypeAndWeight(fromIdx: Int, toIdx: Int): TypeWeight<T>
    fun getConnectionsByType(type: T?, fromIdx: Int): List<Edge<T>>
    fun getEdgeCount(): Int
}

typealias TypeWeight<T> = Pair<T, Double>
data class Edge<T> (val fromIdx: Int, val toIdx: Int, val type: T, val weight: Double)