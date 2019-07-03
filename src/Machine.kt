import java.io.InputStream
import java.lang.Exception

class Machine(val rom: InputStream) {
    var run = true
    val memory = IntArray(4096)
    val V = IntArray(16)
    val stack = IntArray(16)
    var I = 0
    var delayTimer = 0
    var soundTimer = 0
    var programCounter = 0
    var stackPointer = 0
    var previousInstruction: Instruction? = null
    var onCycle: ((Machine, Instruction) -> Unit)? = null

    fun boot() {
        val bytes = rom.readAllBytes()

        for (i in bytes.indices) {
            memory[i + 512] = bytes[i].toInt().and(0xFF)
        }

        programCounter = 512

        while (run) {
            cycle()
        }
    }

    private fun fetch(): Instruction {
        val value = memory[programCounter]
                .shl(8)
                .or(memory[programCounter + 1])

        return Instruction(value)
    }

    private fun cycle() {
        val instr = fetch()

        when (instr.value) {
            0x00E0 -> {
                // CLEAR SCREEN
                programCounter += 2
            }
            0x00EE -> {
                // RETURN
                stackPointer -= 1
                programCounter = stack[stackPointer]
            }
            0x0FFF.and(instr.value) -> {
                // SYS ADDRESS
                // do nothing.
            }
            else -> {
                throw Exception("Unknown instruction \"%s\"".format(Integer.toHexString(instr.value)))
            }
        }

        this.previousInstruction = instr

        onCycle?.let { it(this, instr) }
    }
}
