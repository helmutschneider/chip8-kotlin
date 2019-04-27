fun main() {
    val rom = Machine::class.java.getResourceAsStream("Space Invaders [David Winter].ch8")
    val machine = Machine(rom)

    machine.boot()
}
