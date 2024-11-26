package de.chrgroth.adventofcode.puzzles

private data class DiskMap(val entries: List<DiskMapEntry>) {
  fun isFreeSpaceBetweenEntries(): Boolean =
    entries.indexOfFirst { it is DiskMapEntry.FreeSpace } < entries.indexOfLast { it is DiskMapEntry.File }

  /*
    - The amphipod would like to move file blocks one at a time from THE END of the disk to the leftmost free space block
    - until there are no gaps remaining between file blocks
    - The final step of this file-compacting process is to update the filesystem checksum.
    - To calculate the checksum, add up the result of multiplying each of these blocks' position with the file ID number it contains
    - The leftmost block is in position 0. If a block contains free space, skip it instead.
    */
  fun compactBlockwise(): DiskMap {
    val freeSpaceIndex = entries.indexOfFirst { it is DiskMapEntry.FreeSpace }
    val freeSpace = entries[freeSpaceIndex]
    val fileIndex = entries.indexOfLast { it is DiskMapEntry.File }
    val file = entries[fileIndex] as DiskMapEntry.File

    val freeSpaceFilled: UShort = if (file.blocks > freeSpace.blocks) freeSpace.blocks else file.blocks
    val freeSpaceRemaining: UShort = if (freeSpaceFilled == freeSpace.blocks) {
      0.toUShort()
    } else {
      (freeSpace.blocks - freeSpaceFilled).toUShort()
    }
    val fileBlocksRemaining: UShort = (file.blocks - freeSpaceFilled).toUShort()

    val unchangedHead: List<DiskMapEntry> = entries.subList(0, freeSpaceIndex)
    val filledFreeSpace: List<DiskMapEntry> = if (freeSpaceRemaining > 0.toUByte()) {
      listOf(file.copy(blocks = freeSpaceFilled), DiskMapEntry.FreeSpace(freeSpaceRemaining))
    } else {
      listOf(file.copy(blocks = freeSpaceFilled))
    }
    val unchangedMid: List<DiskMapEntry> = entries.subList(freeSpaceIndex + 1, fileIndex)
    val remainingOfMovedFileAndNewFreeSpace: List<DiskMapEntry> = if (file.blocks > freeSpace.blocks) {
      listOf(DiskMapEntry.File(file.id, fileBlocksRemaining), DiskMapEntry.FreeSpace(freeSpaceFilled))
    } else {
      listOf(DiskMapEntry.FreeSpace(freeSpaceFilled))
    }
    val unchangedTail = if ((fileIndex + 1) < entries.size) {
      entries.subList(fileIndex + 1, entries.size)
    } else {
      emptyList()
    }

    return copy(entries = unchangedHead + filledFreeSpace + unchangedMid + remainingOfMovedFileAndNewFreeSpace + unchangedTail)
  }

  /*
  - This time, attempt to move whole files to the leftmost span of free space blocks that could fit the file.
  - Attempt to move each file exactly once in order of decreasing file ID number starting with the file with the highest file ID number.
  - If there is no span of free space to the left of a file that is large enough to fit the file, the file does not move.
   */
  fun compactFilewise(): DiskMap {
    var optimizedDiskMap = this
    entries.filterIsInstance<DiskMapEntry.File>().reversed().forEach { file ->
      val fileIndex = optimizedDiskMap.entries.indexOfFirst { it is DiskMapEntry.File && it.id == file.id }
      val freeSpaceIndex = optimizedDiskMap.entries.indexOfFirst {
        it is DiskMapEntry.FreeSpace && it.blocks >= file.blocks
      }
      if (freeSpaceIndex != -1 && freeSpaceIndex < fileIndex) {
        val freeSpace = optimizedDiskMap.entries[freeSpaceIndex]
        val freeSpaceRemaining: UShort = (freeSpace.blocks - file.blocks).toUShort()

        val unchangedHead: List<DiskMapEntry> = optimizedDiskMap.entries.subList(0, freeSpaceIndex)
        val filledFreeSpace: List<DiskMapEntry> = if (freeSpaceRemaining > 0.toUShort()) {
          listOf(file, DiskMapEntry.FreeSpace(freeSpaceRemaining))
        } else {
          listOf(file)
        }
        val unchangedMid: List<DiskMapEntry> = optimizedDiskMap.entries.subList(freeSpaceIndex + 1, fileIndex)
        val newFreeSpace: List<DiskMapEntry> = listOf(DiskMapEntry.FreeSpace(file.blocks))
        val unchangedTail = if ((fileIndex + 1) < optimizedDiskMap.entries.size) {
          optimizedDiskMap.entries.subList(fileIndex + 1, optimizedDiskMap.entries.size)
        } else {
          emptyList()
        }

        optimizedDiskMap = optimizedDiskMap.copy(
          entries = unchangedHead + filledFreeSpace + unchangedMid + newFreeSpace + unchangedTail
        )
      }
    }

    return optimizedDiskMap
  }

  /*
  - To calculate the checksum, add up the result of multiplying each of these blocks' position with the file ID number it contains.
  - The leftmost block is in position 0.
  - If a block contains free space, skip it instead.
   */
  fun computeChecksum(): ULong =
    entries.flatMap { entry ->
      (0..<entry.blocks.toInt()).map {
        when (entry) {
          is DiskMapEntry.File -> entry.id
          is DiskMapEntry.FreeSpace -> 0.toUShort()
        }
      }
    }.mapIndexed { index, value ->
      index.toULong() * value.toULong()
    }.sum()
}

private sealed interface DiskMapEntry {
  val blocks: UShort

  data class File(val id: UShort, override val blocks: UShort) : DiskMapEntry
  data class FreeSpace(override val blocks: UShort) : DiskMapEntry
}

data object Day09 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    /*
    - The disk map uses a dense format to represent the layout of files and free space on the disk.
    - The digits alternate between indicating the length of a file and the length of free space.
    - Each file on disk also has an ID number based on the order of the files as they appear before they are rearranged, starting with ID 0.
    */
    var fileId: UShort = 0.toUShort()
    val diskMapEntries = input.first().mapIndexedNotNull { index: Int, value: Char ->
      val blocks: UShort = value.digitToInt().toUShort()
      if (index % 2 == 0) {
        DiskMapEntry.File(fileId++, blocks)
      } else {
        DiskMapEntry.FreeSpace(blocks)
      }
    }

    var diskMapToBeOptimized = DiskMap(diskMapEntries)
    while (diskMapToBeOptimized.isFreeSpaceBetweenEntries()) {
      diskMapToBeOptimized = diskMapToBeOptimized.compactBlockwise()
    }

    val diskMapOptimizedFilewise = DiskMap(diskMapEntries).compactFilewise()

    return PuzzleSolution(diskMapToBeOptimized.computeChecksum(), diskMapOptimizedFilewise.computeChecksum())
  }
}

suspend fun main() {
  Day09.run()
}
