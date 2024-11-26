package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Topology
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private data class Robot(val position: Coordinate, val direction: Vector) {

  fun move(times: Int, map: Topology<Unit>): Robot {
    var robot = this
    repeat(times) {
      robot = robot.move(map)
    }

    return robot
  }

  /*
  - These robots have a unique feature for maximum bathroom security: they can teleport.
  - When a robot would run into an edge of the space they're in, they instead teleport to the other side effectively wrapping around the edges
   */
  fun move(map: Topology<Unit>): Robot = copy(position = position.plus(direction).let {
    when {
      it.x < 0 -> it.copy(x = it.x + map.columns)
      it.x > map.columns - 1 -> it.copy(x = it.x - map.columns)
      else -> it
    }
  }.let {
    when {
      it.y < 0 -> it.copy(y = it.y + map.rows)
      it.y > map.rows - 1 -> it.copy(y = it.y - map.rows)
      else -> it
    }
  })

  companion object {
    val LINE_FORMAT = Regex("""p=(-?\d+),(-?\d+) v=(-?\d+),(-?\d+)""")
  }
}

data object Day14 : Puzzle {
  private const val PART_ONE_ITERATIONS = 100

  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - You make a list (your puzzle input) of all of the robots' current positions (p) and velocities (v), one robot per line. For example:
    - Each robot's position is given as p=x,y
    - where x represents the number of tiles the robot is from the left wall
    - and y represents the number of tiles from the top wall (when viewed from above).
    - Each robot's velocity is given as v=x,y where x and y are given in tiles per second.
    - Positive x means the robot is moving to the right, and positive y means the robot is moving down.
    */

    val robots: List<Robot> = input.skipBlank().mapNotNull { line ->
      val match = Robot.LINE_FORMAT.find(line)
      if (match != null) {
        Robot(
          position = Coordinate(y = match.groupValues[2].toInt(), x = match.groupValues[1].toInt()),
          direction = Vector(y = match.groupValues[4].toInt(), x = match.groupValues[3].toInt()),
        )
      } else {
        println("Input does not match Robot format: $line")
        null
      }
    }

    /*
    - The robots outside the actual bathroom are in a space which is 101 tiles wide and 103 tiles tall (when viewed from above).
    */
    // input Topology(rows = 103, columns = 101)
    // test Topology(rows = 7, columns = 11)
    val map = Topology<Unit>(rows = 103, columns = 101)

    val safetyFactor = robots.map {
      it.move(PART_ONE_ITERATIONS, map)
    }.computeSafetyFactor(map)

    /*
    - During the bathroom break, someone notices that these robots seem awfully similar to ones built and used at the North Pole.
    - If they're the same type of robots, they should have a hard-coded Easter egg:
    - very rarely, most of the robots should arrange themselves into a picture of a Christmas tree.
    - What is the fewest number of seconds that must elapse for the robots to display the Easter egg?
    */

    return PuzzleSolution(safetyFactor, null)
  }

  /*
  - To determine the safest area, count the number of robots in each quadrant after 100 seconds.
  - Robots that are exactly in the middle (horizontally or vertically) don't count as being in any quadrant
  - Multiplying these together gives the total safety factor.
   */
  private fun List<Robot>.computeSafetyFactor(map: Topology<Unit>): Int =
    mapNotNull {
      map.computeQuadrant(it.position)
    }.groupBy { it }.values.map { it.size }.fold(1) { result, count -> result * count }

  private enum class TopologyQuadrant {
    UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT
  }

  private fun Topology<Unit>.computeQuadrant(position: Coordinate): TopologyQuadrant? {
    return when {
      isInUpperHalf(position) && isInLeftHalf(position) -> TopologyQuadrant.UPPER_LEFT
      isInUpperHalf(position) && isInRightHalf(position) -> TopologyQuadrant.UPPER_RIGHT
      isInLowerHalf(position) && isInLeftHalf(position) -> TopologyQuadrant.LOWER_LEFT
      isInLowerHalf(position) && isInRightHalf(position) -> TopologyQuadrant.LOWER_RIGHT
      else -> null
    }
  }

  private fun Topology<Unit>.isInUpperHalf(position: Coordinate) =
    position.y < rows.floorDiv(2)

  private fun Topology<Unit>.isInLowerHalf(position: Coordinate) =
    position.y > rows.floorDiv(2)

  private fun Topology<Unit>.isInLeftHalf(position: Coordinate) =
    position.x < columns.floorDiv(2)

  private fun Topology<Unit>.isInRightHalf(position: Coordinate) =
    position.x > columns.floorDiv(2)
}

suspend fun main() {
  Day14.run()
}
