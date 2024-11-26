package de.chrgroth.adventofcode.puzzles

private data class PrintOrder(val pages: List<Int>) {
  val middlePage: Int
    get() = pages[pages.size.floorDiv(2)]

  fun hasValidOrder(orderingRules: Map<Int, List<Int>>): Boolean =
    computeFix(orderingRules) == null

  fun fixPageOrder(orderingRules: Map<Int, List<Int>>): PrintOrder {
    computeFix(orderingRules) ?: return this

    val validOrders: List<PrintOrder> = pages.fold(emptyList()) { result, pageToAdd ->
      if (result.isEmpty()) {
        result.plus(PrintOrder(listOf(pageToAdd)))
      } else {
        result.flatMap { printOrder: PrintOrder ->
          IntRange(0, printOrder.pages.size - 1).map { index ->
            PrintOrder(
              printOrder.pages.subList(0, index) + listOf(pageToAdd) + printOrder.pages.subList(
                index,
                printOrder.pages.size
              )
            )
          }.plus(PrintOrder(printOrder.pages.plus(pageToAdd)))
            .filter { it.hasValidOrder(orderingRules) }
        }
      }
    }

    return validOrders.first()
  }

  private fun computeFix(orderingRules: Map<Int, List<Int>>): PrintOrderFix? {
    pages.forEachIndexed { index, page ->
      val pagesBeforeCurrent = pages.subList(0, index)
      val pagesThatMustBeAfterCurrent = orderingRules.getOrDefault(page, emptyList())
      val invalidIndex = pagesBeforeCurrent.indexOfFirst { pageBefore -> pageBefore in pagesThatMustBeAfterCurrent }
      if (invalidIndex != -1) {
        return PrintOrderFix(invalidIndex, index)
      }
    }

    return null
  }

  data class PrintOrderFix(val sourceIndex: Int, val newTargetIndex: Int) {
    override fun toString(): String {
      return "$sourceIndex -> $newTargetIndex"
    }
  }
}

data object Day05 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    val blankIndex = input.indexOfFirst { it.isBlank() }
    val rulesInput = input.take(blankIndex)
    val ordersInput = input.drop(blankIndex.inc())

    /*
    The first section specifies the page ordering rules, one per line.
    The first rule, 47|53, means that if an update includes both page number 47 and page number 53,
    then page number 47 must be printed at some point before page number 53.
    (47 doesn't necessarily need to be immediately before 53; other pages are allowed to be between them.)
    */
    val orderingRules = rulesInput
      .map { line ->
        line.split('|')
          .map { it.toInt() }
          .let {
            it[0] to it[1]
          }
      }
      .fold(mutableMapOf<Int, MutableList<Int>>(), { result, rule ->
        result.apply {
          getOrPut(rule.first) { mutableListOf() }.add(rule.second)
        }
      }).map {
        it.key to it.value.toList()
      }.toMap()

    /*
    The second section specifies the page numbers of each update.
    Because most safety manuals are different, the pages needed in the updates are different too.
    The first update, 75,47,61,53,29, means that the update consists of page numbers 75, 47, 61, 53, and 29.
    */
    val printOrders = ordersInput.map { line ->
      PrintOrder(line.split(',').map { it.toInt() })
    }

    /*
    - To get the printers going as soon as possible, start by identifying which updates are already in the right order.
    - For some reason, the Elves also need to know the middle page number of each update being printed.
    - What do you get if you add up the middle page number from those correctly-ordered updates?
    */
    val sumOfValidPrintOrdersMiddlePages = printOrders
      .filter { it.hasValidOrder(orderingRules) }
      .sumOf { it.middlePage }

    /*
    - Find the updates which are not in the correct order.
    - What do you get if you add up the middle page numbers after correctly ordering just those updates?
    */
    val sumOfFixedPrintOrdersMiddlePages = printOrders
      .filter { !it.hasValidOrder(orderingRules) }
      .map { it.fixPageOrder(orderingRules) }
      .sumOf { it.middlePage }

    return PuzzleSolution(sumOfValidPrintOrdersMiddlePages, sumOfFixedPrintOrdersMiddlePages)
  }
}

suspend fun main() {
  Day05.run()
}
