package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private data class BoundaryLine(val direction: Vector, val positions: Set<Coordinate>) {
  fun connectsHorizontallyWith(position: Coordinate): Boolean =
    !positions.contains(position) &&
        positions.any { linePosition ->
          position.y == linePosition.y && (position.x == linePosition.x.dec() || position.x == linePosition.x.inc())
        }

  fun connectsVerticallyWith(position: Coordinate): Boolean =
    !positions.contains(position) &&
        positions.any { linePosition ->
          position.x == linePosition.x && (position.y == linePosition.y.dec() || position.y == linePosition.y.inc())
        }

  fun expand(position: Coordinate): BoundaryLine {
    require(connectsHorizontallyWith(position) || connectsVerticallyWith(position)) { "Can only expand to connecting position!" }
    return copy(positions = positions.plus(position))
  }

  fun canBeMerged(other: BoundaryLine): Boolean =
    other.direction == this.direction && positions.any {
      other.connectsHorizontallyWith(it) || other.connectsVerticallyWith(
        it
      )
    }

  fun merge(other: BoundaryLine): BoundaryLine =
    copy(positions = positions + other.positions)
}

private data class PlantRegion(val plant: Char, val positions: Set<Coordinate>) {

  /*
  - The area of a region is simply the number of garden plots the region contains.
   */
  inline val area: Int
    get() = positions.size

  /*
  - Each garden plot is a square and so has four sides.
  - The perimeter of a region is the number of sides of garden plots in the region that do not touch another garden plot in the same region.
   */
  val perimeter: Int by lazy {
    positions.flatMap { it.expand() }.count { !positions.contains(it) }
  }

  val fencePrice: Long
    get() = (area * perimeter).toLong()

  /*
  - Each straight section of fence counts as a side, regardless of how long it is.
   */
  val sides: List<BoundaryLine> by lazy {
    boundaries.fold(emptyList<BoundaryLine>()) { result, (boundaryPosition, boundaryDirection) ->
      val connectingLineIndex = result.indexOfFirst { line ->
        line.direction == boundaryDirection &&
            (line.connectsVerticallyWith(boundaryPosition) || line.connectsHorizontallyWith(boundaryPosition))
      }
      val connectingLine = result.getOrNull(connectingLineIndex)
      if (connectingLine == null) {
        result.plus(BoundaryLine(boundaryDirection, setOf(boundaryPosition)))
      } else {
        result.subList(0, connectingLineIndex) + listOf(connectingLine.expand(boundaryPosition)) + result.subList(
          connectingLineIndex + 1,
          result.size
        )
      }
    }.compactAll()
  }

  // All boundaries seen from insight. A boundary is position combined with a Vector leading out of this region.
  val boundaries: List<Pair<Coordinate, Vector>> by lazy {
    positions.flatMap { position ->
      setOf(
        position to Vector.UP,
        position to Vector.RIGHT,
        position to Vector.DOWN,
        position to Vector.LEFT,
      ).filter { possibleBoundary ->
        !positions.contains(position.plus(possibleBoundary.second))
      }
    }
  }

  private fun List<BoundaryLine>.compactAll(): List<BoundaryLine> {
    var lines = this
    var numberOfLines = this.size
    do {
      numberOfLines = lines.size
      lines = lines.compact()
    } while (lines.size != numberOfLines)
    return lines
  }

  private fun List<BoundaryLine>.compact(): List<BoundaryLine> =
    fold(emptyList()) { result, line ->
      val connectingLineIndex = result.indexOfFirst { other -> other.canBeMerged(line) }
      val connectingLine: BoundaryLine? = result.getOrNull(connectingLineIndex)
      if (connectingLine == null) {
        result.plus(line)
      } else {
        result.subList(
          0,
          connectingLineIndex
        ) + listOf(connectingLine.merge(line)) + result.subList(connectingLineIndex + 1, result.size)
      }
    }

  val discountFencePrice: Long
    get() = (area * sides.count()).toLong()

  private fun Coordinate.expand(): Set<Coordinate> =
    setOf(
      this.plus(Vector.UP),
      this.plus(Vector.RIGHT),
      this.plus(Vector.DOWN),
      this.plus(Vector.LEFT),
    )

  fun touches(position: Coordinate): Boolean {
    return positions.any {
      it.expand().contains(position)
    }
  }

  fun expand(position: Coordinate): PlantRegion {
    require(touches(position)) { "Can only expand to touching position!" }
    return copy(positions = positions.plus(position))
  }

  fun canBeMerged(other: PlantRegion): Boolean =
    other.plant == this.plant && positions.any { other.touches(it) }

  fun merge(other: PlantRegion): PlantRegion =
    copy(positions = positions + other.positions)
}

data object Day12 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The topographic map indicates the height at each position using a scale from 0 (lowest) to 9 (highest)
    */
    val plantRegions: List<PlantRegion> = input.skipBlank().flatMapIndexed { lineIndex, line ->
      line.mapIndexed { columnIndex, plant ->
        plant to Coordinate(y = lineIndex, x = columnIndex)
      }
    }.fold<Pair<Char, Coordinate>, List<PlantRegion>>(emptyList()) { result, (plant, position) ->
      val touchingRegionIndex = result.indexOfFirst { region -> region.plant == plant && region.touches(position) }
      val touchingRegion: PlantRegion? = result.getOrNull(touchingRegionIndex)
      if (touchingRegion == null) {
        result.plus(PlantRegion(plant, setOf(position)))
      } else {
        result.subList(0, touchingRegionIndex) + listOf(touchingRegion.expand(position)) + result.subList(
          touchingRegionIndex + 1,
          result.size
        )
      }
    }.compactAll()

    return PuzzleSolution(plantRegions.sumOf { it.fencePrice }, plantRegions.sumOf { it.discountFencePrice })
  }

  private fun List<PlantRegion>.compactAll(): List<PlantRegion> {
    var regions = this
    var numberOfRegions = this.size
    do {
      numberOfRegions = regions.size
      regions = regions.compact()
    } while (regions.size != numberOfRegions)
    return regions
  }

  private fun List<PlantRegion>.compact(): List<PlantRegion> =
    fold(emptyList()) { result, region ->
      val touchingRegionIndex = result.indexOfFirst { other -> other.canBeMerged(region) }
      val touchingRegion: PlantRegion? = result.getOrNull(touchingRegionIndex)
      if (touchingRegion == null) {
        result.plus(region)
      } else {
        result.subList(0, touchingRegionIndex) + listOf(touchingRegion.merge(region)) + result.subList(
          touchingRegionIndex + 1,
          result.size
        )
      }
    }
}

suspend fun main() {
  Day12.run()
}
