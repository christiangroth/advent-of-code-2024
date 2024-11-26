package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.GateOperation.Companion.toGateOperation

private enum class GateOperation {
  AND, OR, XOR;

  companion object {
    fun String.toGateOperation() =
      entries.first { it.toString() == this }
  }
}

private data class Gate(val sourceA: String, val sourceB: String, val operation: GateOperation, val target: String) {
  fun canUpdateValues(values: Map<String, Boolean>): Boolean =
    values.containsKey(sourceA) && values.containsKey(sourceB)

  fun updateValues(values: Map<String, Boolean>): Map<String, Boolean> =
    values.plus(
      target to when (operation) {
        GateOperation.AND -> values.getValue(sourceA).and(values.getValue(sourceB))
        GateOperation.OR -> values.getValue(sourceA).or(values.getValue(sourceB))
        GateOperation.XOR -> values.getValue(sourceA).xor(values.getValue(sourceB))
      }
    )
}

data object Day24 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The first section specifies these values.
    - For example, x00: 1 means that the wire named x00 starts with the value 1 (as if a gate is already outputting that value onto that wire).
    - The second section lists all of the gates and the wires connected to them.
    - x00 AND y00 -> z00 describes an instance of an AND gate which has wires x00 and y00 connected to its inputs and which will write its output to wire z00.
    */
    val (values, gates) = input.let {
      val blankLineIndex = it.indexOfFirst { it.isBlank() }
      val values = it.subList(0, blankLineIndex).associate { line ->
        line.split(":").let {
          it.first().trim() to (it.last().trim().toInt() == 1)
        }
      }

      val gates = it.subList(blankLineIndex.inc(), it.size).map { line ->
        line.split(" ").let {
          Gate(
            sourceA = it[0],
            sourceB = it[2],
            operation = it[1].toGateOperation(),
            target = it[4],
          )
        }
      }

      values to gates
    }

    /*
    - Ultimately, the system is trying to produce a number by combining the bits on all wires starting with z.
    - z00 is the least significant bit, then z01, then z02, and so on.
    */
    val solution = solve(gates, values)

    /*
    - After inspecting the monitoring device more closely, you determine that the system you're simulating is trying to add two binary numbers.
    - Specifically, it is treating the bits on wires starting with x as one binary number,
    - treating the bits on wires starting with y as a second binary number,
    - and then attempting to add those two numbers together.
    - The output of this operation is produced as a binary number on the wires starting with z.
    - (In all three cases, wire 00 is the least significant bit, then 01, then 02, and so on.)

    - Your system of gates and wires has four pairs of gates which need their output wires swapped - eight wires in total.
    - Determine which four pairs of gates need their outputs swapped so that your system correctly performs addition;
    - what do you get if you sort the names of the eight wires involved in a swap and then join those names with commas?
    */

    return PuzzleSolution(solution, null)
  }

  private fun solve(gates: List<Gate>, values: Map<String, Boolean>): Long {
    var currentValues = values
    var remainingGates = gates
    while (remainingGates.isNotEmpty()) {
      val gatesToProcess = gates.filter { it.canUpdateValues(currentValues) }
      currentValues = gatesToProcess.fold(currentValues) { result, gate ->
        gate.updateValues(result)
      }
      remainingGates = remainingGates.minus(gatesToProcess)
    }

    return currentValues.solve()
  }

  private fun Map<String, Boolean>.solve(): Long = entries
    .filter { it.key.startsWith("z") }
    .sortedByDescending { it.key }
    .joinToString(separator = "") { if (it.value) "1" else "0" }
    .toLong(radix = 2)
}

suspend fun main() {
  Day24.run()
}
