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
        setInstructions(listOf(0xF065))

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

    @Test
    fun shouldDoNothingOnSysAddress() {
        setInstructions(listOf(0x0123))

        machine.boot()

        assertEquals(514, machine.programCounter)
        assertEquals(0, machine.stackPointer)
    }

    @Test
    fun shouldJumpToAddress() {
        setInstructions(listOf(0x13FF))

        machine.boot()

        assertEquals(0x03FF, machine.programCounter)
    }

    @Test
    fun shouldCallAddress() {
        setInstructions(listOf(0x23FF))

        machine.boot()

        assertEquals(0x0202, machine.stack[0])
        assertEquals(1, machine.stackPointer)
        assertEquals(0x03FF, machine.programCounter)
    }

    @Test
    fun shouldSkipInstructionIfVxEqualsKk() {
        setInstructions(listOf(0x31F0))
        machine.V[1] = 240
        machine.boot()

        assertEquals(516, machine.programCounter)
    }

    @Test
    fun shouldNotSkipInstructionIfVxNotEqualsKk() {
        setInstructions(listOf(0x31F0))
        machine.V[1] = 239
        machine.boot()

        assertEquals(514, machine.programCounter)
    }

    @Test
    fun shouldSkipInstructionIfVxNotEqualsKk() {
        setInstructions(listOf(0x41F0))

        machine.V[1] = 239
        machine.boot()

        assertEquals(516, machine.programCounter)
    }

    @Test
    fun shouldNotSkipInstructionIfVxEqualsKk() {
        setInstructions(listOf(0x41F0))

        machine.V[1] = 240
        machine.boot()

        assertEquals(514, machine.programCounter)
    }

    @Test
    fun shouldSkipInstructionIfVxEqualsVy() {
        setInstructions(listOf(0x5250))

        machine.V[2] = 50
        machine.V[5] = 50

        machine.boot()

        assertEquals(516, machine.programCounter)
    }

    @Test
    fun shouldNotSkipInstructionIfVxNotEqualsVy() {
        setInstructions(listOf(0x5250))

        machine.V[2] = 50
        machine.V[5] = 49

        machine.boot()

        assertEquals(514, machine.programCounter)
    }

    @Test
    fun shouldSetVxToKk() {
        setInstructions(listOf(0x61F1))

        machine.boot()

        assertEquals(241, machine.V[1])
    }

    @Test
    fun shouldAddKkToVx() {
        setInstructions(listOf(0x71F1))

        machine.V[1] = 50

        machine.boot()

        assertEquals(291, machine.V[1])
    }

    @Test
    fun shouldSetVxToVy() {
        setInstructions(listOf(0x8140))

        machine.V[4] = 123

        machine.boot()

        assertEquals(123, machine.V[1])
    }

    @Test
    fun shouldSetVxToVxBitwiseOrVy() {
        setInstructions(listOf(0x8141))

        machine.V[1] = 0xF0
        machine.V[4] = 0x0F

        machine.boot()

        assertEquals(0xFF, machine.V[1])
    }

    @Test
    fun shouldSetVxToVxBitwiseAndVy() {
        setInstructions(listOf(0x8142))

        machine.V[1] = 0b01011111
        machine.V[4] = 0b00111111

        machine.boot()

        assertEquals(0b00011111, machine.V[1])
    }

    @Test
    fun shouldSetVxToVxBitwiseXorVy() {
        setInstructions(listOf(0x8143))

        machine.V[1] = 0xEF
        machine.V[4] = 0xFF

        machine.boot()

        assertEquals(0x10, machine.V[1])
    }

    @Test
    fun shouldAddVxToVyAndNotSetCarry() {
        setInstructions(listOf(0x8144))

        machine.V[1] = 1
        machine.V[4] = 3

        machine.boot()

        assertEquals(4, machine.V[1])
        assertEquals(0, machine.V[15])
    }

    @Test
    fun shouldAddVxToVyAndSetCarry() {
        setInstructions(listOf(0x8144))

        machine.V[1] = 254
        machine.V[4] = 512

        machine.boot()

        assertEquals(254, machine.V[1])
        assertEquals(1, machine.V[15])
    }

    @Test
    fun shouldSubtractVyFromVxAndSetCarry() {
        setInstructions(listOf(0x8145))

        machine.V[1] = 5
        machine.V[4] = 3

        machine.boot()

        assertEquals(2, machine.V[1])
        assertEquals(1, machine.V[15])
    }

    @Test
    fun shouldSubtractVyFromVxAndNotSetCarry() {
        setInstructions(listOf(0x8145))

        machine.V[1] = 4
        machine.V[4] = 7

        machine.boot()

        assertEquals(253, machine.V[1])
        assertEquals(0, machine.V[15])
    }

    @Test
    fun shouldShiftVxRightAndSetCarry() {
        setInstructions(listOf(0x81F6))

        machine.V[1] = 0b11110001

        machine.boot()

        assertEquals(0b01111000, machine.V[1])
        assertEquals(1, machine.V[15])
    }

    @Test
    fun shouldShiftVxRightAndNotSetCarry() {
        setInstructions(listOf(0x81F6))

        machine.V[1] = 0b11110000

        machine.boot()

        assertEquals(0b01111000, machine.V[1])
        assertEquals(0, machine.V[15])
    }

    @Test
    fun shouldSubtractVxFromVyAndSetCarry() {
        setInstructions(listOf(0x8147))

        machine.V[1] = 6
        machine.V[4] = 9

        machine.boot()

        assertEquals(3, machine.V[1])
        assertEquals(1, machine.V[15])
    }

    @Test
    fun shouldSubtractVxFromVyAndNotSetCarry() {
        setInstructions(listOf(0x8147))

        machine.V[1] = 10
        machine.V[4] = 9

        machine.boot()

        assertEquals(255, machine.V[1])
        assertEquals(0, machine.V[15])
    }

    @Test
    fun shouldShiftVxLeftAndSetCarry() {
        setInstructions(listOf(0x810E))

        machine.V[1] = 0x81

        machine.boot()

        assertEquals(2, machine.V[1])
        assertEquals(1, machine.V[15])
    }

    @Test
    fun shouldShiftVxLeftAndNotCarry() {
        setInstructions(listOf(0x810E))

        machine.V[1] = 0x01

        machine.boot()

        assertEquals(2, machine.V[1])
        assertEquals(0, machine.V[15])
    }

    @Test
    fun shouldSkipInstructionWhenVxNotEqualsVy() {
        setInstructions(listOf(0x9120))

        machine.V[1] = 2
        machine.V[2] = 3

        machine.boot()

        assertEquals(516, machine.programCounter)
    }

    @Test
    fun shouldSetIToNnn() {
        setInstructions(listOf(0xA123))

        machine.boot()

        assertEquals(0x0123, machine.I)
    }

    @Test
    fun shouldJumpToNnnPlusV0() {
        setInstructions(listOf(0xB0FF))

        machine.V[0] = 10

        machine.boot()

        assertEquals(265, machine.programCounter)
    }

    @Test
    fun shouldRandomizeNumberAndBitwiseAndWithKk() {
        setInstructions(listOf(0xC10F))

        machine.boot()

        assertTrue(machine.V[1] in (0..15))
    }
}
