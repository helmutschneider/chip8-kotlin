import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import java.io.ByteArrayInputStream
import java.io.InputStream

class MachineTest {
    var machine: Machine? = null
    var romBytes: ByteArray? = null
    var rom: InputStream? = null

    @BeforeEach
    fun setUp() {

    }

    @Test
    fun shouldHandleClearScreenInstruction() {

    }
}
