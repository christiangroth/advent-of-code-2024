package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.skipBlank
import kotlin.math.abs

/*
So, a report only counts as safe if both of the following are true:

The levels are either all increasing or all decreasing.
Any two adjacent levels differ by at least one and at most three.
*/
private data class Report(val levels: List<Int>) {
  private val sortedLevels = levels.sorted()
  private fun isSorted() = levels == sortedLevels || levels.asReversed() == sortedLevels

  private val levelDifferences = levels.mapIndexedNotNull { index, level ->
    if (index == levels.lastIndex) {
      null
    } else {
      abs(level - levels[index + 1])
    }
  }

  fun isSafe() = isSorted() && levelDifferences.all { it in SAFE_LEVEL_MIN..SAFE_LEVEL_MAX }

  companion object {
    const val SAFE_LEVEL_MIN = 1
    const val SAFE_LEVEL_MAX = 3
  }
}

/*
The Problem Dampener is a reactor-mounted module that lets the reactor safety systems tolerate a single bad level in what would otherwise be a safe report.
It's like the bad level never happened!
Now, the same rules apply as before, except if removing a single level from an unsafe report would make it safe, the report instead counts as safe.
*/
private class ReportDampener(report: Report) {
  private val dampenedReports = (0 until report.levels.size).map { indexToDrop ->
    Report(report.levels.filterIndexed { index, level -> index != indexToDrop })
  }

  fun isSafe() = dampenedReports.any { it.isSafe() }
}

data object Day02 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {
    val reports = input.skipBlank()
      .map { report ->
        report.split(Regex("\\s+")).map { level -> level.toInt() }
      }
      .map { Report(it) }

    val validReports = reports.filter { it.isSafe() }
    val validReportsWithDampener = reports.filter { it.isSafe() || ReportDampener(it).isSafe() }

    return PuzzleSolution(validReports.size, validReportsWithDampener.size)
  }
}

suspend fun main() {
  Day02.run()
}
