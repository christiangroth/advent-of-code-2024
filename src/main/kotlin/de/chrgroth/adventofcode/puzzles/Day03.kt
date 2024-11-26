package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.Operation.Multiply.Companion.GROUP_INDEX_OPERAND_A
import de.chrgroth.adventofcode.puzzles.Operation.Multiply.Companion.GROUP_INDEX_OPERAND_B
import de.chrgroth.adventofcode.puzzles.OperationCode.Companion.GROUP_INDEX_CODE
import de.chrgroth.adventofcode.puzzles.OperationCode.Companion.toOperationCodeOrNull
import de.chrgroth.adventofcode.puzzles.utils.skipBlank

private enum class OperationCode(val textValue: String) {
  DO("do"), DONT("don't"), MULTIPLY("mul");

  companion object {
    const val GROUP_INDEX_CODE = 1
    fun String.toOperationCodeOrNull(): OperationCode? =
      entries.firstOrNull { it.textValue == this }
  }
}

private sealed interface Operation {

  // It does that with instructions like mul(X,Y), where X and Y are each 1-3 digit numbers.
  // For instance, mul(44,46) multiplies 44 by 46 to get a result of 2024. Similarly, mul(123,4) would multiply 123 by 4.
  data class Multiply(val operandA: Int, val operandB: Int) : Operation {
    fun compute(): Int = operandA * operandB

    companion object {
      val parser = Regex("""(${OperationCode.MULTIPLY.textValue})\((\d{1,3}),(\d{1,3})\)""")

      const val GROUP_INDEX_OPERAND_A = 2
      const val GROUP_INDEX_OPERAND_B = 3
    }
  }

  // - The do() instruction enables future mul instructions.
  object Do : Operation {
    val parser = Regex("""(${OperationCode.DO.textValue})\(\)""")
  }

  // -The don't() instruction disables future mul instructions.
  object Dont : Operation {
    val parser = Regex("""(${OperationCode.DONT.textValue})\(\)""")
  }
}

data object Day03 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {
    val operations = input.skipBlank()
      .flatMap {
        val allMultiplyMatches = Operation.Multiply.parser.findAll(it)
        val allDoMatches = Operation.Do.parser.findAll(it)
        val allDontMatches = Operation.Dont.parser.findAll(it)
        val sortedMatches = allMultiplyMatches.plus(allDoMatches).plus(allDontMatches)
          .sortedBy { match ->
            match.range.first
          }

        sortedMatches.mapNotNull { match ->
          val code = match.groupValues[GROUP_INDEX_CODE].toOperationCodeOrNull()
          when (code) {
            null -> null
            OperationCode.MULTIPLY -> {
              val operandA = match.groupValues[GROUP_INDEX_OPERAND_A].toInt()
              val operandB = match.groupValues[GROUP_INDEX_OPERAND_B].toInt()
              Operation.Multiply(operandA, operandB)
            }

            OperationCode.DO -> Operation.Do
            OperationCode.DONT -> Operation.Dont
          }
        }
      }

    val allOperationsSum = operations.sumMultiplications()

    // Only the most recent do() or don't() instruction applies. At the beginning of the program, mul instructions are enabled.
    var collectOperation = true
    val filteredOperationsSum = operations.filter {
      when (it) {
        is Operation.Multiply -> collectOperation
        is Operation.Do -> {
          collectOperation = true
          false
        }

        is Operation.Dont -> {
          collectOperation = false
          false
        }
      }
    }.sumMultiplications()

    return PuzzleSolution(allOperationsSum, filteredOperationsSum)
  }
}

private fun List<Operation>.sumMultiplications(): Int =
  this.filterIsInstance<Operation.Multiply>().sumOf { it.compute() }

suspend fun main() {
  Day03.run()
}
