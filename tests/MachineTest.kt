import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.util.Arrays

class MachineTest {
    val rom: ByteArray = ByteArray(128)
    var machine: Machine = Machine(ByteArrayInputStream(rom))

    @BeforeEach
    fun setUp() {
        Arrays.fill(rom, 0x00)
        machine = Machine(ByteArrayInputStream(rom))
        machine.onCycle = { m, _ ->
            m.run = false
        }
    }

    private fun setInstructions(instructions: List<Int>): Unit {
        for (index in instructions.indices) {
            val instr = instructions[index]
            val k = index * 2
            rom[k] = instr.shr(8).toByte()
            rom[k + 1] = instr.and(0x00FF).toByte()
        }
    }

    @Test
    fun shouldHandleClearScreenInstruction() {
        setInstructions(listOf(0x00E0))

        machine.boot()

        assertEquals(0x00E0, machine.previousInstruction?.value)
        assertEquals(514, machine.programCounter)
    }

    @Test
    fun shouldThrowOnUnknownInstruction() {
        setInstructions(listOf(0x70FF))

        assertThrows(Exception::class.java) {
            machine.boot()
        }
    }

    @Test
    fun shouldReturnFromSubroutine() {
        setInstructions(listOf(0x00EE))

        machine.stackPointer += 1
        machine.stack[0] = 1000

        machine.boot()

        assertEquals(0x00EE, machine.previousInstruction?.value)
        assertEquals(1000, machine.programCounter)
    }
}
