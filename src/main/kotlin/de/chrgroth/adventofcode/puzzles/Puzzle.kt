package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.skipBlank
import java.io.File
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

enum class Stage(val filenamePart: String) {
  TEST("test"), PROD("prod")
}

private enum class FileMode(val filenamePart: String) {
  EXPECTATION("expect"), INPUT("input")
}

data class PuzzleSolution(
  val partOne: String,
  val partTwo: String?,
) {
  constructor(partOne: Any, partTwo: Any?) :
      this(partOne.toString(), partTwo?.toString())

  fun print(): String =
    "$partOne & $partTwo"
}

private data class PuzzleInput(
  val stage: Stage,
  val counter: Int?,
  val data: List<String>,
  val assertion: PuzzleSolution,
)

private data class PuzzleExecution(
  val name: String,
  val stage: Stage,
  val counter: Int?,
  val solution: TimedValue<PuzzleSolution>,
  val assertion: PuzzleSolution,
)

internal sealed interface Puzzle {
  val name: String
    get() = javaClass.simpleName

  suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution

  suspend fun run(): Boolean {
    var allValid = true
    execute().forEach { execution ->
      val solutionConfirmed = execution.assertion == execution.solution.value
      val (variableText, postfix) = if (solutionConfirmed) {
        "VALID  " to ""
      } else {
        allValid = false
        "BROKEN " to " (Expected: ${execution.assertion.print()})"
      }

      println(
        "${execution.name} $variableText | ${execution.stage} ${execution.counter?.formatCounter() ?: "  "} | " +
            "${execution.solution.value.print()} in ${execution.solution.duration}$postfix"
      )
    }

    return allValid
  }

  private suspend fun execute(): List<PuzzleExecution> =
    resolveInputPaths().map { input ->
      PuzzleExecution(
        name = name,
        stage = input.stage,
        counter = input.counter,
        solution = measureTimedValue { solve(input.stage, input.data) },
        assertion = input.assertion,
      )
    }

  private fun resolveInputPaths(): List<PuzzleInput> {
    val prodInputFilename = name.deriveFilename(Stage.PROD, FileMode.INPUT, null)
    val prodInput = prodInputFilename.readInputFile()
    val prodExpectation = name.deriveFilename(Stage.PROD, FileMode.EXPECTATION, null).readExpectationFile()
    val prodPuzzleInput = (prodInput to prodExpectation).toPuzzleInputOrNull(Stage.PROD, null)

    val testInputFilename = name.deriveFilename(Stage.TEST, FileMode.INPUT, null)
    val testInput = testInputFilename.readInputFile()
    val testExpectation = name.deriveFilename(Stage.TEST, FileMode.EXPECTATION, null).readExpectationFile()
    val simpleTestPuzzleInput = (testInput to testExpectation).toPuzzleInputOrNull(Stage.TEST, null)

    val testInputs = if (simpleTestPuzzleInput == null) {
      var counter = 1
      var countedTestPuzzleInput: PuzzleInput?
      var countedTestInputs: List<PuzzleInput> = emptyList()

      do {
        val countedTestInputFilename = name.deriveFilename(Stage.TEST, FileMode.INPUT, counter)
        val countedTestInput = countedTestInputFilename.readInputFile()
        val countedTestExpectation =
          name.deriveFilename(Stage.TEST, FileMode.EXPECTATION, counter).readExpectationFile()
        countedTestPuzzleInput =
          (countedTestInput to countedTestExpectation).toPuzzleInputOrNull(Stage.TEST, counter)

        if (countedTestPuzzleInput != null) {
          countedTestInputs = countedTestInputs.plus(countedTestPuzzleInput)
        }
        counter += 1
      } while (countedTestPuzzleInput != null)

      countedTestInputs
    } else {
      listOf(simpleTestPuzzleInput)
    }

    return testInputs.let {
      if (prodPuzzleInput != null) {
        it.plus(prodPuzzleInput)
      } else {
        it
      }
    }
  }

  private fun String.deriveFilename(stage: Stage, mode: FileMode, counter: Int?): String =
    this.removePrefix("Day")
      .plus(".")
      .plus(stage.filenamePart)
      .plus(".")
      .plus(mode.filenamePart)
      .let {
        if (counter != null) {
          it.plus(".").plus(counter.formatCounter())
        } else {
          it
        }
      }
      .plus(".txt")

  private fun String.readInputFile(): List<String>? =
    File("$INPUTS_BASE_PATH/$this").read()

  private fun String.readExpectationFile(): PuzzleSolution? =
    File("$INPUTS_BASE_PATH/$this").read()?.skipBlank()?.let {
      if (it.isEmpty()) {
        println("Invalid assertion file found: $this. No content, but either one or two lines of data expected!")
        null
      } else if (it.size <= 2) {
        it.toPuzzleSolution()
      } else {
        println("Invalid assertion file found: $this. Only one or two lines of data expected!")
        null
      }
    }

  private fun File.read(): List<String>? {
    if (!exists() || !canRead()) {
      return null
    }

    return readLines()
  }

  private fun Pair<List<String>?, PuzzleSolution?>.toPuzzleInputOrNull(stage: Stage, counter: Int?): PuzzleInput? {
    val input = first
    val assertion = second

    return when {
      input != null && assertion != null -> PuzzleInput(
        stage = stage,
        counter = counter,
        data = input,
        assertion = assertion,
      )

      input != null && assertion == null -> null.also {
        println("Ignoring input without assertion: $stage ${counter?.formatCounter()}")
      }

      input == null && assertion != null -> null.also {
        println("Ignoring assertion without input: $stage ${counter?.formatCounter()}")
      }

      else -> null
    }
  }

  private fun List<String>.toPuzzleSolution(): PuzzleSolution {
    check(size in 1..2)
    return if (size == 1) {
      PuzzleSolution(first(), null)
    } else {
      PuzzleSolution(first(), last())
    }
  }

  private fun Int.formatCounter(): String =
    toString().padStart(2, '0')

  companion object {
    private const val INPUTS_BASE_PATH: String = "src/main/resources/inputs"
  }
}
