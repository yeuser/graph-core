package me.yeuser.graph.core

interface IEdgeIndexer<T> {
    fun addEdge(fromIdx: Int, toIdx: Int, type: T, weight: Double, biDirectional: Boolean)
    fun removeEdge(fromIdx: Int, toIdx: Int, biDirectional: Boolean)
    fun getEdgeTypeAndWeight(fromIdx: Int, toIdx: Int): TypeWeight<T>?
    fun getConnections(fromIdx: Int): Sequence<Edge<T>>
    fun getEdgeCount(): Int
}

typealias TypeWeight<T> = Pair<T, Double>

data class Edge<T>(val fromIdx: Int, val toIdx: Int, val type: T, val weight: Double)