package de.chrgroth.adventofcode.puzzles.utils

internal enum class MazeSolutionMode {
  FIRST_BEST, ALL_BEST, ALL
}

internal data class Maze(
  val map: Topology<Unit>,
  val start: Coordinate,
  val end: Coordinate,
  val scoreMove: Int,
  val scoreMoveAndTurn: Int
) {

  private val pathScoreCache: MutableMap<String, Int> = mutableMapOf()

  fun solve(mode: MazeSolutionMode): List<Path> =
    solveRecursively(
      backlog = setOf(Path(start, Vector.RIGHT, mapOf(start to 0), 0)),
      solutions = emptyList(),
      mode = mode
    )

  private tailrec fun solveRecursively(backlog: Set<Path>, solutions: List<Path>, mode: MazeSolutionMode): List<Path> {
    if (backlog.isEmpty()) {
      return solutions
    }

    val newSolutions = solutions.toMutableList()
    val newBacklog = backlog.flatMap { current ->
      if (current.position == end) {
        if (mode == MazeSolutionMode.FIRST_BEST || mode == MazeSolutionMode.ALL_BEST) {
          if (newSolutions.isNotEmpty() && current.score < newSolutions.first().score) {
            newSolutions.clear()
          }

          if (newSolutions.isEmpty() || current.score == newSolutions.first().score) {
            newSolutions.add(current)
          }
        } else {
          newSolutions.add(current)
        }
        emptyList()
      } else {
        listOfNotNull(
          map.explore(current, current.direction, scoreMove),
          map.explore(current, current.direction.turn90clockwise(), scoreMoveAndTurn),
          map.explore(current, current.direction.turn90counterclockwise(), scoreMoveAndTurn),
        )
      }
    }.filter {
      pathScoreCache.getValue(it.computeCacheKey()) == it.score
    }.filter {
      if (mode == MazeSolutionMode.FIRST_BEST || mode == MazeSolutionMode.ALL_BEST) {
        newSolutions.isEmpty() || it.score <= newSolutions.first().score
      } else {
        true
      }
    }.toSet()

    val optimizedNewBacklog = getOptimizedNewBacklog(mode, newBacklog)
    return solveRecursively(optimizedNewBacklog, newSolutions, mode)
  }

  private fun Topology<Unit>.explore(path: Path, direction: Vector, score: Int): Path? =
    path.position.plus(direction).let { targetTile ->
      if (path.visited.contains(targetTile) || state(targetTile) != TopologyTileState.FREE) {
        null
      } else {
        val targetScore = path.score + score
        path.copy(
          position = targetTile,
          direction = direction,
          visited = path.visited.plus(targetTile to targetScore),
          score = targetScore
        ).also { target ->
          val cachedMinScore = pathScoreCache[target.computeCacheKey()]
          if (cachedMinScore == null || cachedMinScore > target.score) {
            pathScoreCache[target.computeCacheKey()] = target.score
          }
        }
      }
    }

  private fun Path.computeCacheKey(): String =
    position.toString().plus(if (scoreMove == scoreMoveAndTurn) "" else direction.toString())

  private fun getOptimizedNewBacklog(mode: MazeSolutionMode, newBacklog: Set<Path>) =
    if (mode == MazeSolutionMode.FIRST_BEST || mode == MazeSolutionMode.ALL_BEST) {
      if (scoreMove == scoreMoveAndTurn) {
        newBacklog.groupBy {
          it.position
        }.flatMap {
          it.value.takeOnlyMaxScore(mode == MazeSolutionMode.ALL_BEST)
        }
      } else {
        newBacklog.groupBy {
          it.position to it.direction
        }.flatMap {
          it.value.takeOnlyMaxScore(mode == MazeSolutionMode.ALL_BEST)
        }
      }
    } else {
      newBacklog
    }.toSet()

  private fun List<Path>.takeOnlyMaxScore(findAllBestPaths: Boolean): List<Path> {
    val minScore = this.minOf { it.score }
    return if (findAllBestPaths) {
      this.filter { it.score == minScore }
    } else {
      listOf(this.first { it.score == minScore })
    }
  }
}
