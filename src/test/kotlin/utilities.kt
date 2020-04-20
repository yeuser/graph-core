import java.lang.management.ManagementFactory
import java.lang.management.MemoryType

/**
 * This method guarantees that garbage collection is done unlike `[System.gc]`
 */
fun gc() {
    val totalGarbageCollections = totalGarbageCollections()
    do {
        System.gc()
        System.runFinalization()
        val totalGarbageCollections2 = totalGarbageCollections()
    } while (totalGarbageCollections2 == totalGarbageCollections)
}

fun totalGarbageCollections(): Long {
    return ManagementFactory.getGarbageCollectorMXBeans()
        .map { it.collectionCount }
        .filter { it >= 0 }
        .sum()
}

fun usedMemory() =
    ManagementFactory.getMemoryPoolMXBeans()
        .filter { it.type == MemoryType.HEAP }
        .map { it.usage.used }
        .sum()