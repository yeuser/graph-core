package me.yeuser.graph.routing

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import me.yeuser.graph.blocks.TypeWeightCompressor.roundToPrecision
import me.yeuser.graph.blocks.fastmap.FastMapEdgeIndexer
import me.yeuser.graph.core.GraphRouter
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.Queue
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue

abstract class BaseRouterFunctionalityTester {

    protected abstract fun createGraphRouter(edges: FastMapEdgeIndexer<String>): GraphRouter<String>

    @Test
    fun `test simple routing`() {
        val t = "t"
        val edges = FastMapEdgeIndexer(100, 10_000, t)
        edges.add(1, 2, t, .45, false)
        edges.add(2, 3, t, .35, false)

        val router = createGraphRouter(edges)
        router.route(1, 2) shouldBe listOf(1 to .0, 2 to .45)
        router.route(2, 3) shouldBe listOf(2 to .0, 3 to .35)
        router.route(1, 3) shouldBe listOf(1 to .0, 2 to .45, 3 to .8)

        edges.add(1, 3, t, .91, false)
        router.route(1, 3) shouldBe listOf(1 to .0, 2 to .45, 3 to .8)

        edges.add(1, 3, t, .79, false)
        router.route(1, 3) shouldBe listOf(1 to .0, 3 to .79)
    }

    @Test
    fun `test routing, same source and destination`() {
        val t = "t"
        val edges = FastMapEdgeIndexer(100, 10_000, t)
        edges.add(1, 2, t, .45, true)
        edges.add(1, 3, t, .45, true)
        edges.add(3, 2, t, .45, true)

        val router = createGraphRouter(edges)
        router.route(1, 1) shouldBe listOf(1 to .0)
        router.route(2, 2) shouldBe listOf(2 to .0)
        router.route(3, 3) shouldBe listOf(3 to .0)
    }

    @Test
    fun `test routing, disconnected source and destination`() {
        val t = "t"
        val edges = FastMapEdgeIndexer(100, 10_000, t)
        edges.add(1, 2, t, .45, true)
        edges.add(1, 3, t, .45, true)
        edges.add(3, 2, t, .45, true)

        val router = createGraphRouter(edges)
        router.route(1, 1) shouldBe listOf(1 to .0)
        router.route(2, 2) shouldBe listOf(2 to .0)
        router.route(3, 3) shouldBe listOf(3 to .0)
    }

    @RepeatedTest(5, name = RepeatedTest.LONG_DISPLAY_NAME)
    @Test
    fun `test routing, complex meshed graph`() {
        val MESH_COUNT = 20
        val MESH_NODE_COUNT = 100

        val t = "t"
        val edges = FastMapEdgeIndexer(100, 10_000, t)

        // initialize random data
        val random = Random()

        val connections = Array(MESH_COUNT * MESH_NODE_COUNT) { mutableMapOf<Int, Double>() }
        repeat(MESH_COUNT) { k -> // #islands of meshed graphs
            repeat(MESH_NODE_COUNT) { i -> // from #nodes
                repeat(MESH_NODE_COUNT) { j -> // to #nodes
                    // randomly (50-50 chance) connect or disconnect the nodes with random weight
                    if (i != j && random.nextBoolean())
                        connections[k * MESH_NODE_COUNT + i][k * MESH_NODE_COUNT + j] =
                            roundToPrecision(10_000, random.nextDouble())
                }
            }
        }
        repeat(MESH_COUNT) { k1 -> // from source mesh
            repeat(MESH_COUNT) { k2 -> // to destination mesh
                // give 50-50 chance for connecting the mesh islands
                if (k1 != k2 && random.nextBoolean())
                    repeat(2) { // connect two nodes from source mesh to two nodes at destination mesh
                        val i = random.nextInt(MESH_NODE_COUNT)
                        val j = random.nextInt(MESH_NODE_COUNT)
                        connections[k1 * MESH_NODE_COUNT + i][k2 * MESH_NODE_COUNT + j] =
                            roundToPrecision(10_000, random.nextDouble())
                    }
            }
        }

        // insert generated connection data into edge-indexer
        connections.forEachIndexed { i, cons ->
            cons.forEach { (j, w) ->
                edges.add(i, j, t, w, false)
            }
        }

        // randomly do find routes
        repeat(20) {
            val bestPaths = mutableMapOf<Int, Pair<Double, IntArray>>()
            val queue: Queue<Int> = ConcurrentLinkedQueue()

            val (i, j) = random.ints(0, MESH_COUNT)
                .distinct()
                .limit(2)
                .toArray()
                .map { MESH_NODE_COUNT * it + random.nextInt(MESH_NODE_COUNT) }


            bestPaths[i] = 0.0 to intArrayOf(i)
            connections[i].forEach { (j, w) ->
                queue.add(j)
                bestPaths[j] = w to intArrayOf(i, j)
            }
            while (queue.isNotEmpty()) {
                val cur = queue.remove()
                connections[cur].forEach { (nxt, dw) ->
                    val w = bestPaths[cur]!!.first + dw
                    if (bestPaths[nxt]?.first ?: Double.MAX_VALUE > w) {
                        queue.add(nxt)
                        bestPaths[nxt] = w to bestPaths[cur]!!.second + nxt
                    }
                }
            }

            val router = createGraphRouter(edges)
            val actual = router.route(i, j)
            val expected = bestPaths[j]!!.second.map { it to bestPaths[it]!!.first }
            try {
                "%,.4f".format(actual.last().second) shouldBe "%,.4f".format(expected.last().second)
            } catch (e: AssertionError) {
                println("""
                    ${actual.size} ${actual.map { it.first }} ${actual.map { "%,.4f".format(it.second) }}
                    ${expected.size} ${expected.map { it.first }} ${expected.map { "%,.4f".format(it.second) }}
                """.trimIndent()
                )
                throw e
            }
        }
    }

    private infix fun List<Pair<Int, Double>>.shouldBe(expected: List<Pair<Int, Double>>) {
        size shouldBe expected.size
        forEachIndexed { i, (n, w) ->
            n shouldBe expected[i].first
            w shouldBe (expected[i].second plusOrMinus 1E-3)
        }
    }
}