package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.utils.skipBlank
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.measureTimedValue

private data class GenerationalStone(val stone: Stone, val generation: UByte) {
  companion object {
    private val EVOLUTION_BLOCK_SIZE: UByte = 20.toUByte()
    internal val EVOLUTION_SUM_CACHE: MutableMap<GenerationalStone, ULong> = mutableMapOf()
    private val EVOLUTION_LIST_CACHE: MutableMap<GenerationalStone, List<GenerationalStone>> = mutableMapOf()

    internal tailrec fun count(backlog: List<GenerationalStone>, sum: ULong): ULong {
      val head = backlog.firstOrNull()
        ?: return sum

      val tail = backlog.subList(1, backlog.size)
      val cachedValue = EVOLUTION_SUM_CACHE[head]
      return when {
        cachedValue != null -> {
          count(
            backlog = tail,
            sum = sum + cachedValue
          )
        }

        head.generation == 0.toUByte() -> {
          count(
            backlog = tail,
            sum = sum + 1.toULong()
          )
        }

        head.generation <= EVOLUTION_BLOCK_SIZE -> {
          val sumPart = head.stone.evolve(head.generation).size.toULong()
          EVOLUTION_SUM_CACHE[head] = sumPart

          count(
            backlog = tail,
            sum = sum + sumPart
          )
        }

        else -> {
          val cachedList = EVOLUTION_LIST_CACHE[head]
          val nextGenerationList = if (cachedList != null) {
            cachedList
          } else {
            val nextGenerationList = head.stone.evolve(EVOLUTION_BLOCK_SIZE).map { stone ->
              GenerationalStone(stone, (head.generation - EVOLUTION_BLOCK_SIZE).toUByte())
            }
            EVOLUTION_LIST_CACHE[head] = nextGenerationList
            nextGenerationList
          }

          count(
            backlog = nextGenerationList + tail,
            sum = sum
          )
        }
      }
    }
  }
}

@JvmInline
private value class Stone private constructor(val number: ULong) {

  /*
  - If the stone is engraved with the number 0, it is replaced by a stone engraved with the number 1.
  - If the stone is engraved with a number that has an even number of digits, it is replaced by two stones.
  - The left half of the digits are engraved on the new left stone, and the right half of the digits are engraved on the new right stone.
  - (The new numbers don't keep extra leading zeroes: 1000 would become stones 10 and 0.)
  - If none of the other rules apply, the stone is replaced by a new stone; the old stone's number multiplied by 2024 is engraved on the new stone.
  */

  fun evolve(times: UByte): List<Stone> {
    var stones = evolve()
    repeat(times.dec().toInt()) {
      stones = stones.flatMap { it.evolve() }
    }

    return stones
  }

  fun evolve(): List<Stone> =
    when {
      this == ZERO -> listOf(ONE)
      hasEvenNumberOfDigits() -> number.split().map { create(it) }
      else -> listOf(create(number * MULTIPLICATION_FACTOR))
    }

  private fun hasEvenNumberOfDigits(): Boolean =
    number.toString().length % 2 == 0

  private fun ULong.split(): List<ULong> =
    toString().let { numberString ->
      numberString.chunked(numberString.length / 2)
    }.map { it.toULong() }

  companion object {
    private const val MULTIPLICATION_FACTOR: ULong = 2024u

    val ZERO = Stone(0.toULong())
    val ONE = Stone(1.toULong())

    val CACHED_VALUES = listOf(
      ZERO,
      ONE,
      Stone(2u),
      Stone(3u),
      Stone(4u),
      Stone(6u),
      Stone(7u),
      Stone(8u),
      Stone(9u),
      Stone(20u),
      Stone(24u),
      Stone(32u),
      Stone(36u),
      Stone(48u),
      Stone(67u),
      Stone(72u),
      Stone(80u),
      Stone(84u),
      Stone(86u),
      Stone(91u),
      Stone(96u),
      Stone(2024u),
      Stone(4048u),
      Stone(8096u),
      Stone(12144u),
      Stone(16192u),
      Stone(18216u),
    )

    val stoneStats: MutableMap<ULong, ULong> = mutableMapOf()

    fun create(number: ULong): Stone {
      val cachedValue = CACHED_VALUES.firstOrNull { it.number == number }
      return if (cachedValue != null) cachedValue else {
        synchronized(stoneStats) {
          stoneStats[number] = stoneStats.getOrDefault(number, 0.toULong()).inc()
        }
        Stone(number)
      }
    }
  }
}

data object Day11 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The topographic map indicates the height at each position using a scale from 0 (lowest) to 9 (highest)
    */
    val stones: List<Stone> = input.skipBlank().flatMap { line ->
      line.split(' ').map { Stone.create(it.toULong()) }
    }

    val cacheValues = Stone.CACHED_VALUES.asSequence()
      .flatMap { it.evolve(35.toUByte()) }
      .toSet()
      .flatMap {
        listOf(
          GenerationalStone(it, 15.toUByte()),
          GenerationalStone(it, 35.toUByte()),
          GenerationalStone(it, 55.toUByte()),
        )
      }
      .map { stone ->
        stone to GenerationalStone.count(listOf(stone), 0u)
      }

    cacheValues.forEach {
      GenerationalStone.EVOLUTION_SUM_CACHE[it.first] = it.second
    }

    return PuzzleSolution(
      partOne = stones.countAfterGenerations(25u),
      partTwo = stones.countAfterGenerations(75u),
    )
  }

  @OptIn(DelicateCoroutinesApi::class)
  private suspend fun List<Stone>.countAfterGenerations(generations: UByte): ULong {
    val sumLock = object {}
    var sum = 0.toULong()

    map { stone ->
      GlobalScope.launch {
        measureTimedValue {
          GenerationalStone.count(listOf(GenerationalStone(stone, generations)), 0u)
        }.let {
          synchronized(sumLock) {
            sum += it.value
          }
        }
      }
    }.joinAll()

    return sum
  }
}

suspend fun main() {
  Day11.run()
}
