import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.util.Arrays

class MachineTest {
    val rom: ByteArray = ByteArray(128)
    val pressedKeys = mutableSetOf<Int>()
    val io = object: InputOutput {
        override fun getPressedKeys() = pressedKeys
    }
    var machine: Machine = Machine(ByteArrayInputStream(rom), io)

    @BeforeEach
    fun setUp() {
        Arrays.fill(rom, 0x00)
        machine = Machine(ByteArrayInputStream(rom), io)
        machine.onCycle = { m, _ ->
            m.run = false
        }
        pressedKeys.clear()
    }

    private fun setInstructions(vararg instructions: Int): Unit {
        for (index in instructions.indices) {
            val instr = instructions[index]
            val k = index * 2
            rom[k] = instr.shr(8).toByte()
            rom[k + 1] = instr.and(0x00FF).toByte()
        }
    }

    @Test
    fun shouldHandleClearScreenInstruction() {
        setInstructions(0x00E0)

        machine.boot()

        assertEquals(514, machine.programCounter)
    }

    @Test
    fun shouldReturnFromSubroutine() {
        setInstructions(0x00EE)

        machine.stackPointer += 1
        machine.stack[0] = 1000

        machine.boot()

        assertEquals(1000, machine.programCounter)
    }

    @Test
    fun shouldDoNothingOnSysAddress() {
        setInstructions(0x0123)

        machine.boot()

        assertEquals(514, machine.programCounter)
        assertEquals(0, machine.stackPointer)
    }

    @Test
    fun shouldJumpToAddress() {
        setInstructions(0x13FF)

        machine.boot()

        assertEquals(0x03FF, machine.programCounter)
    }

    @Test
    fun shouldCallAddress() {
        setInstructions(0x23FF)

        machine.boot()

        assertEquals(0x0202, machine.stack[0])
        assertEquals(1, machine.stackPointer)
        assertEquals(0x03FF, machine.programCounter)
    }

    @ParameterizedTest
    @MethodSource("skipsInstructionWhenVxEqualsKkProvider")
    fun shouldSkipInstructionWhenVxEqualsKk(instruction: Int, vValue: Int, expectedProgramCounter: Int) {
        setInstructions(instruction.or(1.shl(8)))

        machine.V[1] = vValue
        machine.boot()

        assertEquals(expectedProgramCounter, machine.programCounter)
    }

    @ParameterizedTest
    @MethodSource("skipsInstructionWhenRegistersEqualProvider")
    fun shouldSkipInstructionWhenRegistersEqual(instruction: Int, a: Int, b: Int, expectedProgramCounter: Int) {
        setInstructions(instruction.or(0x0250))

        machine.V[2] = a
        machine.V[5] = b

        machine.boot()

        assertEquals(expectedProgramCounter, machine.programCounter)
    }

    @Test
    fun shouldSetVxToKk() {
        setInstructions(0x61F1)

        machine.boot()

        assertEquals(241, machine.V[1])
    }

    @Test
    fun shouldAddKkToVx() {
        setInstructions(0x71F1)

        machine.V[1] = 50

        machine.boot()

        assertEquals(291, machine.V[1])
    }

    @ParameterizedTest
    @MethodSource("mathProvider")
    fun shouldDoMathToVx(instruction: Int, vxValue: Int, vyValue: Int, expectedCarry: Int, expectedResult: Int) {
        setInstructions(instruction)
        machine.V[2] = vxValue
        machine.V[5] = vyValue

        machine.boot()

        assertEquals(expectedResult, machine.V[2])
        assertEquals(expectedCarry, machine.V[15])
    }

    @Test
    fun shouldSetIToNnn() {
        setInstructions(0xA123)

        machine.boot()

        assertEquals(0x0123, machine.I)
    }

    @Test
    fun shouldJumpToNnnPlusV0() {
        setInstructions(0xB0FF)

        machine.V[0] = 10

        machine.boot()

        assertEquals(265, machine.programCounter)
    }

    @ParameterizedTest
    @MethodSource("randomizeSource")
    fun shouldRandomizeNumberAndBitwiseAndWithKk(x: Int, kk: Int) {
        val instr = (0xC000)
            .or(x.shl(8))
            .or(kk)

        setInstructions(instr)
        machine.boot()
        assertTrue(machine.V[x] in (0..kk))
    }

    @ParameterizedTest
    @MethodSource("skipsNextInstructionIfPressedProvider")
    fun shouldSkipNextInstructionIfKeyWithVxIsPressed(instruction: Int, vValue: Int, pressedKey: Int, expectedProgramCounter: Int) {
        setInstructions(
            instruction.or((0x02).shl(8))
        )

        machine.V[2] = vValue
        pressedKeys.add(pressedKey)

        machine.boot()

        assertEquals(expectedProgramCounter, machine.programCounter)
    }

    @Test
    fun shouldSetVxToDelayTimer() {
        setInstructions(0xF107)

        machine.delayTimer = 3

        machine.boot()

        assertEquals(3, machine.V[1])
    }

