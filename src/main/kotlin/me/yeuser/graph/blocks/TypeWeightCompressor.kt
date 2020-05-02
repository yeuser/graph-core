package me.yeuser.graph.blocks

import kotlin.math.roundToInt

object TypeWeightCompressor {
    private const val uInt16Upper = 0xFFFF

    fun roundToPrecision(precision: Int, weight: Double): Double =
        (weight * precision).roundToInt().toDouble() / precision

    fun compress(precision: Int, t: Int, weight: Double): Short {
        val w = (weight * precision).roundToInt()
        return (t * (precision + 1) + w).toShort()
    }

    fun extractType(precision: Int, typeWeight: Short) =
        (typeWeight.toInt() and uInt16Upper) / (precision + 1)

    fun extractWeight(precision: Int, typeWeight: Short) =
        ((typeWeight.toInt() and uInt16Upper) % (precision + 1)).toDouble() / precision

    fun checkOverflow(precision: Int, typeCount: Int) {

        check(typeCount * (precision + 1) <= uInt16Upper) {
            throw ArithmeticException(
                "Overflow: Given set of `precision and typeCount` creates more than $uInt16Upper distinct states."
            )
        }
    }
}