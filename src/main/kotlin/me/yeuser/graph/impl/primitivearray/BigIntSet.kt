package me.yeuser.graph.impl.primitivearray

private typealias BlockIndices = Pair<Int, Int>

internal class BigIntSet(
    private val blockSize: Int = 4096 // 4k memory blocks
) {

    private val blocks = mutableListOf<IntArray>()
    private val dirtyIntSet = mutableSetOf<Int>()

    fun addAll(ints: Iterable<Int>) {
        insert(ints.sortedBy { it }.toIntArray())
    }

    private fun insert(i2sBlock: IntArray) {
        assert(i2sBlock.none { it < 0 }) { "Negative Index value means removed items" }

        val finds = findSortedArr(i2sBlock)

        dirtyIntSet.removeAll(i2sBlock.toList())

        val addition = i2sBlock.filterIndexed { i, _ -> finds[i] == null }.toIntArray()

        when {
            blocks.isEmpty() || blocks.last().size >= blockSize -> blocks.add(addition)
            else -> appendToLastBlock(addition)
        }

        shrink()
    }

    private fun appendToLastBlock(addition: IntArray) {
        val lastBlock = blocks.last()
        val len = lastBlock.size + addition.size
        val retBlock = IntArray(len)
        var i = 0 // lastBlock
        var j = 0 // addition
        while (i + j < len) {
            val fromAddition = when {
                i == lastBlock.size -> true
                j == addition.size -> false
                addition[j] < lastBlock[i] -> true
                lastBlock[i] < addition[j] -> false
                else -> throw IllegalStateException()
            }
            if (fromAddition) {
                retBlock[i + j] = addition[j]
                j++
            } else {
                retBlock[i + j] = lastBlock[i]
                i++
            }
        }
        blocks[blocks.lastIndex] = retBlock
    }

    private fun shrink() {
        if (dirtyCells > memSize * 0.1) {
            blocks.indices.forEach { idx ->
                blocks[idx] = blocks[idx].filter { it !in dirtyIntSet }.toIntArray()
            }
            dirtyIntSet.clear()
        }
    }

    fun removeAll(keys: Iterable<Int>) {
        val inArr = keys.sorted().filter { it !in dirtyIntSet }.toIntArray()
        val finds = findSortedArr(inArr)

        val toBeRemoved = finds.withIndex()
            .filter { it.value != null }
            .map { inArr[it.index] }

        dirtyIntSet.addAll(toBeRemoved)
    }

    fun has(k: Int): Boolean = k !in dirtyIntSet && find(k) != null

    private fun findSortedArr(inArr: IntArray): Array<BlockIndices?> {
        val ret = arrayOfNulls<BlockIndices?>(inArr.size)
        blocks.forEachIndexed { idx, colArr ->
            var i = 0
            var j = 0
            while (i < inArr.size && j < colArr.size) {
                when {
                    inArr[i] == colArr[j] -> {
                        ret[i] = BlockIndices(idx, j)
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

    private fun find(k: Int): BlockIndices? {
        blocks.forEachIndexed { idx, arr ->
            var p = 0
            var q = arr.size - 1
            while (q - p > 7) {
                val i = (p + q) / 2
                when {
                    arr[i] == k -> return BlockIndices(idx, i)
                    arr[i] > k -> q = i
                    arr[i] < k -> p = i
                }
            }
            for (i in p..q) {
                if (arr[i] == k) return BlockIndices(idx, i)
            }
        }
        return null
    }

    fun asSequence(): Sequence<Int> =
        ArrayList(blocks)
            .asSequence()
            .flatMap {
                it.asSequence()
                    .filter { it !in dirtyIntSet }
            }

    val size: Int get() = memSize - dirtyCells

    val memSize: Int get() = blocks.sumBy { it.size }

    val dirtyCells: Int get() = dirtyIntSet.size
}