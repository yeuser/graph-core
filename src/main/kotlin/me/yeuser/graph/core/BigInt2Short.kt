package me.yeuser.graph.core

import java.lang.IllegalStateException

typealias Int2ShortBlock = Pair<IntArray, ShortArray>

const val REMOVE_FLAG =
    (-1).toShort() // -1≈[0xFFFF in short] is the highest unsigned value and is reserved for this flag

class BigInt2Short(
    private val blockSize: Int = 4096 // 4k memory blocks
) {

    private val blocks = mutableListOf<Int2ShortBlock>()

    fun addAll(i2sMap: Map<Int, Short>) {
        val list = i2sMap.entries.sortedBy { it.key }
        addAll(Int2ShortBlock(list.map { it.key }.toIntArray(), list.map { it.value }.toShortArray()))
    }

    fun removeAll(keys: Iterable<Int>) {
        val finds = findSortedArr(keys.sorted().toIntArray())

        // Overwrite the weights with REMOVE_FLAG (-1≈[0xFFFF in short])
        finds.filterNotNull().forEach { (idx, i, _) ->
            blocks[idx].second[i] = REMOVE_FLAG
        }
    }

    private fun addAll(i2sBlock: Int2ShortBlock) {
        assert(i2sBlock.second.none { it == REMOVE_FLAG }) { "Short value '-1≈[0xFFFF]' is reserved for REMOVE_FLAG" }

        val inArr = i2sBlock.first
        val finds = findSortedArr(inArr)

        // Overwrite the weights for the existing items
        finds.forEachIndexed { idx: Int, blockIndicesValue: BlockIndicesValue? ->
            if (blockIndicesValue != null) {
                blocks[blockIndicesValue.first].second[blockIndicesValue.second] = i2sBlock.second[idx]
            }
        }

        val addition = Int2ShortBlock(
            i2sBlock.first.filterIndexed { i, _ -> finds[i] == null }.toIntArray(),
            i2sBlock.second.filterIndexed { i, _ -> finds[i] == null }.toShortArray()
        )
        when {
            blocks.isEmpty() || blocks.last().first.size >= blockSize -> blocks.add(addition)
            else -> appendToLastBlock(addition)
        }

        shrink()
    }

    private fun appendToLastBlock(addition: Int2ShortBlock) {
        val lastBlock = blocks.last()
        val len = lastBlock.first.size + addition.first.size
        val retBlock = Int2ShortBlock(IntArray(len), ShortArray(len))
        var i = 0 // lastBlock
        var j = 0 // addition
        while (i + j < len) {
            val fromAddition = when {
                i == lastBlock.first.size -> true
                j == addition.first.size -> false
                addition.first[j] < lastBlock.first[i] -> true
                lastBlock.first[i] < addition.first[j] -> false
                else -> throw IllegalStateException()
            }
            if (fromAddition) {
                retBlock.first[i + j] = addition.first[j]
                retBlock.second[i + j] = addition.second[j]
                j++
            } else {
                retBlock.first[i + j] = lastBlock.first[i]
                retBlock.second[i + j] = lastBlock.second[i]
                i++
            }
        }
        blocks[blocks.lastIndex] = retBlock
    }

    private fun shrink() {
        if (dirtyCells > memSize * 0.1) {
            blocks.indices.forEach { idx ->
                val (keys, values) = blocks[idx]
                blocks[idx] = Int2ShortBlock(
                    keys.filterIndexed { i, _ -> values[i] != REMOVE_FLAG }.toIntArray(),
                    values.filter { it != REMOVE_FLAG }.toShortArray()
                )
            }
        }
    }

    fun has(k: Int): Boolean = find(k) != null

    fun getValues(ks: IntArray): Array<Short?> = findArr(ks).map { it?.third }.toTypedArray()

    private fun findArr(ks: IntArray): Array<BlockIndicesValue?> = findSortedArr(ks.sortedArray())

    private fun findSortedArr(inArr: IntArray): Array<BlockIndicesValue?> {
        val ret = arrayOfNulls<BlockIndicesValue?>(inArr.size)
        blocks.forEachIndexed { idx, (colArr, vs) ->
            var i = 0
            var j = 0
            while (i < inArr.size && j < colArr.size) {
                when {
                    inArr[i] == colArr[j] -> {
                        ret[i] = Triple(idx, j, vs[j])
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

    fun getValue(k: Int): Short? = find(k)?.third

    private fun find(k: Int): BlockIndicesValue? {
        blocks.forEachIndexed { idx, (arr, vs) ->
            var p = 0
            var q = arr.size - 1
            while (q - p > 7) {
                val i = (p + q) / 2
                when {
                    arr[i] == k -> return BlockIndicesValue(idx, i, vs[i])
                    arr[i] > k -> q = i
                    arr[i] < k -> p = i
                }
            }
            for (i in p..q) {
                if (arr[i] == k) return BlockIndicesValue(idx, i, vs[i])
            }
        }
        return null
    }

    fun asSequence(): Sequence<Pair<Int, Short>> = ArrayList(blocks).asSequence()
        .flatMap { (ints, shorts) -> ints.indices.asSequence().map { i -> ints[i] to shorts[i] } }
        .filter { (_, v) -> v != REMOVE_FLAG }

    val size: Int get() = memSize - dirtyCells

    val memSize: Int get() = blocks.sumBy { it.first.size }

    val dirtyCells: Int get() = blocks.sumBy { it.second.count { it == REMOVE_FLAG } }
}

typealias BlockIndicesValue = Triple<Int, Int, Short>