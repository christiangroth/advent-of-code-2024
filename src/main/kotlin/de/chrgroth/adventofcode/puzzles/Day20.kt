package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Maze
import de.chrgroth.adventofcode.puzzles.utils.MazeSolutionMode
import de.chrgroth.adventofcode.puzzles.utils.Path
import de.chrgroth.adventofcode.puzzles.utils.Topology
import de.chrgroth.adventofcode.puzzles.utils.TopologyTileState
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.findCoordinates
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private data class Cheat(
  val leavingTrack: Coordinate,
  val saving: Int
) {
  companion object {
    const val SAVING_THRESHOLD = 100
    const val ENHANCED_MOVES_ALLOWED = 20
  }
}

private data class TwoStepCheat(
  val cheat: Cheat,
  val cheatTile: Coordinate,
)

data object Day20 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The map consists of track (.) - including the start (S) and end (E) positions (both of which also count as track) - and walls (#).
    */
    val maze = parse(input)

    /*
    - The rules for cheating are very strict.
    - Exactly once during a race, a program may disable collision for up to 2 picoseconds.
    - This allows the program to pass through walls as if they were regular track.
    - At the end of the cheat, the program must be back on normal track again; otherwise, it will receive a segmentation fault and get disqualified.
    - Each cheat has a distinct start position (the position where the cheat is activated, just before the first move that is allowed to go through walls)
    - and end position; cheats are uniquely identified by their start position and end position.
    - How many cheats would save you at least 100 picoseconds?
    */

    val bestPath = maze.solve(MazeSolutionMode.FIRST_BEST).first()
    val twoMoveCheats = bestPath.visited.flatMap { visited ->
      setOf(Vector.UP, Vector.RIGHT, Vector.DOWN, Vector.LEFT).mapNotNull { direction ->
        val possibleCheatTile = visited.key.plus(direction)
        val possibleBackOnTrack = possibleCheatTile.plus(direction)
        if (maze.map.state(possibleCheatTile) == TopologyTileState.BLOCKED
          && maze.map.state(possibleBackOnTrack) == TopologyTileState.FREE
        ) {
          TwoStepCheat(
            cheat = Cheat(
              leavingTrack = visited.key,
              saving = bestPath.visited.getValue(possibleBackOnTrack) - visited.value - 2,
            ),
            cheatTile = possibleCheatTile,
          )
        } else {
          null
        }
      }
    }.distinctBy { it.cheatTile }

    /*
    - The latest version of the cheating rule permits a single cheat that instead lasts at most 20 picoseconds.
    - How many cheats would save you at least 100 picoseconds?
    */

    /*val twentyMoveCheats = bestPath.visited.flatMap { visited ->
      setOf(Vector.UP, Vector.RIGHT, Vector.DOWN, Vector.LEFT).map { direction ->
        visited.key.plus(direction) to direction
      }.filter { possibleCheatStart ->
        maze.map.state(possibleCheatStart.first) == TopologyTileState.BLOCKED
      }.map { cheatStart ->
        Path(
          position = cheatStart.first,
          direction = cheatStart.second,
          visited = mapOf(visited.key to 0, cheatStart.first to 1),
          score = 1
        )
      }.flatMap {
        expandToAllEndingOnTrack(
          topology = maze.map,
          backlog = listOf(it to Cheat.ENHANCED_MOVES_ALLOWED.dec()),
          emptyList()
        )
      }
    }.distinctBy {
      it.start() to it.position
    }.map {
      Cheat(
        leavingTrack = it.start(),
        saving = bestPath.visited.getValue(it.position) - bestPath.visited.getValue(it.start()) - 2,
      )
    }*/

    return PuzzleSolution(
      twoMoveCheats.count { it.cheat.saving >= Cheat.SAVING_THRESHOLD },
      // twentyMoveCheats.count { it.saving >= Cheat.SAVING_THRESHOLD },
      null,
    )
  }

  private fun parse(input: List<String>) =
    input.skipBlank().let { line ->
      Maze(
        map = Topology(
          rows = line.size,
          columns = line.maxOfOrNull { it.length } ?: 0,
          obstaclePositions = line.findCoordinates('#'),
        ),
        start = line.findCoordinates('S').first(),
        end = line.findCoordinates('E').first(),
        scoreMove = 1,
        scoreMoveAndTurn = 1,
      )
    }

  private tailrec fun expandToAllEndingOnTrack(
    topology: Topology<Unit>,
    backlog: List<Pair<Path, Int>>,
    results: List<Path>
  ): List<Path> {
    val head = backlog.firstOrNull()
      ?: return results

    val tail = backlog.drop(1)
    val newResults = if (topology.isFree(head.first.position)) {
      results.plus(head.first).distinctBy { it.position }
    } else {
      results
    }

    return if (head.second == 0) {
      expandToAllEndingOnTrack(topology, tail, newResults)
    } else {
      val expandedPaths = head.first.expand().map {
        it to head.second.dec()
      }
      expandToAllEndingOnTrack(topology, expandedPaths + tail, newResults)
    }
  }

  private fun Path.expand(): List<Path> =
    position.expand().mapNotNull { (newPos, newDirection) ->
      if (visited.containsKey(newPos)) {
        null
      } else {
        copy(
          position = newPos,
          direction = newDirection,
          visited = visited.plus(newPos to score.inc()),
          score = score.inc(),
        )
      }
    }

  private fun Coordinate.expand(): Set<Pair<Coordinate, Vector>> =
    setOf(
      this.plus(Vector.UP) to Vector.UP,
      this.plus(Vector.RIGHT) to Vector.RIGHT,
      this.plus(Vector.DOWN) to Vector.DOWN,
      this.plus(Vector.LEFT) to Vector.LEFT,
    )

  private fun Path.start(): Coordinate =
    visited.entries.first { it.value == 0 }.key
}

suspend fun main() {
  Day20.run()
}
