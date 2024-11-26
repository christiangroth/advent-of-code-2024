package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Maze
import de.chrgroth.adventofcode.puzzles.utils.MazeSolutionMode
import de.chrgroth.adventofcode.puzzles.utils.Topology
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private const val NUMBER_OF_ROWS_AND_COLS_PROD = 71
private const val NUMBER_OF_OBSTACLES_TO_DROP_PROD = 1024

private const val NUMBER_OF_ROWS_AND_COLS_TEST = 7
private const val NUMBER_OF_OBSTACLES_TO_DROP_TEST = 12

data object Day18 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - Your memory space is a two-dimensional grid with coordinates that range from 0 to 70 both horizontally and vertically.
    - However, for the sake of example, suppose you're on a smaller grid with coordinates that range from 0 to 6 and the following list of incoming byte positions:
    - Each byte position is given as an X,Y coordinate
    */
    val obstaclesDrops = input.skipBlank().map { line ->
      line.split(",").let {
        Coordinate(x = it[0].toInt(), y = it[1].toInt())
      }
    }

    val rowsAndCols = if (stage == Stage.PROD) NUMBER_OF_ROWS_AND_COLS_PROD else NUMBER_OF_ROWS_AND_COLS_TEST
    val initialTopology = Topology<Unit>(
      rows = rowsAndCols,
      columns = rowsAndCols,
      obstaclePositions = emptyList(),
    )

    /*
    - You and The Historians are currently in the top left corner of the memory space (at 0,0)
    - and need to reach the exit in the bottom right corner (at 70,70 in your memory space, but at 6,6 in this example).
    - You'll need to simulate the falling bytes to plan out where it will be safe to run;
    - for now, simulate just the first few bytes falling into your memory space.
     */


    val numberToDrop = if (stage == Stage.PROD) NUMBER_OF_OBSTACLES_TO_DROP_PROD else NUMBER_OF_OBSTACLES_TO_DROP_TEST
    val topology = obstaclesDrops
      .take(numberToDrop)
      .fold(initialTopology) { t, o ->
        t.copy(obstaclePositions = t.obstaclePositions.plus(o))
      }

    val maze = Maze(
      map = topology,
      start = Coordinate(x = 0, y = 0),
      end = Coordinate(x = rowsAndCols - 1, y = rowsAndCols - 1),
      scoreMove = 1,
      scoreMoveAndTurn = 1,
    )

    /*
    - You can take steps up, down, left, or right.
    - Simulate the first kilobyte (1024 bytes) falling onto your memory space.
    - Afterward, what is the minimum number of steps needed to reach the exit?
     */

    val bestPaths = maze.solve(MazeSolutionMode.FIRST_BEST)
    val bestPathsScore = bestPaths.first().score

    /*
    - What are the coordinates of the first byte that will prevent the exit from being reachable from your starting position?
    */

    var modifiedBestPaths = bestPaths
    var modifiedTopology = topology
    var remainingObstacleDrops = obstaclesDrops.drop(numberToDrop)
    while (modifiedBestPaths.isNotEmpty()) {
      val stopIndexInclusive = remainingObstacleDrops.indexOfFirst { drop ->
        modifiedBestPaths.first().visited.containsKey(drop)
      }

      modifiedTopology = remainingObstacleDrops
        .take(stopIndexInclusive.inc())
        .fold(modifiedTopology) { t, o ->
          t.copy(obstaclePositions = t.obstaclePositions.plus(o))
        }
      remainingObstacleDrops = remainingObstacleDrops.drop(stopIndexInclusive.inc())

      modifiedBestPaths = maze.copy(map = modifiedTopology).solve(MazeSolutionMode.FIRST_BEST)
    }

    return PuzzleSolution(bestPathsScore, modifiedTopology.obstaclePositions.last().let { "${it.x},${it.y}" })
  }
}

suspend fun main() {
  Day18.run()
}
