import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.provider.Arguments

class InstructionTest {
    @ParameterizedTest
    @MethodSource("extractProvider")
    fun shouldExtractInstructionValues(value: Int, nnn: Int, n: Int, x: Int, y: Int, kk: Int) {
        val instruction = Instruction(value)

        assertEquals(nnn, instruction.nnn)
        assertEquals(n, instruction.n)
        assertEquals(x, instruction.x)
        assertEquals(y, instruction.y)
        assertEquals(kk, instruction.kk)
    }

    companion object {
        @JvmStatic
        fun extractProvider(): List<Arguments> {
            return listOf(
                Arguments.of(0xEFA1, 0x0FA1, 0x01, 0x0F, 0x0A, 0xA1),
                Arguments.of(0xFFFF, 0x0FFF, 0x0F, 0x0F, 0x0F, 0xFF)
            )
        }
    }
}
