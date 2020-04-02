package me.yeuser.graph.core

interface INodeIndexer {
    fun indexOf(node: Long): Int
    fun fromIndex(index: Int): Long
    fun size(): Int
}