    @ParameterizedTest
    @MethodSource("waitForKeyPressProvider")
    fun shouldWaitForKeyPress(pressedKey: Int?, expectedProgramCounter: Int) {
        setInstructions(0xF10A)
        pressedKey?.let { pressedKeys.add(it) }
        machine.boot()

        assertEquals(expectedProgramCounter, machine.programCounter)
        assertEquals(pressedKey ?: 0, machine.V[1])
    }

    @Test
    fun shouldSetDelayTimerToVx() {
        setInstructions(0xF115)

        machine.V[1] = 123

        machine.boot()

        assertEquals(123, machine.delayTimer)
    }

    @Test
    fun shouldSetSoundTimerToVx() {
        setInstructions(0xF118)

        machine.V[1] = 123

        machine.boot()

        assertEquals(123, machine.soundTimer)
    }

    @Test
    fun shouldAddVxToI() {
        setInstructions(0xF11E)

        machine.I = 3
        machine.V[1] = 4

        machine.boot()

        assertEquals(7, machine.I)
    }

    @Test
    fun shouldReadBCDRepresentationOfVxToMemory() {
        setInstructions(0xF133)

        machine.V[1] = 51
        machine.I = 1000

        machine.boot()

        assertEquals(0, machine.memory[1000])
        assertEquals(5, machine.memory[1001])
        assertEquals(1, machine.memory[1002])
    }

    @Test
    fun shouldStoreRegistersInMemoryStartingAtI() {
        setInstructions(0xF255)

        machine.I = 1000
        machine.V[0] = 2
        machine.V[1] = 7
        machine.V[2] = 9

        machine.boot()

        assertEquals(2, machine.memory[1000])
        assertEquals(7, machine.memory[1001])
        assertEquals(9, machine.memory[1002])
    }

    @Test
    fun shouldReadRegistersFromMemoryStartingAtI() {
        setInstructions(0xF265)

        machine.I = 1000
        machine.memory[1000] = 3
        machine.memory[1001] = 4
        machine.memory[1002] = 2

        machine.boot()

        assertEquals(3, machine.V[0])
        assertEquals(4, machine.V[1])
        assertEquals(2, machine.V[2])
    }

    companion object {
        @JvmStatic
        fun skipsInstructionWhenVxEqualsKkProvider(): List<Arguments> {
            return listOf(
                // skip on equal
                Arguments.of(0x30F0, 0xF0, 516),
                Arguments.of(0x30F0, 0xF1, 514),

                // skip on not equal
                Arguments.of(0x41F0, 0xF0, 514),
                Arguments.of(0x41F0, 0xF1, 516)
            )
        }

        @JvmStatic
        fun skipsInstructionWhenRegistersEqualProvider(): List<Arguments> {
            return listOf(
                Arguments.of(0x5000, 1, 2, 514),
                Arguments.of(0x5000, 3, 3, 516),
                Arguments.of(0x9000, 3, 3, 514),
                Arguments.of(0x9000, 1, 2, 516)
            )
        }

        @JvmStatic
        fun mathProvider(): List<Arguments> {
            return listOf(
                Arguments.of(0x8250, 0, 5, 0, 5),
                Arguments.of(0x8251, 0x0101, 0x1010, 0, 0x1111),
                Arguments.of(0x8252, 0x3034, 0xFFF0, 0, 0x3030),
                Arguments.of(0x8253, 0xEF, 0xFF, 0, 0x10),
                Arguments.of(0x8254, 1, 3, 0, 4),
                Arguments.of(0x8254, 254, 512, 1, 254),
                Arguments.of(0x8255, 5, 3, 1, 2),
                Arguments.of(0x8255, 5, 6, 0, 255),
                Arguments.of(0x8206, 0b11110001, 0, 1, 0b01111000),
                Arguments.of(0x8206, 0b11110000, 0, 0, 0b01111000),
                Arguments.of(0x8257, 3, 9, 1, 6),
                Arguments.of(0x8257, 10, 9, 0, 255),
                Arguments.of(0x820E, 0x81, 0, 1, 2),
                Arguments.of(0x820E, 0x01, 0, 0, 2)
            )
        }

        @JvmStatic
        fun randomizeSource(): List<Arguments> {
            val registers = 0..15
            val low = registers.map { Arguments.of(it, 0) }
            val mid = registers.map { Arguments.of(it, 15) }
            val high = registers.map { Arguments.of(it, 255) }

            return low + mid + high
        }

        @JvmStatic
        fun skipsNextInstructionIfPressedProvider(): List<Arguments> {
            return listOf(
                // skip if pressed
                Arguments.of(0xE09E, 0x0A, 0x0A, 516),
                Arguments.of(0xE09E, 0x0A, 0x0B, 514),

                // skip if not pressed
                Arguments.of(0xE0A1, 0x0A, 0x0A, 514),
                Arguments.of(0xE0A1, 0x0A, 0x0B, 516)
            )
        }

        @JvmStatic
        fun waitForKeyPressProvider(): List<Arguments> {
            return listOf(
                Arguments.of(null, 512),
                Arguments.of(0, 514)
            )
        }
    }
}
