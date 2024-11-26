package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.Warehouse.Companion.COORDINATE_Y_FACTOR
import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Topology
import de.chrgroth.adventofcode.puzzles.utils.TopologyTileState
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.findCoordinates

private sealed interface Obstacle {
  fun move(topology: Topology<Obstacle>, direction: Vector): Topology<Obstacle>?

  /*
  - The lanternfish use their own custom Goods Positioning System (GPS for short) to track the locations of the boxes.
  - The GPS coordinate of a box is equal to 100 times its distance from the top edge of the map plus its distance from the left edge of the map.
  - (This process does not stop at wall tiles; measure all the way to the edges of the map.)
  */
  fun computeGps(): Long
}

private data class Box(val position: Coordinate) : Obstacle {
  override fun move(topology: Topology<Obstacle>, direction: Vector): Topology<Obstacle>? {
    val target = position.plus(direction)
    return when (val targetState = topology.state(target)) {
      TopologyTileState.BLOCKED, TopologyTileState.OUTSIDE -> null
      TopologyTileState.FREE, TopologyTileState.POS -> {
        val newTopology = topology.copy(
          pointsOfInterest = topology.pointsOfInterest
            .minus(this.position to this)
            .plus(target to copy(position = target))
        )

        if (targetState == TopologyTileState.POS) {
          topology.pointsOfInterest.first { it.first == target }.second.move(newTopology, direction)
        } else {
          newTopology
        }
      }
    }
  }

  override fun computeGps(): Long =
    (COORDINATE_Y_FACTOR * position.y) + position.x
}

private data class WideBox(val positionLeft: Coordinate, val positionRight: Coordinate) : Obstacle {
  override fun move(topology: Topology<Obstacle>, direction: Vector): Topology<Obstacle>? =
    when (direction) {
      in setOf(Vector.DOWN, Vector.UP) -> moveVertically(topology, direction)
      in setOf(Vector.LEFT, Vector.RIGHT) -> moveHorizontally(topology, direction)
      else -> null.also {
        println("Unknown direction for movement: $direction")
      }
    }

  fun moveVertically(topology: Topology<Obstacle>, direction: Vector): Topology<Obstacle>? {
    val leftTarget = positionLeft.plus(direction)
    val leftTargetState = topology.state(leftTarget)
    val rightTarget = positionRight.plus(direction)
    val rightTargetState = topology.state(rightTarget)

    return if (leftTargetState in listOf(TopologyTileState.BLOCKED, TopologyTileState.OUTSIDE) ||
      rightTargetState in listOf(TopologyTileState.BLOCKED, TopologyTileState.OUTSIDE)
    ) {
      null
    } else {
      val movedObstacle = copy(positionLeft = leftTarget, positionRight = rightTarget)
      var newTopology: Topology<Obstacle>? = topology.copy(
        pointsOfInterest = topology.pointsOfInterest
          .minus(this.positionLeft to this)
          .minus(this.positionRight to this)
          .plus(leftTarget to movedObstacle)
          .plus(rightTarget to movedObstacle)
      )

      setOfNotNull(
        topology.pointsOfInterest.firstOrNull { it.first == leftTarget },
        topology.pointsOfInterest.firstOrNull { it.first == rightTarget },
      ).forEach { pos ->
        newTopology?.let {
          newTopology = pos.second.move(it, direction)
        }
      }

      newTopology
    }
  }

  fun moveHorizontally(topology: Topology<Obstacle>, direction: Vector): Topology<Obstacle>? {
    val target = if (Vector.LEFT == direction) positionLeft.plus(direction) else positionRight.plus(direction)
    val targetState = topology.state(target)

    return if (targetState in listOf(TopologyTileState.BLOCKED, TopologyTileState.OUTSIDE)) {
      null
    } else {
      val movedObstacle =
        copy(positionLeft = positionLeft.plus(direction), positionRight = positionRight.plus(direction))
      var newTopology: Topology<Obstacle> = topology.copy(
        pointsOfInterest = topology.pointsOfInterest
          .minus(this.positionLeft to this)
          .minus(this.positionRight to this)
          .plus(movedObstacle.positionLeft to movedObstacle)
          .plus(movedObstacle.positionRight to movedObstacle)
      )

      topology.pointsOfInterest.firstOrNull { it.first == target }?.second?.move(newTopology, direction) ?: newTopology
    }
  }

  override fun computeGps(): Long =
    (COORDINATE_Y_FACTOR * positionLeft.y) + positionLeft.x
}

private data class Warehouse(val topology: Topology<Obstacle>, val robot: Coordinate) {

