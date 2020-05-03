package me.yeuser.graph.blocks

import io.kotest.matchers.shouldBe
import me.yeuser.graph.blocks.TypeWeightCompressor.checkOverflow
import me.yeuser.graph.blocks.TypeWeightCompressor.compress
import me.yeuser.graph.blocks.TypeWeightCompressor.extractType
import me.yeuser.graph.blocks.TypeWeightCompressor.extractWeight
import me.yeuser.graph.blocks.TypeWeightCompressor.roundToPrecision
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeWeightCompressorTester {

    @ParameterizedTest(name = "Sanity test #{index} for TypeWeightCompressor: {0}")
    @MethodSource("sanityCheckArguments")
    fun `test Sanity of TypeWeightCompressor`(td: TestData) {
        val (precision: Int, type: Int, weight: Double) = td
        val compressed = compress(precision, type, weight)
        (extractType(precision, compressed) to extractWeight(precision, compressed)) shouldBe
            (type to roundToPrecision(precision, weight))
    }

    data class TestData(val precision: Int, val type: Int, val weight: Double)

    private fun sanityCheckArguments() = arrayOf(
        TestData(100, 0, 0.0),
        TestData(100, 1, 0.0),
        TestData(10_000, 0, 0.0),
        TestData(100, 100, 1.0),
        TestData(100, 10, 0.99),
        TestData(1000, 0, 1.0),
        TestData(256, 254, 1.0),
        TestData(256, 254, 0.0)
    )

    @Test
    fun `test overflow of TypeWeightCompressor`() {
        assertThrows<ArithmeticException> {
            checkOverflow(255, 256)
        }
        checkOverflow(256, 255)
    }
}