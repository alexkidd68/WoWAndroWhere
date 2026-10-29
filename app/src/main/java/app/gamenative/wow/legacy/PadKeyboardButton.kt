package app.gamenative.wow.legacy

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.winlator.container.Container

/**
 * Part of Controller Menus: holding Select opens or closes the Android keyboard while the 3.3.5a client runs, so the
 * login, code and character name boxes can be typed into without the Back menu. The login screens can't reach the
 * app, so the app watches the button itself. A short press still reaches the game as Select: it's held back until
 * the button comes up, then sent as a tap.
 */
class PadKeyboardButton(private val toggleKeyboard: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var pressed: KeyEvent? = null
    private var toggled = false
    private val hold = Runnable {
        toggled = true
        toggleKeyboard()
    }

    /** Takes Select from the pad and passes a short press on through [forward]. */
    fun onKeyEvent(event: KeyEvent, forward: (KeyEvent) -> Boolean): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_BUTTON_SELECT) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                pressed = event
                toggled = false
                handler.postDelayed(hold, HOLD_MS)
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(hold)
                val down = pressed ?: return true
                pressed = null
                if (!toggled) {
                    forward(down)
                    forward(event)
                }
            }
        }
        return true
    }

    fun cancel() {
        handler.removeCallbacks(hold)
        pressed = null
    }

    companion object {
        const val EXTRA = "padKeyboard"
        const val HOLD_MS = 500L

        fun isOn(container: Container) = container.getExtra(EXTRA) == "1"
    }
}