  /*
  - As the robot (@) attempts to move, if there are any boxes (O) in the way, the robot will also attempt to push those boxes.
  - However, if this action would cause the robot or a box to move into a wall (#), nothing moves instead, including the robot.
  */
  fun move(direction: Vector): Warehouse {
    val target = robot.plus(direction)
    val state = topology.state(target)
    return when (state) {
      TopologyTileState.BLOCKED, TopologyTileState.OUTSIDE -> this
      TopologyTileState.FREE -> copy(robot = target)
      TopologyTileState.POS -> {
        val obstacle = topology.pointsOfInterest.first { it.first == target }.second
        val newTopology = obstacle.move(topology, direction)
        if (newTopology != null) {
          copy(
            topology = newTopology,
            robot = target
          )
        } else {
          this
        }
      }
    }
  }

  fun computeSmallBoxGpsCoordinates(): List<Pair<Coordinate, Long>> =
    topology.pointsOfInterest.distinctBy { it.second }.map {
      it.first to (COORDINATE_Y_FACTOR * it.first.y) + it.first.x
    }

  companion object {
    internal const val COORDINATE_Y_FACTOR = 100
  }
}

data object Day15 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The lanternfish already have a map of the warehouse and a list of movements the robot will attempt to make (your puzzle input)
    - As the robot (@) attempts to move, if there are any boxes (O) in the way, the robot will also attempt to push those boxes.
    - However, if this action would cause the robot or a box to move into a wall (#), nothing moves instead, including the robot.
    - The rest of the document describes the moves (^ for up, v for down, < for left, > for right) that the robot will attempt to make, in order.
    - The moves form a single giant sequence; they are broken into multiple lines just to make copy-pasting easier.
    - Newlines within the move sequence should be ignored.
    */

    val (warehouse, movements) = parse(input)

    /*
    - The lanternfish would like to know the sum of all boxes' GPS coordinates after the robot finishes moving.
    - In the smaller example, the sum is 2028.
    - In the larger example, the sum of all boxes' GPS coordinates is 10092.
     */

    val warehouseAfterMovements = movements.fold(warehouse) { result, movement ->
      result.move(movement)
    }

    /*
    To get the wider warehouse's map, start with your original map and, for each tile, make the following changes:
    - If the tile is #, the new map contains ## instead.
    - If the tile is O, the new map contains [] instead.
    - If the tile is ., the new map contains .. instead.
    - If the tile is @, the new map contains @. instead.
    */

    val widenedInput = input.takeWhile { it.isNotBlank() }.map { line ->
      line.map { char ->
        when (char) {
          'O' -> "[]"
          '@' -> "@."
          else -> "$char$char"
        }
      }.joinToString(separator = "")
    }

    val widenedWarehouse = Warehouse(
      Topology(
        rows = widenedInput.size,
        columns = widenedInput.maxOfOrNull { it.length } ?: 0,
        obstaclePositions = widenedInput.findCoordinates('#'),
        pointsOfInterest = widenedInput.findCoordinates('[').flatMap {
          val wideBox = WideBox(positionLeft = it, positionRight = it.plus(Vector.RIGHT))
          listOf(it to wideBox, it.plus(Vector.RIGHT) to wideBox)
        },
      ),
      widenedInput.findCoordinates('@').first()
    )

    @Suppress("UnusedPrivateProperty")
    val widenedWarehouseAfterMovements = movements.fold(widenedWarehouse) { result, movement ->
      result.move(movement)
    }

    return PuzzleSolution(
      warehouseAfterMovements.computeSmallBoxGpsCoordinates().sumOf { it.second },
      null,
    )
  }

  private fun parse(input: List<String>) =
    input.let { line ->
      val blankLineIndex = line.indexOfFirst { it.isBlank() }
      val mapInput = line.subList(0, blankLineIndex)
      val warehouse = Warehouse(
        Topology(
          rows = mapInput.size,
          columns = mapInput.maxOfOrNull { it.length } ?: 0,
          obstaclePositions = mapInput.findCoordinates('#'),
          pointsOfInterest = mapInput.findCoordinates('O').map { it to Box(position = it) },
        ),
        mapInput.findCoordinates('@').first()
      )

      val movements = line.subList(blankLineIndex + 1, line.size).flatMap { line ->
        line.mapNotNull { movementChar ->
          when (movementChar) {
            '^' -> Vector.UP
            '>' -> Vector.RIGHT
            'v' -> Vector.DOWN
            '<' -> Vector.LEFT
            else -> null.also {
              println("Ignoring invalid movement: $line")
            }
          }
        }
      }

      warehouse to movements
    }
}

suspend fun main() {
  Day15.run()
}
