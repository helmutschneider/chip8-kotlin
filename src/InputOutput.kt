import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.lang.NumberFormatException
import javax.swing.*

class DisplayBuffer {
    val pixels = Array(64) {
        BooleanArray(32)
    }
}

interface InputOutput {
    fun getPressedKeys(): Set<Int>
    fun draw(buffer: DisplayBuffer): Unit
}

class SwingInputOutput: InputOutput {
    private var displayBuffer = DisplayBuffer()
    private val frame: JFrame = JFrame("CHIP8")
    private val panel = object: JPanel() {
        override fun paintComponent(g: Graphics?) {
            super.paintComponent(g)

            if (g == null) {
                return
            }

            g.color = Color.black

            val pixels = displayBuffer.pixels
            val scale = size.width / pixels.size

            for (colIdx in pixels.indices) {
                for (rowIdx in pixels[colIdx].indices) {
                    val px = pixels[colIdx][rowIdx]

                    if (px) {
                        g.fillRect(
                            colIdx * scale,
                            rowIdx * scale,
                            scale,
                            scale
                        )
                    }
                }
            }
        }
    }
    private val pressedKeys = mutableSetOf<Int>()

    init {
        // the display size is 640x320 but the title bar appears
        // to be 22 pixels tall on MacOS.
        panel.preferredSize = Dimension(640, 320)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.addKeyListener(object: KeyAdapter() {
            override fun keyPressed(event: KeyEvent?) {
                getKeyValue(event)?.let { pressedKeys.add(it) }
            }
            override fun keyReleased(event: KeyEvent?) {
                getKeyValue(event)?.let { pressedKeys.remove(it) }
            }
        })
        frame.add(panel)
        frame.isResizable = false
        frame.pack()
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

    override fun draw(buffer: DisplayBuffer) {
        this.displayBuffer = buffer
        this.panel.repaint()
    }
}
