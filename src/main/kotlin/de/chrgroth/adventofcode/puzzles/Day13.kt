package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.Coordinate
import de.chrgroth.adventofcode.puzzles.utils.Vector
import de.chrgroth.adventofcode.puzzles.utils.skipBlank
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.min

/*
- The claw machines here are a little unusual.
- Instead of a joystick or directional buttons to control the claw, these machines have two buttons labeled A and B.
- Worse, you can't just put in a token and play; it costs 3 tokens to push the A button and 1 token to push the B button.
*/
private data class ClawMachine(val prize: Coordinate, val buttonA: Vector, val buttonB: Vector) {

  /*
  - You wonder: what is the smallest number of tokens you would have to spend to win as many prizes as possible?
   */
  fun solve(): Long? {
    val (positionA, movesNeededA) = solveOnlyUsing(buttonA)
    val prizeWonA = positionA == prize
    val costA = if (prizeWonA) movesNeededA * COST_BUTTON_A else null

    val (positionB, movesNeededB) = solveOnlyUsing(buttonB)
    val prizeWonB = positionB == prize
    val costB = if (prizeWonB) movesNeededB * COST_BUTTON_B else null

    val useB = costB != null && (costA == null || costA > costB)
    return if (useB) {
      backtrace(
        movesNeeded = movesNeededB,
        position = positionB,
        buttonUsed = buttonB,
        costUsed = COST_BUTTON_B,
        buttonToBacktrace = buttonA,
        costToBacktrace = COST_BUTTON_A,
        cost = costB,
      )
    } else {
      backtrace(
        movesNeeded = movesNeededA,
        position = positionA,
        buttonUsed = buttonA,
        costUsed = COST_BUTTON_A,
        buttonToBacktrace = buttonB,
        costToBacktrace = COST_BUTTON_B,
        cost = costA,
      )
    }
  }

  fun solveOnlyUsing(button: Vector): Pair<Coordinate, Long> {
    val (movesNeededToReachY, movesNeededToReachX) = computesMovesNeededToWinOrExceed(button)
    val movesNeeded = min(movesNeededToReachY, movesNeededToReachX)
    val position = Coordinate(y = button.y * movesNeeded, x = button.x * movesNeeded)
    return position to movesNeeded
  }

  @Suppress("LongParameterList")
  private tailrec fun backtrace(
    movesNeeded: Long,
    buttonUsed: Vector,
    costUsed: Int,
    buttonToBacktrace: Vector,
    costToBacktrace: Int,
    position: Coordinate,
    cost: Long?,
  ): Long? {
    val steppedBack = position.minus(buttonUsed)
    val steppedBackMovesNeeded = movesNeeded.dec()
    val (movesNeededToReachY, movesNeededToReachX) = computesMovesNeededToWinOrExceed(buttonToBacktrace, steppedBack)
    val prizeWon = movesNeededToReachY == movesNeededToReachX
    val newMovesNeeded = min(movesNeededToReachY, movesNeededToReachX)
    val newCost = if (prizeWon) {
      steppedBackMovesNeeded.times(costUsed).plus(newMovesNeeded.times(costToBacktrace))
    } else {
      null
    }

    return if (newCost != null && (cost == null || newCost < cost)) {
      newCost
    } else if (steppedBack == Coordinate(0, 0)) {
      cost
    } else {
      backtrace(
        movesNeeded = steppedBackMovesNeeded,
        buttonUsed = buttonUsed,
        costUsed = costUsed,
        buttonToBacktrace = buttonToBacktrace,
        costToBacktrace = costToBacktrace,
        position = steppedBack,
        cost = cost,
      )
    }
  }

  private fun computesMovesNeededToWinOrExceed(
    button: Vector,
    correction: Coordinate = Coordinate(y = 0, x = 0)
  ): Pair<Long, Long> {
    val basePosition = prize.minus(correction.asVector())
    val stepsY = basePosition.y.floorDiv(button.y)
    val yPosition = correction.plus(button.times(stepsY))
    val movesY = if (prize.y == yPosition.y) stepsY else stepsY.inc()
    val stepsX = basePosition.x.floorDiv(button.x)
    val xPosition = correction.plus(button.times(stepsX))
    val movesX = if (prize.x == xPosition.x) stepsX else stepsX.inc()

    return movesY to movesX
  }

