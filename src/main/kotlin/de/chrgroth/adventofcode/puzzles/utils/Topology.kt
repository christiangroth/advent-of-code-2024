package de.chrgroth.adventofcode.puzzles.utils

internal enum class TopologyTileState {
  FREE, BLOCKED, POS, OUTSIDE
}

internal data class Path(
  val position: Coordinate,
  val direction: Vector,
  val visited: Map<Coordinate, Int>,
  val score: Int,
)

internal data class Topology<T>(
  val rows: Int,
  val columns: Int,
  val obstaclePositions: List<Coordinate> = emptyList(),
  val pointsOfInterest: List<Pair<Coordinate, T>> = emptyList(),
) {

  fun allFree(): List<Coordinate> =
    IntRange(0, columns - 1).flatMap { x ->
      IntRange(0, rows - 1).mapNotNull { y ->
        val coordinate = Coordinate(y = y, x = x)
        if (isFree(coordinate)) {
          coordinate
        } else {
          null
        }
      }
    }

  fun state(position: Coordinate): TopologyTileState =
    when {
      !contains(position) -> TopologyTileState.OUTSIDE
      isBlocked(position) -> TopologyTileState.BLOCKED
      isPointOfInterest(position) -> TopologyTileState.POS
      else -> TopologyTileState.FREE
    }

  fun contains(coordinate: Coordinate) =
    coordinate.y in 0..<rows && coordinate.x in 0..<columns

  fun isFree(coordinate: Coordinate) =
    !isBlocked(coordinate) && !isPointOfInterest(coordinate)

  fun isBlocked(coordinate: Coordinate) =
    obstaclePositions.contains(coordinate)

  fun isPointOfInterest(coordinate: Coordinate) =
    pointsOfInterest.any { it.first == coordinate }

  fun dump(
    free: String = ".",
    blocked: String = "#",
    pos: String = "0",
    specialMappings: Map<Coordinate, String> = emptyMap()
  ) {
    println()
    IntRange(0, rows - 1).forEach { y ->
      IntRange(0, columns - 1).forEach { x ->
        val coordinate = Coordinate(y = y, x = x)
        val special = specialMappings.get(coordinate)
        if (special != null) {
          print(special)
        } else {
          print(
            when (state(coordinate)) {
              TopologyTileState.FREE -> free
              TopologyTileState.BLOCKED -> blocked
              TopologyTileState.POS -> pos
              TopologyTileState.OUTSIDE -> ""
            }
          )
        }
      }
      println()
    }
    println()
  }
}

internal data class Vector(val y: Long, val x: Long) {
  constructor(y: Int, x: Int) : this(y = y.toLong(), x = x.toLong())

  fun turn90counterclockwise(): Vector =
    turn90clockwise().turn90clockwise().turn90clockwise()

  fun turn90clockwise(): Vector =
    when (this) {
      UP -> RIGHT
      UP_RIGHT -> DOWN_RIGHT
      RIGHT -> DOWN
      DOWN_RIGHT -> DOWN_LEFT
      DOWN -> LEFT
      DOWN_LEFT -> UP_LEFT
      LEFT -> UP
      UP_LEFT -> UP_RIGHT
      else -> error("Only defined for defualt vectors!")
    }

  operator fun times(times: Long) =
    copy(y = y * times, x = x * times)

  override fun toString(): String {
    return "<$x, $y>"
  }

  companion object {
    val UP = Vector(y = -1, x = 0)
    val UP_RIGHT = Vector(y = -1, x = 1)
    val RIGHT = Vector(y = 0, x = 1)
    val DOWN_RIGHT = Vector(y = 1, x = 1)
    val DOWN = Vector(y = 1, x = 0)
    val DOWN_LEFT = Vector(y = 1, x = -1)
    val LEFT = Vector(y = 0, x = -1)
    val UP_LEFT = Vector(y = -1, x = -1)

    val directions = setOf(UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT, LEFT, UP_LEFT)
  }
}

internal data class Coordinate(val y: Long, val x: Long) {
  constructor(y: Int, x: Int) : this(y = y.toLong(), x = x.toLong())

  operator fun plus(vector: Vector): Coordinate =
    copy(
      y = y + vector.y,
      x = x + vector.x
    )

  operator fun minus(vector: Vector): Coordinate =
    copy(
      y = y - vector.y,
      x = x - vector.x
    )

  fun diff(other: Coordinate): Vector =
    Vector(y = y - other.y, x = x - other.x)

  fun isNextToAnyOf(coordinates: Set<Coordinate>, directions: Set<Vector>): Boolean =
    coordinates.any { other ->
      directions.map { other + it }.contains(this)
    }

  fun asVector(): Vector =
    Vector(y = y, x = x)

  override fun toString(): String {
    return "[$x, $y]"
  }
}

internal fun List<String>.findCoordinates(expectedText: Char): List<Coordinate> = flatMapIndexed { lineIndex, line ->
  line.mapIndexedNotNull { rowIndex, char ->
    if (char == expectedText) {
      Coordinate(y = lineIndex, x = rowIndex)
    } else {
      null
    }
  }
}
