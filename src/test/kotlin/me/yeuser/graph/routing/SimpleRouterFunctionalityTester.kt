package me.yeuser.graph.routing

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import me.yeuser.graph.blocks.fastmap.FastMapEdgeIndexer
import org.junit.jupiter.api.Test

class SimpleRouterFunctionalityTester {

    @Test
    fun `test simple routing`() {
        val type1 = "t1"
        val edges = FastMapEdgeIndexer(100, 10_000, type1)
        edges.add(1, 2, type1, .45, false)
        edges.add(2, 3, type1, .35, false)

        val router = SimpleGraphRouter(edges)
        router.route(1, 2) shouldBe listOf(1 to .0, 2 to .45)
        router.route(2, 3) shouldBe listOf(2 to .0, 3 to .35)
        router.route(1, 3) shouldBe listOf(1 to .0, 2 to .45, 3 to .8)

        edges.add(1, 3, type1, .91, false)
        router.route(1, 3) shouldBe listOf(1 to .0, 2 to .45, 3 to .8)

        edges.add(1, 3, type1, .79, false)
        router.route(1, 3) shouldBe listOf(1 to .0, 3 to .79)
    }

    fun List<Pair<Int, Double>>.shouldBe(expected: List<Pair<Int, Double>>) {
        size shouldBe expected.size
        forEachIndexed { i, (n, w) ->
            n shouldBe expected[i].first
            w shouldBe (expected[i].second plusOrMinus 1E-4)
        }
    }
}