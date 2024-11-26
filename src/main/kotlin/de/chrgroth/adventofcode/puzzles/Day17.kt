package de.chrgroth.adventofcode.puzzles

import de.chrgroth.adventofcode.puzzles.Opcode.Companion.toOpcode
import de.chrgroth.adventofcode.puzzles.utils.skipBlank
import kotlin.math.pow

/*
 - This seems to be a 3-bit computer: its program is a list of 3-bit numbers (0 through 7), like 0,1,2,3.
 - The computer also has three registers named A, B, and C, but these registers aren't limited to 3 bits and can instead hold any integer.
 */
private data class Program(
  val registerA: Int,
  val registerB: Int,
  val registerC: Int,
  val instructions: List<Instruction>,
  val output: List<Byte> = emptyList(),
  val instructionPointer: Int = 0,
) {
  fun isEnded(): Boolean =
    instructionPointer == instructions.size

  fun executeNextInstruction(): Program {
    val instruction = instructions.getOrNull(instructionPointer)
      ?: return this

    return instruction.opcode.execute(this, instruction.operand).let {
      it.copy(
        instructionPointer = it.instructionPointer.inc()
      )
    }
  }

  fun getOutput(): String =
    output.joinToString(separator = ",")

  companion object {
    val PARSER_REGISTER_A = Regex("""Register A: (\d+)""")
    val PARSER_REGISTER_B = Regex("""Register B: (\d+)""")
    val PARSER_REGISTER_C = Regex("""Register C: (\d+)""")
    val PARSER_REGISTER_INSTRUCTION_SEQUENCE = Regex("""Program: (.+)""")
  }
}

private data class Instruction(val opcode: Opcode, val operand: Byte)

/*
- The computer knows eight instructions, each identified by a 3-bit number (called the instruction's opcode).
- Each instruction also reads the 3-bit number after it as an input; this is called its operand.
 */
private enum class Opcode(val value: Byte, val operandType: OperandType) {

  /*
  - The adv instruction (opcode 0) performs division.
  - The numerator is the value in the A register.
  - The denominator is found by raising 2 to the power of the instruction's combo operand.
  - (So, an operand of 2 would divide A by 4 (2^2); an operand of 5 would divide A by 2^B.)
  - The result of the division operation is truncated to an integer and then written to the A register.
   */
  DIV_A(0.toByte(), OperandType.COMBO) {
    override fun executeInternal(program: Program, operand: Byte): Program {
      val numerator = program.registerA
      val denominator = 2.toDouble().pow(operand.toInt())
      return program.copy(
        registerA = numerator.div(denominator).toInt()
      )
    }
  },

  /*
  - The bxl instruction (opcode 1) calculates the bitwise XOR of register B and the instruction's literal operand, then stores the result in register B.
   */
  XOR_B_OPERAND(1.toByte(), OperandType.LITERAL) {
    override fun executeInternal(program: Program, operand: Byte): Program =
      program.copy(
        registerB = program.registerB.xor(operand.toInt())
      )
  },

  /*
  - The bst instruction (opcode 2) calculates the value of its combo operand modulo 8 (thereby keeping only its lowest 3 bits), then writes that value to the B register.
   */
  MOD_8_B(2.toByte(), OperandType.COMBO) {
    override fun executeInternal(program: Program, operand: Byte): Program =
      program.copy(
        registerB = operand.mod(8.toByte()).toInt()
      )
  },

  /*
  - The jnz instruction (opcode 3) does nothing if the A register is 0.
  - However, if the A register is not zero, it jumps by setting the instruction pointer to the value of its literal operand;
  - if this instruction jumps, the instruction pointer is not increased by 2 after this instruction.
   */
  JUMP_NOT_ZERO_A(3.toByte(), OperandType.LITERAL) {
    override fun executeInternal(program: Program, operand: Byte): Program =
      if (program.registerA == 0) {
        program
      } else {
        program.copy(
          instructionPointer = operand.toInt().floorDiv(2) - 1
        )
      }
  },

  /*
  - The bxc instruction (opcode 4) calculates the bitwise XOR of register B and register C, then stores the result in register B.
  - (For legacy reasons, this instruction reads an operand but ignores it.)
   */
  XOR_B_C(4.toByte(), OperandType.IGNORE) {
    override fun executeInternal(program: Program, operand: Byte): Program =
      program.copy(
        registerB = program.registerB.xor(program.registerC)
      )
  },

