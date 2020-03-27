package me.yeuser.graph.core

class BigIntSet {
  private val col = mutableListOf<IntArray>()

  fun addAll(arr: IntArray) {
    col.add(
      arr.filterNot { has(it) }.toSortedSet().toIntArray()
    )
  }

  fun has(k: Int): Boolean = find(k) != null


  fun find(k: Int): Pair<Int, Int>? {
    col.forEachIndexed { idx, arr ->
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

  fun asSequence(): Sequence<Int> =
    ArrayList(col).asSequence().flatMap { it.asSequence() }

  fun size(): Int = col.sumBy { it.size }
}