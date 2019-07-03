class Instruction(val value: Int) {
    // A 12-bit value, the lowest 12 bits of the instruction
    val nnn = value.and(0x0FFF)

    // A 4-bit value, the lowest 4 bits of the instruction
    val n = value.and(0x000F)

    // A 4-bit value, the lower 4 bits of the high byte of the instruction
    val x = value.shr(8).and(0x000F)

    // A 4-bit value, the upper 4 bits of the low byte of the instruction
    val y = value.shr(4).and(0x000F)

    // An 8-bit value, the lowest 8 bits of the instruction
    val kk = value.and(0x00FF)
}
