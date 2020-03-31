package me.yeuser.graph.core

import it.unimi.dsi.fastutil.Arrays

typealias Int2ShortBlock = Pair<IntArray, ShortArray>

class BigInt2Short(
    private val blockSize: Int = 4096 * 16 // assuming 16 sets of 4k memory blocks
) {

    private val blocks = mutableListOf<Int2ShortBlock>()

    fun addAll(i2sMap: Map<Int, Short>) {
        val list = i2sMap.entries.sortedBy { it.key }
        addAll(Int2ShortBlock(list.map { it.key }.toIntArray(), list.map { it.value }.toShortArray()))
    }

    private fun addAll(i2sBlock: Int2ShortBlock) {
        val inArr = i2sBlock.first
        val finds = findSortedArr(inArr)
        val toBeAdded = Int2ShortBlock(
            i2sBlock.first.filterIndexed { i, _ -> finds[i] == null }.toIntArray(),
            i2sBlock.second.filterIndexed { i, _ -> finds[i] == null }.toShortArray()
        )
        when {
            blocks.isEmpty() || blocks.last().first.size >= blockSize -> blocks.add(toBeAdded)
            else -> {
                val block = Int2ShortBlock(
                    blocks.last().first + toBeAdded.first,
                    blocks.last().second + toBeAdded.second
                )
                Arrays.mergeSort(0, block.first.size,
                    { k1, k2 -> block.first[k1].compareTo(block.first[k2]) },
                    { a, b ->
                        val itmp = block.first[a]
                        block.first[a] = block.first[b]
                        block.first[b] = itmp

                        val stmp = block.second[a]
                        block.second[a] = block.second[b]
                        block.second[b] = stmp
                    })
                blocks[blocks.lastIndex] = block
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

    val size: Int get() = blocks.sumBy { it.first.size }
}

typealias BlockIndicesValue = Triple<Int, Int, Short>