package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Maze
import de.chrgroth.adventofcode.puzzles.utils.MazeSolutionMode
import de.chrgroth.adventofcode.puzzles.utils.Topology
import de.chrgroth.adventofcode.puzzles.utils.findCoordinates
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private const val SCORE_MOVE = 1
private const val SCORE_TURN = 1000
private const val SCORE_MOVE_AND_TURN = SCORE_MOVE + SCORE_TURN

data object Day16 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The Reindeer start on the Start Tile (marked S) facing East and need to reach the End Tile (marked E).
    - They can move forward one tile at a time (increasing their score by 1 point), but never into a wall (#).
    - They can also rotate clockwise or counterclockwise 90 degrees at a time (increasing their score by 1000 points).
    */

    val maze = input.skipBlank().let { line ->
      Maze(
        map = Topology(
          rows = line.size,
          columns = line.maxOfOrNull { it.length } ?: 0,
          obstaclePositions = line.findCoordinates('#'),
        ),
        start = line.findCoordinates('S').first(),
        end = line.findCoordinates('E').first(),
        scoreMove = SCORE_MOVE,
        scoreMoveAndTurn = SCORE_MOVE_AND_TURN,
      )
    }

    /*
    - What is the lowest score a Reindeer could possibly get?
     */

    val bestPaths = maze.solve(MazeSolutionMode.ALL_BEST)
    val bestPathsScore = bestPaths.first().score

    /*
    - How many tiles are part of at least one of the best paths through the maze?
     */

    val bestPathsTilesCount = bestPaths.flatMap { it.visited.keys }.distinct().size
    return PuzzleSolution(bestPathsScore, bestPathsTilesCount)
  }
}

suspend fun main() {
  Day16.run()
}
