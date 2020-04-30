package me.yeuser.graph.blocks

import io.kotest.matchers.shouldBe
import me.yeuser.graph.blocks.TypeWeightCompressor.compress
import me.yeuser.graph.blocks.TypeWeightCompressor.extractType
import me.yeuser.graph.blocks.TypeWeightCompressor.extractWeight
import me.yeuser.graph.blocks.TypeWeightCompressor.roundToPrecision
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TypeWeightCompressorTester {

    @ParameterizedTest(name = "Sanity test {index} precision={0}, type={1}, weight={2}")
    @MethodSource("sanityCheckArguments")
    fun testSanity(precision: Int, type: Int, weight: Double) {
        val compressed = compress(precision, type, weight)
        (extractType(precision, compressed) to extractWeight(precision, compressed)) shouldBe
            (type to roundToPrecision(precision, weight))
    }

    companion object {
        @JvmStatic
        private fun sanityCheckArguments(): Array<Array<*>> = arrayOf(
            arrayOf<Any>(100, 0, 0.0),
            arrayOf<Any>(100, 1, 0.0),
            arrayOf<Any>(10_000, 0, 0.0),
            arrayOf<Any>(100, 100, 1.0),
            arrayOf<Any>(100, 10, 0.99),
            arrayOf<Any>(1000, 0, 1.0),
            arrayOf<Any>(256, 254, 1.0),
            arrayOf<Any>(256, 254, 0.0)
        )
    }
}