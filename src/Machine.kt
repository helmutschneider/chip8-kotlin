import java.io.InputStream
import java.lang.Exception
import kotlin.random.Random

private val CHARACTER_SET = intArrayOf(
    0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
    0x20, 0x60, 0x20, 0x20, 0x70, // 1
    0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
    0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
    0x90, 0x90, 0xF0, 0x10, 0x10, // 4
    0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
    0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
    0xF0, 0x10, 0x20, 0x40, 0x40, // 7
    0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
    0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
    0xF0, 0x90, 0xF0, 0x90, 0x90, // A
    0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
    0xF0, 0x80, 0x80, 0x80, 0xF0, // C
    0xE0, 0x90, 0x90, 0x90, 0xE0, // D
    0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
    0xF0, 0x80, 0xF0, 0x80, 0x80  // F
)

// 500 hz
private const val TICK_SIZE_EMULATION = 1e9/500

// 60 hz
private const val TICK_SIZE_TIMER = 1e9/60

class Machine(val rom: InputStream, val io: InputOutput) {
    var run = true
    val memory = IntArray(4096)
    val V = IntArray(16)
    val stack = IntArray(16)
    var I = 0
    var delayTimer = 0
    var soundTimer = 0
    var programCounter = 0
    var stackPointer = 0
    var onCycle: ((Machine, Instruction) -> Unit)? = null
    var displayBuffer = DisplayBuffer()

