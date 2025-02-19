package de.chrgroth.adventofcode.puzzles

private const val MULTIPLY_FACTOR_ONE: Long = 64
private const val MULTIPLY_FACTOR_TWO: Long = 2048
private const val DIVISION_FACTOR: Long = 32
private const val PRUNE_FACTOR: Long = 16777216

private const val NUMBER_OF_ITERATIONS: Int = 2000

data object Day22 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - Fortunately, the Historian's research has uncovered the initial secret number of each buyer
    */

    val secretNumbers = input.map { line ->
      line.toLong()
    }

    /*
    - What is the sum of the 2000th secret number generated by each buyer?
    */

    val sumAfterIterations = secretNumbers.sumOf { secret ->
      computeSecret(secret, NUMBER_OF_ITERATIONS)
    }

    return PuzzleSolution(
      sumAfterIterations,
      null,
    )
  }

  /*
  - In particular, each buyer's secret number evolves into the next secret number in the sequence via the following process:
  - Calculate the result of multiplying the secret number by 64.
  - Then, mix this result into the secret number.
  - Finally, prune the secret number.
  - Calculate the result of dividing the secret number by 32.
  - Round the result down to the nearest integer.
  - Then, mix this result into the secret number.
  - Finally, prune the secret number.
  - Calculate the result of multiplying the secret number by 2048.
  - Then, mix this result into the secret number.
  - Finally, prune the secret number.
  */

  private tailrec fun computeSecret(value: Long, times: Int): Long {
    if (times == 0) {
      return value
    }

    val stepOne = value.mix(value.times(MULTIPLY_FACTOR_ONE)).prune()
    val stepTwo = stepOne.mix(stepOne.floorDiv(DIVISION_FACTOR)).prune()
    val stepThree = stepTwo.mix(stepTwo.times(MULTIPLY_FACTOR_TWO)).prune()
    return computeSecret(stepThree, times.dec())
  }

  /*
  - To mix a value into the secret number, calculate the bitwise XOR of the given value and the secret number.
  - Then, the secret number becomes the result of that operation.
  */
  private fun Long.mix(other: Long): Long =
    this.xor(other)

  /*
  - To prune the secret number, calculate the value of the secret number modulo 16777216.
  - Then, the secret number becomes the result of that operation.
   */
  private fun Long.prune(): Long =
    this.mod(PRUNE_FACTOR)
}

suspend fun main() {
  Day22.run()
}
