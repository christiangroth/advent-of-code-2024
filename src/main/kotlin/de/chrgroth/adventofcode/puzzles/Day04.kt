package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private val CHARACTER_FILTER = listOf('X', 'M', 'A', 'S')

data object Day04 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {
    val characterPositions = input.skipBlank()
      .flatMapIndexed { lineIndex: Int, line: String ->
        line.filter { CHARACTER_FILTER.contains(it.uppercaseChar()) }.mapIndexed { columnIndex, char ->
          char to Coordinate(lineIndex, columnIndex)
        }
      }.fold(mutableMapOf<Char, MutableList<Coordinate>>()) { result, charOccurrence ->
        result.apply {
          getOrPut(charOccurrence.first) { mutableListOf() }.add(charOccurrence.second)
        }
      }.map {
        it.key to it.value.toList()
      }.toMap()

    /*
    - She only has to find one word: XMAS.
    - This word search allows words to be horizontal, vertical, diagonal, written backwards, or even overlapping other words.
    - It's a little unusual, though, as you don't merely need to find one instance of XMAS - you need to find all of them.
    */
    val xmasWords = characterPositions.getOrDefault('X', emptyList())
      .flatMap { xCoordinate ->
        Vector.directions.mapNotNull { direction ->
          val mCoordinate = xCoordinate + direction
          val mExists = characterPositions.containsAt('M', mCoordinate)

          val aCoordinate = mCoordinate + direction
          val aExists = characterPositions.containsAt('A', aCoordinate)

          val sCoordinate = aCoordinate + direction
          val sExists = characterPositions.containsAt('S', sCoordinate)

          if (mExists && aExists && sExists) {
            xCoordinate
          } else {
            null
          }
        }
      }

    /*
    - this isn't actually an XMAS puzzle; it's an X-MAS puzzle in which you're supposed to find two MAS in the shape of an X
    - Within the X, each MAS can be written forwards or backwards.
    */
    val xShapedMasWords = characterPositions.getOrDefault('A', emptyList())
      .filter { aCoordinate ->

        val upLeftCoordinate = aCoordinate + Vector.UP_LEFT
        val downRightCoordinate = aCoordinate + Vector.DOWN_RIGHT
        val masOrSamStartingUpLeft = (
            characterPositions.containsAt('M', upLeftCoordinate) &&
                characterPositions.containsAt('S', downRightCoordinate)
            ) || (
            characterPositions.containsAt('S', upLeftCoordinate) &&
                characterPositions.containsAt('M', downRightCoordinate)
            )

        val upRightCoordinate = aCoordinate + Vector.UP_RIGHT
        val downLeftCoordinate = aCoordinate + Vector.DOWN_LEFT
        val masOrSamStartingUpRight = (
            characterPositions.containsAt('M', upRightCoordinate) &&
                characterPositions.containsAt('S', downLeftCoordinate)
            ) || (
            characterPositions.containsAt('S', upRightCoordinate) &&
                characterPositions.containsAt('M', downLeftCoordinate)
            )

        masOrSamStartingUpLeft && masOrSamStartingUpRight
      }

    return PuzzleSolution(xmasWords.size, xShapedMasWords.size)
  }
}

private fun Map<Char, List<Coordinate>>.containsAt(char: Char, coordinate: Coordinate): Boolean =
  getOrDefault(char, emptyList()).contains(coordinate)

suspend fun main() {
  Day04.run()
}
