package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private enum class EquationOperator {
  ADD {
    override fun calculate(a: Long, b: Long): Long = a + b
  },
  MULTIPLY {
    override fun calculate(a: Long, b: Long): Long = a * b
  },
  CONCAT {
    override fun calculate(a: Long, b: Long): Long = "$a$b".toLong()
  };

  abstract fun calculate(a: Long, b: Long): Long

  companion object {
    private data class CacheKey(val length: Int, val operators: Set<EquationOperator>)

    private val cache: MutableMap<CacheKey, List<List<EquationOperator>>> = mutableMapOf()

    fun computeAllOperatorCombinations(length: Int, operators: Set<EquationOperator>): List<List<EquationOperator>> =
      cache.computeIfAbsent(CacheKey(length, operators)) { cacheKey ->
        var combinations = listOf<List<EquationOperator>>(emptyList())
        repeat(cacheKey.length) {
          combinations = combinations.flatMap { operators ->
            cacheKey.operators.map {
              operators.plus(it)
            }
          }
        }

        combinations
      }
  }
}

private data class Equation(val result: Long, val operands: List<Long>) {
  fun canBeSolved(operatorsToBeUsed: Set<EquationOperator>): Boolean =
    EquationOperator.computeAllOperatorCombinations(operands.size - 1, operatorsToBeUsed).any { operators ->
      val sum = operands.foldIndexed(0.toLong()) { index, result, operand ->
        if (index == 0) {
          operand
        } else {
          operators[index - 1].calculate(result, operand)
        }
      }

      sum == result
    }
}

data object Day07 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - Each line represents a single equation.
    - The test value appears before the colon on each line
    - it is your job to determine whether the remaining numbers can be combined with operators to produce the test value.
    */
    val equations = input.skipBlank().map { line ->
      val (resultText, operandsText) = line.split(':')
      val operands = operandsText.trim().split(' ')
      Equation(resultText.toLong(), operands.map { it.toLong() })
    }

    /*
    - Operators are always evaluated left-to-right, not according to precedence rules.
    - Furthermore, numbers in the equations cannot be rearranged.
    - two different types of operators: add (+) and multiply (*).
    */

    val sumOfValidEquationsUsingAddAndMul = equations
      .filter { it.canBeSolved(setOf(EquationOperator.ADD, EquationOperator.MULTIPLY)) }
      .sumOf { it.result }

    val sumOfValidEquationsUsingAllOperators = equations
      .filter { it.canBeSolved(EquationOperator.entries.toSet()) }
      .sumOf { it.result }

    return PuzzleSolution(sumOfValidEquationsUsingAddAndMul, sumOfValidEquationsUsingAllOperators)
  }
}

suspend fun main() {
  Day07.run()
}
