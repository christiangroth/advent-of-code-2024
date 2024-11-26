package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.skipBlank
import kotlin.math.abs

data object Day01 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {
    val (left, right) = input.skipBlank()
      .map { line -> line.split(Regex("\\s+")).let { it[0] to it[1] } }
      .map { it.first.toInt() to it.second.toInt() }
      .let { result ->
        result.map { it.first } to result.map { it.second }
      }

    /*
    - Pair up the smallest number in the left list with the smallest number in the right list
    - then the second-smallest left number with the second-smallest right number, and so on.
    - Within each pair, figure out how far apart the two numbers are (if you pair up a 3 from the left list with a 7 from the right list, the distance apart is 4)
    - you'll need to add up all of those distances
    */
    val distance = left.sorted().zip(right.sorted())
      .sumOf { abs(it.first - it.second) }

    /*
    Calculate a total similarity score by adding up each number in the left list after multiplying it by the number of times that number appears in the right list.
    */
    val similarity = left.sumOf { leftItem ->
      leftItem * right.count { rightItem -> rightItem == leftItem }
    }

    return PuzzleSolution(distance, similarity)
  }
}

suspend fun main() {
  Day01.run()
}
