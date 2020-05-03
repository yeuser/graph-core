package me.yeuser.graph.routing

import me.yeuser.graph.blocks.fastmap.FastMapEdgeIndexer
import me.yeuser.graph.core.GraphRouter

class BiDirGraphRouterTester : BaseRouterFunctionalityTester() {
    override fun createGraphRouter(edges: FastMapEdgeIndexer<String>): GraphRouter<String> = BiDirGraphRouter(edges)
}