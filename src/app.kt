import java.nio.file.Files
import java.nio.file.Paths

fun main(argv: Array<String>) {
    val rom = if (argv.isNotEmpty()) {
        Files.newInputStream(
            Paths.get(argv[0])
        )
    } else {
        Machine::class.java.getResourceAsStream("Space Invaders [David Winter].ch8")
    }

    val machine = Machine(rom, SwingInputOutput())

    machine.boot()
}
