package de.chrgroth.adventofcode

import de.chrgroth.adventofcode.puzzles.Puzzle
import kotlinx.coroutines.runBlocking
import kotlin.reflect.full.createInstance
import kotlin.system.exitProcess
import kotlin.time.measureTime

fun main() {
  var allValid = true
  runBlocking {
    println("Computing Advent of Code solutions...")
    measureTime {
      Puzzle::class.sealedSubclasses.sortedBy { it.simpleName }.forEach {
        val puzzleValid = (it.objectInstance ?: it.createInstance()).run()
        allValid = allValid && puzzleValid
      }
    }.also {
      println("Done in $it")
    }
  }

  if (!allValid) {
    System.err.println("Some puzzle solutions failed to match expected solution!")
    exitProcess(1)
  }
}
