package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private data class Trail(val traces: List<Trace>) {
  val latestLevel: UByte
    get() = traces.last().level

  val latestPosition: Coordinate
    get() = traces.last().position

  val firstPosition: Coordinate
    get() = traces.first().position
}

private data class Trace(val level: UByte, val position: Coordinate) {
  companion object {
    val LEVEL_END: UByte = 9.toUByte()
    val LEVEL_START: UByte = 0.toUByte()
  }
}

data object Day10 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The topographic map indicates the height at each position using a scale from 0 (lowest) to 9 (highest)
    */
    val coordinatesByHeight: Map<UByte, List<Coordinate>> = input.skipBlank()
      .flatMapIndexed { lineIndex: Int, line: String ->
        line.mapIndexed { columnIndex, char ->
          char.digitToInt().toUByte() to Coordinate(lineIndex, columnIndex)
        }
      }.fold(emptyMap<UByte, List<Coordinate>>()) { result, charOccurrence ->
        result.plus(
          charOccurrence.first to result.getOrDefault(charOccurrence.first, emptyList()).plus(charOccurrence.second)
        )
      }.toMap()

    val trails = coordinatesByHeight.travel()
    return PuzzleSolution(trails.score(), trails.scoreInverse())
  }

  private fun Map<UByte, List<Coordinate>>.travel(): List<Trail> {
    val startingTrails = getOrDefault(Trace.LEVEL_START, emptyList()).map {
      Trail(listOf(Trace(Trace.LEVEL_START, it)))
    }

    var trails = startingTrails
    repeat(Trace.LEVEL_END.toInt()) {
      trails = trails.flatMap { travel(it) }
    }

    return trails.filter {
      it.latestLevel == Trace.LEVEL_END
    }
  }

  private fun Map<UByte, List<Coordinate>>.travel(trail: Trail): List<Trail> {
    val nextLevel = trail.latestLevel.inc()
    val neighbours = listOf(
      trail.latestPosition + Vector.UP,
      trail.latestPosition + Vector.RIGHT,
      trail.latestPosition + Vector.DOWN,
      trail.latestPosition + Vector.LEFT,
    )

    return getOrDefault(nextLevel, emptyList()).filter { it in neighbours }.map {
      trail.copy(traces = trail.traces.plus(Trace(nextLevel, it)))
    }
  }

  /*
  - a trailhead's score is the number of 9-height positions reachable from that trailhead
   */
  private fun List<Trail>.score(): Int =
    fold(emptyMap<Coordinate, Set<Coordinate>>()) { result, trail ->
      val currentValues = result.getOrDefault(trail.firstPosition, emptySet())
      result.plus(trail.firstPosition to currentValues.plus(trail.latestPosition))
    }.map { it.value.size }.sum()

  /*
  - The paper describes a second way to measure a trailhead called its rating.
  - A trailhead's rating is the number of distinct hiking trails which begin at that trailhead.
   */
  private fun List<Trail>.scoreInverse(): Int =
    fold(emptyMap<Coordinate, List<Coordinate>>()) { result, trail ->
      val currentValues = result.getOrDefault(trail.latestPosition, emptyList())
      result.plus(trail.latestPosition to currentValues.plus(trail.firstPosition))
    }.map { it.value.size }.sum()
}

suspend fun main() {
  Day10.run()
}
