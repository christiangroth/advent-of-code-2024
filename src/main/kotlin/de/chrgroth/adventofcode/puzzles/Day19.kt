package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.skipBlank
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

data object Day19 : Puzzle {
  @OptIn(DelicateCoroutinesApi::class)
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - To start, collect together all of the available towel patterns and the list of desired designs (your puzzle input).
    */
    val patterns = input.first().split(",").map { it.trim() }.sortedByDescending { it.length }
    val designs = input.drop(1).skipBlank()

    /*
    - How many designs are possible?
    */

    val coroutinesLock = object {}
    val solutionsPerDesign: MutableMap<String, ULong> = mutableMapOf()
    designs.map { design ->
      GlobalScope.launch {
        val solutionsCount = buildUsing(design, patterns)
        synchronized(coroutinesLock) {
          solutionsPerDesign[design] = solutionsCount
        }
      }
    }.joinAll()
    val validDesigns = solutionsPerDesign.filter { it.value > 0u }.count()

    /*
    - What do you get if you add up the number of different ways you could make each design?
    */

    val totalNumberOfDesigns = solutionsPerDesign.entries.sumOf { it.value }

    return PuzzleSolution(validDesigns, totalNumberOfDesigns)
  }

  private val CACHE_LOCK = object {}
  private val CACHE: MutableMap<String, ULong> = mutableMapOf()

  private fun buildUsing(design: String, patterns: List<String>): ULong {
    val cached = CACHE[design]
    if (cached != null) {
      return cached
    }

    return if (design.isBlank()) {
      1u
    } else {
      patterns.filter { design.startsWith(it) }.sumOf {
        buildUsing(design.substring(it.length), patterns)
      }.also {
        synchronized(CACHE_LOCK) {
          CACHE[design] = it
        }
      }
    }
  }
}

suspend fun main() {
  Day19.run()
}
