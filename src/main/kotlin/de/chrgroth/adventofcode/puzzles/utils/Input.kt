package de.chrgroth.adventofcode.puzzles.utils

internal fun List<String>.skipBlank(): List<String> =
  filter { it.isNotBlank() }
