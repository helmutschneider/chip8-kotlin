import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.provider.MethodSource

data class NibbleItem(
        val value: Int,
        val index: Int,
        val length: Int,
        val expected: Int
)

class InstructionTest {
    companion object {
        @JvmStatic
        fun nibbleSource(): List<NibbleItem> {
            return listOf(
                    NibbleItem(0x00EE, 0, 2, 0x00),
                    NibbleItem(0x00EE, 1, 2, 0x0E),
                    NibbleItem(0x00EE, 2, 2, 0xEE),
                    NibbleItem(0x00EE, 3, 1, 0x0E)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("nibbleSource")
    fun nibbleShouldReturnCorrectValue(item: NibbleItem) {
        val instr = Instruction(item.value)

        assertEquals(item.expected, instr.nibbles(item.index, item.length))
    }
}
