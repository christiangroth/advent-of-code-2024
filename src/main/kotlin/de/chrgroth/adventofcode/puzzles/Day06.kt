package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Topology
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.findCoordinates
import de.chrgroth.adventofcode.puzzles.utils.skipBlank
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private class Guard(startPos: Coordinate) {

  private var pos = startPos
  private var direction = Vector.UP
  private val trace: MutableSet<Coordinate> = mutableSetOf()
  private val traceWithDirection: MutableSet<Pair<Coordinate, Vector>> = mutableSetOf()

  /*
  - Lab guards in 1518 follow a very strict patrol protocol which involves repeatedly following these steps:
  - If there is something directly in front of you, turn right 90 degrees. Otherwise, take a step forward.
  - This process continues for a while, but the guard eventually leaves the mapped area
  */
  fun patrol(map: Topology<Unit>): Pair<Set<Coordinate>, Boolean> {
    var notInLoop = true
    while (map.contains(pos) && notInLoop) {
      trace.add(pos)
      // return false means element was already present, we are entering a loop...
      notInLoop = traceWithDirection.add(pos to direction)

      while (map.isBlocked(pos + direction)) {
        direction = direction.turn90clockwise()
      }
      pos += direction
    }

    return trace.toSet() to !notInLoop
  }
}

@OptIn(DelicateCoroutinesApi::class)
data object Day06 : Puzzle {

  private const val CHAR_OBSTACLE = '#'
  private const val CHAR_GUARD_START_POS = '^'

  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The map shows the current position of the guard with ^ (to indicate the guard is currently facing up from the perspective of the map).
    - Any obstructions - crates, desks, alchemical reactors, etc. - are shown as #.
    */

    val (map, startPosition) = input.skipBlank().let { inputLines ->
      Topology<Unit>(
        rows = inputLines.size,
        columns = inputLines.maxOfOrNull { it.length } ?: 0,
        obstaclePositions = inputLines.findCoordinates(CHAR_OBSTACLE)
      ) to inputLines.findCoordinates(CHAR_GUARD_START_POS).first()
    }

    /*
    Predict the path of the guard. How many distinct positions will the guard visit before leaving the mapped area?
     */
    val guardTrace = Guard(startPosition).patrol(map).first

    /*
    You need to get the guard stuck in a loop by adding a single new obstruction. How many different positions could you choose for this obstruction?
     */

    var obstaclePositionsForcingTheGuardIntoALoopCounter = 0
    val computeJobs = (0..<map.rows).flatMap { row ->
      (0..<map.columns).mapNotNull { column ->
        val coordinate = Coordinate(row, column)
        when {
          startPosition == coordinate -> null
          map.isBlocked(coordinate) -> null
          guardTrace.contains(coordinate) || coordinate.isNextToAnyOf(
            guardTrace,
            setOf(Vector.UP, Vector.RIGHT, Vector.DOWN, Vector.LEFT)
          ) -> coordinate

          else -> null
        }
      }
    }.map { possibleNewObstaclePos ->
      GlobalScope.launch(Dispatchers.Default) {
        val wasInLoop = Guard(startPosition).patrol(
          map.copy(
            obstaclePositions = map.obstaclePositions.plus(possibleNewObstaclePos)
          )
        ).second

        if (wasInLoop) {
          obstaclePositionsForcingTheGuardIntoALoopCounter++
        }
      }
    }

    computeJobs.joinAll()
    return PuzzleSolution(guardTrace.size, obstaclePositionsForcingTheGuardIntoALoopCounter)
  }
}

suspend fun main() {
  Day06.run()
}
