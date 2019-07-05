import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.lang.NumberFormatException
import javax.swing.*

interface InputOutput {
    fun getPressedKeys(): Set<Int>
}

class SwingInputOutput: InputOutput {
    val frame: JFrame = JFrame("CHIP8")
    private val pressedKeys = mutableSetOf<Int>()

    init {
        frame.size = Dimension(640, 480)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.addKeyListener(object: KeyListener {
            override fun keyTyped(event: KeyEvent?) {
            }
            override fun keyPressed(event: KeyEvent?) {
                getKeyValue(event)?.let { pressedKeys.add(it) }
            }
            override fun keyReleased(event: KeyEvent?) {
                getKeyValue(event)?.let { pressedKeys.remove(it) }
            }
        })
        frame.isVisible = true
    }

    private fun getKeyValue(event: KeyEvent?): Int? {
        if (event == null) {
            return null
        }
        return try {
            Integer.parseInt(event.keyChar.toString(), 16)
        } catch (e: NumberFormatException) {
            null
        }
    }

    override fun getPressedKeys() = pressedKeys
}