  /*
  - The out instruction (opcode 5) calculates the value of its combo operand modulo 8, then outputs that value.
  - (If a program outputs multiple values, they are separated by commas.)
   */
  OUT(5.toByte(), OperandType.COMBO) {
    override fun executeInternal(program: Program, operand: Byte): Program =
      program.copy(
        output = program.output.plus(operand.mod(8.toByte()))
      )
  },

  /*
  - The bdv instruction (opcode 6) works exactly like the adv instruction except that the result is stored in the B register.
  - The numerator is still read from the A register.
   */
  DIV_B(6.toByte(), OperandType.COMBO) {
    override fun executeInternal(program: Program, operand: Byte): Program = program.copy(
      registerB = program.registerA.div(
        2.toDouble().pow(operand.toInt())
      ).toInt()
    )
  },

  /*
  - The cdv instruction )opcode 7= works exactlz like the adv instruction except that the result is stored in the C register.
  - The numerator is still read from the A register.
   */
  DIV_C(7.toByte(), OperandType.COMBO) {
    override fun executeInternal(program: Program, operand: Byte): Program {
      val numerator = program.registerA
      val denominator = 2.toDouble().pow(operand.toInt())
      return program.copy(
        registerC = numerator.div(denominator).toInt()
      )
    }
  };

  fun execute(program: Program, operand: Byte): Program =
    if (this.operandType == OperandType.COMBO) {
      executeInternal(program, resolveComboOperand(program, operand))
    } else {
      executeInternal(program, operand)
    }

  private fun resolveComboOperand(program: Program, operand: Byte): Byte =
    when (operand) {
      1.toByte(), 2.toByte(), 3.toByte() -> operand
      4.toByte() -> program.registerA.toByte()
      5.toByte() -> program.registerB.toByte()
      6.toByte() -> program.registerC.toByte()
      else -> throw IllegalArgumentException("Invalid operand value 7!")
    }

  abstract fun executeInternal(program: Program, operand: Byte): Program

  companion object {
    fun Byte.toOpcode(): Opcode? =
      entries.firstOrNull { it.value == this }
  }
}

/*
- There are two types of operands; each instruction specifies the type of its operand.
- The value of a literal operand is the operand itself.
- For example, the value of the literal operand 7 is the number 7.
- The value of a combo operand can be found as follows:
  - Combo operands 0 through 3 represent literal values 0 through 3.
  - Combo operand 4 represents the value of register A.
  - Combo operand 5 represents the value of register B.
  - Combo operand 6 represents the value of register C.
  - Combo operand 7 is reserved and will not appear in valid programs.
 */
private enum class OperandType { LITERAL, COMBO, IGNORE }

data object Day17 : Puzzle {
  override suspend fun solve(stage: Stage, input: List<String>): PuzzleSolution {

    val joinedInputFile = input.skipBlank().joinToString(separator = "     ")
    val registerA = Program.PARSER_REGISTER_A.find(joinedInputFile)?.groupValues?.get(1)?.toInt() ?: 0
    val registerB = Program.PARSER_REGISTER_B.find(joinedInputFile)?.groupValues?.get(1)?.toInt() ?: 0
    val registerC = Program.PARSER_REGISTER_C.find(joinedInputFile)?.groupValues?.get(1)?.toInt() ?: 0
    val instructions =
      (Program.PARSER_REGISTER_INSTRUCTION_SEQUENCE.find(joinedInputFile)?.groupValues?.get(1) ?: "").split(',')
        .chunked(2)
        .mapNotNull {
          val opcode = it.first().toByte().toOpcode()
          if (opcode != null) {
            Instruction(opcode, it.last().toByte())
          } else {
            null
          }
        }

    val program = Program(registerA, registerB, registerC, instructions)

    /*
    - Using the information provided by the debugger, initialize the registers to the given values, then run the program.
    - Once it halts, what do you get if you use commas to join the values it output into a single string?
    */
    val partOneOutput = run(program)

    /*
    - Digging deeper in the device's manual, you discover the problem: this program is supposed to output another copy of the program!
    - Unfortunately, the value in register A seems to have been corrupted.
    - You'll need to find a new value to which you can initialize register A so that the program's output instructions produce an exact copy of the program itself.
    - What is the lowest positive initial value for register A that causes the program to output a copy of itself?
    */

    return PuzzleSolution(partOneOutput, null)
  }

  private tailrec fun run(program: Program): String {
    if (program.isEnded()) {
      return program.getOutput()
    }

    return run(program.executeNextInstruction())
  }
}

suspend fun main() {
  Day17.run()
}
