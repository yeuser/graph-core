package me.yeuser.graph.core

class BigIntSet(
    private val blockSize: Int = 4096 * 16 // assuming 16 sets of 4k memory blocks
) {

    private val blocks = mutableListOf<IntArray>()

    fun addAll(arr: IntArray) {
        if (blocks.isEmpty() || blocks.last().size >= blockSize) {
            val inArr = arr.toSortedSet().toIntArray()
            val finds = findSortedArr(inArr)
            val toBeAdded = inArr.filterIndexed { i, _ -> finds[i] == null }.toIntArray().sortedArray()
            blocks.add(toBeAdded)
        } else {
            val inArr = arr.toSortedSet().toIntArray()
            val finds = findSortedArr(inArr)
            val toBeAdded =
                inArr.filterIndexed { i, _ -> finds[i] == null }.toIntArray().sortedArray()
            blocks[blocks.lastIndex] = (blocks.last() + toBeAdded).sortedArray()
        }
    }

    fun has(k: Int): Boolean = find(k) != null

    fun findArr(ks: IntArray): Array<Pair<Int, Int>?> {
        return findSortedArr(ks.sortedArray())
    }

    private fun findSortedArr(inArr: IntArray): Array<Pair<Int, Int>?> {
        val ret = arrayOfNulls<Pair<Int, Int>?>(inArr.size)
        blocks.forEachIndexed { idx, colArr ->
            var i = 0
            var j = 0
            while (i < inArr.size && j < colArr.size) {
                when {
                    inArr[i] == colArr[j] -> {
                        ret[i] = idx to j
                        i++
                        j++
                    }
                    inArr[i] < colArr[j] -> {
                        i++
                    }
                    else -> {
                        j++
                    }
                }
            }
        }
        return ret
    }

    fun find(k: Int): Pair<Int, Int>? {
        blocks.forEachIndexed { idx, arr ->
            var p = 0
            var q = arr.size - 1
            while (q - p > 7) {
                val i = (p + q) / 2
                when {
                    arr[i] == k -> return idx to i
                    arr[i] > k -> q = i
                    arr[i] < k -> p = i
                }
            }
            for (i in p..q) {
                if (arr[i] == k) return idx to i
            }
        }
        return null
    }

    fun asSequence(): Sequence<Int> = ArrayList(blocks).asSequence().flatMap { it.asSequence() }

    val size: Int get() = blocks.sumBy { it.size }
}