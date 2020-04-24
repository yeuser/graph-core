package me.yeuser.graph.blocks

import kotlin.math.roundToInt

object TypeWeightCompressor {

    fun roundToPrecision(precision: Int, weight: Double): Double =
        (weight * precision).roundToInt().toDouble() / precision

    fun compress(precision: Int, t: Int, weight: Double): Short {
        val w = (weight * precision).roundToInt()
        return (t * (precision + 1) + w).toShort()
    }

    fun extractType(precision: Int, typeWeight: Short) =
        (typeWeight.toInt() and 0xFFFF) / (precision + 1)

    fun extractWeight(precision: Int, typeWeight: Short) =
        ((typeWeight.toInt() and 0xFFFF) % (precision + 1)).toDouble() / precision

    fun checkOverflow(precision: Int, typeCount: Int) {
        check(((typeCount - 1) * (precision + 1) + precision) < 0xFFFF) {
            throw ArithmeticException(
                "Overflow: Given set of `precision and typeCount` creates more than $0x10000 distinct states."
            )
        }
    }
}