  companion object {
    const val COST_BUTTON_A = 3
    const val COST_BUTTON_B = 1

    val PARSER_BUTTON_A = Regex("""Button A: X\+(\d+), Y\+(\d+)""")
    val PARSER_BUTTON_B = Regex("""Button B: X\+(\d+), Y\+(\d+)""")
    val PARSER_PRIZE = Regex("""Prize: X=(\d+), Y=(\d+)""")
  }
}

data object Day13 : Puzzle {
  private val coroutinesSyncLock = object {}

  @Suppress("UNREACHABLE_CODE")
  @OptIn(DelicateCoroutinesApi::class)
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - You make a list (your puzzle input) of all of the robots' current positions (p) and velocities (v), one robot per line. For example:
    - Each robot's position is given as p=x,y
    - where x represents the number of tiles the robot is from the left wall
    - and y represents the number of tiles from the top wall (when viewed from above).
    - Each robot's velocity is given as v=x,y where x and y are given in tiles per second.
    - Positive x means the robot is moving to the right, and positive y means the robot is moving down.
    */

    val clawMachines: List<ClawMachine> = input.skipBlank().fold(emptyList()) { result, line ->
      val parsedValuesButtonA = ClawMachine.PARSER_BUTTON_A.find(line)?.groupValues
      val parsedValuesButtonB = ClawMachine.PARSER_BUTTON_B.find(line)?.groupValues
      val parsedValuesPrize = ClawMachine.PARSER_PRIZE.find(line)?.groupValues

      when {
        parsedValuesButtonA != null -> result.plus(
          ClawMachine(
            prize = Coordinate(x = -1, y = -1),
            buttonA = Vector(x = parsedValuesButtonA[1].toInt(), y = parsedValuesButtonA[2].toInt()),
            buttonB = Vector(y = -1, x = -1),
          )
        )

        parsedValuesButtonB != null -> {
          result.dropLast(1).plus(
            result.last().copy(
              buttonB = Vector(x = parsedValuesButtonB[1].toInt(), y = parsedValuesButtonB[2].toInt()),
            )
          )
        }

        parsedValuesPrize != null ->
          result.dropLast(1).plus(
            result.last().copy(
              prize = Coordinate(x = parsedValuesPrize[1].toInt(), y = parsedValuesPrize[2].toInt()),
            )
          )

        else -> result.also { println("Ignoring non matching line: $line") }
      }
    }

    /*
    - Figure out how to win as many prizes as possible. What is the fewest tokens you would have to spend to win all possible prizes?
    */
    var tokensToWinAllPrizes: Long = 0
    clawMachines.map {
      GlobalScope.launch {
        val sumPart = it.solve()
        if (sumPart != null) {
          synchronized(coroutinesSyncLock) {
            tokensToWinAllPrizes += sumPart
          }
        }
      }
    }.joinAll()

    return PuzzleSolution(tokensToWinAllPrizes, null)

    /*
    - As you go to win the first prize, you discover that the claw is nowhere near where you expected it would be.
    - Due to a unit conversion error in your measurements, the position of every prize is actually 10000000000000 higher on both the X and Y axis!
    - Add 10000000000000 to the X and Y position of every prize.
    */

    @Suppress("MagicNumber")
    val prizePositionFix = Vector(10000000000000, 10000000000000)
    var tokensToWinAllMovedPrizes: Long = 0
    clawMachines.map {
      it.copy(prize = it.prize.plus(prizePositionFix))
    }.map {
      GlobalScope.launch {
        val sumPart = it.solve()
        if (sumPart != null) {
          synchronized(coroutinesSyncLock) {
            tokensToWinAllMovedPrizes += sumPart
          }
        }
      }.join()
    }

    return PuzzleSolution(tokensToWinAllPrizes, tokensToWinAllMovedPrizes)
  }
}

suspend fun main() {
  Day13.run()
}
