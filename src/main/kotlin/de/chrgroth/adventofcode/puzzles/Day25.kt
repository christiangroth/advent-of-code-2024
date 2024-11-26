package de.chrgroth.adventofcode.puzzles

private data class Lock(val maxHeight: Int, val heights: List<Int>)
private data class Key(val maxHeight: Int, val heights: List<Int>) {
  fun matches(lock: Lock): Boolean =
    maxHeight == lock.maxHeight
        && heights.size == lock.heights.size
        && heights.zip(lock.heights) { keyHeight, lockHeight -> keyHeight + lockHeight }.all { it <= maxHeight }
}

data object Day25 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - "The locks are schematics that have the top row filled (#) and the bottom row empty (.);
    - the keys have the top row empty and the bottom row filled.
    - each schematic is actually a set of columns of various heights, either extending downward from the top (for locks) or upward from the bottom (for keys)."
    - For locks, those are the pins themselves; you can convert the pins in schematics to a list of heights, one per column.
    - For keys, the columns make up the shape of the key where it aligns with pins; those can also be converted to a list of heights."
    */
    val (locks, keys) = input
      .chunked(input.indexOfFirst { it.isBlank() }.inc())
      .map { chunk ->
        chunk.filter { it.isNotBlank() }
      }.map { data ->
        if (data.isLock()) {
          data.toLock()
        } else {
          data.toKey()
        }
      }.let { all ->
        all.filterIsInstance<Lock>() to all.filterIsInstance<Key>()
      }

    /*
    - How many unique lock/key pairs fit together without overlapping in any column?
    */

    val matchingLocksToKeys = locks.flatMap { lock ->
      keys.filter { key ->
        key.matches(lock)
      }.map { lock to it }
    }

    /*
    - What do you get if you add up the number of different ways you could make each design?
    */

    return PuzzleSolution(matchingLocksToKeys.size, null)
  }

  private fun List<String>.isLock(): Boolean =
    first().all { it == '#' }

  private fun List<String>.toLock(): Lock =
    Lock(
      maxHeight = first().length,
      heights = drop(1).toHeights(),
    )

  private fun List<String>.toKey(): Key =
    Key(
      maxHeight = first().length,
      heights = reversed().drop(1).toHeights(),
    )

  private fun List<String>.toHeights(): List<Int> =
    let { data ->
      IntRange(0, data.first().length.dec()).map { column ->
        data.takeWhile { line ->
          line[column] == '#'
        }.count()
      }
    }
}

suspend fun main() {
  Day25.run()
}
