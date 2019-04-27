import java.io.InputStream

class Instruction(val value: Int) {
    val nnn = value.and(0x0FFF)
    val n = value.and(0x000F)
    val x = value.shr(8).and(0x000F)
    val y = value.shr(4).and(0x000F)
    val kk = value.and(0x00FF)

    fun nibbles(index: Int, length: Int): Int {
        return 0
    }
}

class InstructionHandler(
    val matches: (Instruction) -> Boolean,
    val execute: (Machine) -> Unit
)

val handlers = listOf(
    // clear screen
    InstructionHandler({ it.value == 0x00E0 }) {

    },
    // return from subroutine
    InstructionHandler({ it.value == 0x00EE }) {

    }
)

class Machine(val rom: InputStream) {
    val memory = IntArray(4096)
    val V = IntArray(16)
    val stack = IntArray(16)
    var I = 0
    var delayTimer = 0
    var soundTimer = 0
    var programCounter = 0
    var stackPointer = 0

    fun boot() {
        val bytes = rom.readAllBytes()

        for (i in bytes.indices) {
            memory[i + 512] = bytes[i].toInt().and(0xFF)
        }

        programCounter = 512

        cycle()
    }

    private fun fetch(): Instruction {
        val value = memory[programCounter]
            .shl(8)
            .or(memory[programCounter + 1])

        return Instruction(value)
    }

    private fun cycle() {
        val instr = fetch()
        val handler = handlers.firstOrNull { it.matches(instr) }

        if (handler == null) {
            val hex = Integer.toHexString(instr.value)
            throw Exception("No matching handler for instruction $hex")
        }

        handler.execute(this)
    }
}
