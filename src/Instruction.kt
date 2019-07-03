class Instruction(val value: Int) {
    val nnn = value.and(0x0FFF)
    val n = value.and(0x000F)
    val x = value.shr(8).and(0x000F)
    val y = value.shr(4).and(0x000F)
    val kk = value.and(0x00FF)
}
