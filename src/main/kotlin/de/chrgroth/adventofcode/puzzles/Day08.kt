package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Topology
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private data class Antenna(val frequency: Char, val position: Coordinate)

data object Day08 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {
    val (topology, antennasByFrequency) = input.skipBlank().let { inputLines ->
      val lines = inputLines.size
      val columns = inputLines.maxOfOrNull { it.length } ?: 0

      val antennasByFrequency = inputLines.flatMapIndexed { lineIndex: Int, line: String ->
        line.mapIndexedNotNull { rowIndex, char ->
          if (char == '.') {
            null
          } else {
            Antenna(char, Coordinate(lineIndex, rowIndex))
          }
        }
      }.fold(emptyMap<Char, List<Antenna>>()) { result, antenna ->
        result.plus(antenna.frequency to result.getOrDefault(antenna.frequency, emptyList()).plus(antenna))
      }

      Topology<Unit>(rows = lines, columns = columns) to antennasByFrequency
    }

    val antennaCombinationsWithSameFrequency = antennasByFrequency.values.flatMap { antennasSameFrequency ->
      antennasSameFrequency.foldIndexed(emptyList<Pair<Antenna, Antenna>>()) { index, result, antenna ->
        result.plus(antennasSameFrequency.subList(index + 1, antennasSameFrequency.size).map { antenna to it })
      }
    }

    /*
    - an antinode occurs at any point that is perfectly in line with two antennas of the same frequency
    - but only when one of the antennas is twice as far away as the other.
    - This means that for any pair of antennas with the same frequency, there are two antinodes, one on either side of them.
    */

    val antinodePositions: List<Coordinate> = antennaCombinationsWithSameFrequency.flatMap { (antennaA, antennaB) ->
      val vector = Vector(antennaB.position.y - antennaA.position.y, antennaB.position.x - antennaA.position.x)
      listOf(antennaA.position - vector, antennaB.position + vector)
    }.filter {
      topology.contains(it)
    }.distinct()

    /*
    - it turns out that an antinode occurs at any grid position exactly in line with at least two antennas of the same frequency, regardless of distance.
    */

    val antinodeGridPositions: List<Coordinate> = antennaCombinationsWithSameFrequency.flatMap { (antennaA, antennaB) ->
      val vector = Vector(antennaB.position.y - antennaA.position.y, antennaB.position.x - antennaA.position.x)

      val antinodesAntennaA = generateSequence(antennaA.position) {
        val nextAntinode = it - vector
        if (topology.contains(nextAntinode)) nextAntinode else null
      }.toList()

      val antinodesAntennaB = generateSequence(antennaB.position) {
        val nextAntinode = it + vector
        if (topology.contains(nextAntinode)) nextAntinode else null
      }.toList()

      antinodesAntennaA.plus(antinodesAntennaB)
    }.filter {
      topology.contains(it)
    }.distinct()

    return PuzzleSolution(antinodePositions.size, antinodeGridPositions.size)
  }
}

suspend fun main() {
  Day08.run()
}