    fun boot() {
        for (i in CHARACTER_SET.indices) {
            memory[i] = CHARACTER_SET[i]
        }

        val bytes = rom.readAllBytes()

        for (i in bytes.indices) {
            memory[i + 512] = bytes[i].toInt().and(0xFF)
        }

        programCounter = 512

        var prevEmulationTick = System.nanoTime()
        var prevTimerTick = System.nanoTime()

        while (run) {
            var t = System.nanoTime()

            if (t - prevEmulationTick >= TICK_SIZE_EMULATION) {
                prevEmulationTick = t
                cycle()
            }

            t = System.nanoTime()

            if (t - prevTimerTick >= TICK_SIZE_TIMER) {
                prevTimerTick = t

                if (delayTimer > 0) {
                    delayTimer -= 1
                }
                if (soundTimer > 0) {
                    soundTimer -= 1
                }

                io.draw(displayBuffer)
            }

            Thread.sleep(0, 1)
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

        programCounter += 2

        when (instr.value) {
            // Clear the display.
            0x00E0 -> {
                displayBuffer = DisplayBuffer()
            }
            // Return from a subroutine.
            0x00EE -> {
                stackPointer -= 1
                programCounter = stack[stackPointer]
            }
            // Jump to a machine code routine at nnn.
            0x0FFF.and(instr.value) -> {
                // do nothing.
            }
            // Jump to location nnn.
            0x1FFF.and(instr.value) -> {
                programCounter = instr.nnn
            }
            // Call subroutine at nnn.
            0x2FFF.and(instr.value) -> {
                stack[stackPointer] = programCounter
                stackPointer += 1
                programCounter = instr.nnn
            }
            // Skip next instruction if Vx = kk.
            0x3FFF.and(instr.value) -> {
                if (V[instr.x] == instr.kk) {
                    programCounter += 2
                }
            }
            // Skip next instruction if Vx != kk.
            0x4FFF.and(instr.value) -> {
                if (V[instr.x] != instr.kk) {
                    programCounter += 2
                }
            }
            // Skip next instruction if Vx = Vy.
            0x5FF0.and(instr.value) -> {
                if (V[instr.x] == V[instr.y]) {
                    programCounter += 2
                }
            }
            // Set Vx = kk.
            0x6FFF.and(instr.value) -> {
                V[instr.x] = instr.kk
            }
            // Set Vx = Vx + kk.
            0x7FFF.and(instr.value) -> {
                V[instr.x] += instr.kk
            }
            // Set Vx = Vy.
            0x8FF0.and(instr.value) -> {
                V[instr.x] = V[instr.y]
            }
            // Set Vx = Vx OR Vy.
            0x8FF1.and(instr.value) -> {
                V[instr.x] = V[instr.x].or(V[instr.y])
            }
            // Set Vx = Vx AND Vy.
            0x8FF2.and(instr.value) -> {
                V[instr.x] = V[instr.x].and(V[instr.y])
            }
            // Set Vx = Vx XOR Vy.
            0x8FF3.and(instr.value) -> {
                V[instr.x] = V[instr.x].xor(V[instr.y])
            }
            // Set Vx = Vx + Vy, set VF = carry.
            0x8FF4.and(instr.value) -> {
                val result = V[instr.x] + V[instr.y]
                V[15] = if (result > 255) 1 else 0
                V[instr.x] = result.and(0xFF)
            }
            // Set Vx = Vx - Vy, set VF = NOT borrow.
            0x8FF5.and(instr.value) -> {
                val result = V[instr.x] - V[instr.y]
                V[15] = if (result > 0) 1 else 0
                V[instr.x] = result.and(0xFF)
            }
            // Set Vx = Vx SHR 1.
            0x8FF6.and(instr.value) -> {
                V[15] = V[instr.x].and(0x01)
                V[instr.x] = V[instr.x].shr(1)
            }
            // Set Vx = Vy - Vx, set VF = NOT borrow.
            0x8FF7.and(instr.value) -> {
                val result = V[instr.y] - V[instr.x]
                V[15] = if (result > 0) 1 else 0
                V[instr.x] = result.and(0xFF)
            }
            // Set Vx = Vx SHL 1.
            0x8FFE.and(instr.value) -> {
                V[15] = V[instr.x].shr(7)
                V[instr.x] = V[instr.x].shl(1).and(0xFF)
            }
            // Skip next instruction if Vx != Vy.
            0x9FF0.and(instr.value) -> {
                if (V[instr.x] != V[instr.y]) {
                    programCounter += 2
                }
            }
            // Set I = nnn.
            0xAFFF.and(instr.value) -> {
                I = instr.nnn
            }
            // Jump to location nnn + V0.
            0xBFFF.and(instr.value) -> {
                programCounter = instr.nnn + V[0]
            }
            // Set Vx = random byte AND kk.
            0xCFFF.and(instr.value) -> {
                V[instr.x] = Random.nextBits(8).and(0xFF).and(instr.kk)
            }
            // Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision.
            0xDFFF.and(instr.value) -> {
                val bytes = memory.slice(I until (I + instr.n))
                val xOrigin = V[instr.x]
                val yOrigin = V[instr.y]
                var didErase = false

                for (idx in bytes.indices) {
                    for (bit in 7 downTo 0) {
                        val x = (xOrigin + 7 - bit).rem(64)
                        val y = (yOrigin + idx).rem(32)
                        val current = displayBuffer.pixels[x][y]
                        val next = (bytes[idx].shr(bit).and(0x01) > 0).xor(current)
                        didErase = didErase || (current && !next)

                        displayBuffer.pixels[x][y] = next
                    }
                }

                V[15] = if (didErase) 1 else 0
            }
            // Skip next instruction if key with the value of Vx is pressed.
            0xEF9E.and(instr.value) -> {
                val key = V[instr.x]

                if (io.getPressedKeys().contains(key)) {
                    programCounter += 2
                }
            }
            // Skip next instruction if key with the value of Vx is not pressed.
            0xEFA1.and(instr.value) -> {
                val key = V[instr.x]

                if (!io.getPressedKeys().contains(key)) {
                    programCounter += 2
                }
            }
            // Set Vx = delay timer value.
            0xFF07.and(instr.value) -> {
                V[instr.x] = delayTimer
            }
            // Wait for a key press, store the value of the key in Vx.
            0xFF0A.and(instr.value) -> {
                val keys = io.getPressedKeys()

                if (keys.isEmpty()) {
                    // if no key is pressed we need to execute
                    // this instruction again.
                    programCounter -= 2
                } else {
                    V[instr.x] = keys.first()
                }
            }
            // Set delay timer = Vx.
            0xFF15.and(instr.value) -> {
                delayTimer = V[instr.x]
            }
            // Set sound timer = Vx.
            0xFF18.and(instr.value) -> {
                soundTimer = V[instr.x]
            }
            // Set I = I + Vx.
            0xFF1E.and(instr.value) -> {
                I += V[instr.x]
            }
            // Set I = location of sprite for digit Vx.
            0xFF29.and(instr.value) -> {
                I = V[instr.x] * 5
            }
            // Store BCD representation of Vx in memory locations I, I+1, and I+2.
            0xFF33.and(instr.value) -> {
                val str = V[instr.x].toString().padStart(3, '0')

                for (i in str.indices) {
                    memory[I + i] = Integer.parseInt(str[i].toString())
                }
            }
            // Store registers V0 through Vx in memory starting at location I.
            0xFF55.and(instr.value) -> {
                for (idx in 0..instr.x) {
                    memory[I + idx] = V[idx]
                }
            }
            // Read registers V0 through Vx from memory starting at location I.
            0xFF65.and(instr.value) -> {
                for (idx in 0..instr.x) {
                    V[idx] = memory[I + idx]
                }
            }
            else -> {
                throw Exception("Unknown instruction \"%s\"".format(Integer.toHexString(instr.value)))
            }
        }

        onCycle?.let { it(this, instr) }
    }
}
