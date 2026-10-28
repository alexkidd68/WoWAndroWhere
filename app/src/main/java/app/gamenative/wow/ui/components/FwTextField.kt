package app.gamenative.wow.ui.components

import android.view.KeyEvent as AndroidKey
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import app.gamenative.wow.ui.theme.Fw

/** An outlined text box for controller screens that opens the keyboard only on A. See [TypeOnA]. */
@Composable
fun FwTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    TypeOnA(modifier, focusRequester) { fieldModifier, focused, stop ->
        OutlinedTextField(
            value,
            onValueChange,
            singleLine = true,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            textStyle = Fw.Text,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { stop() }),
            colors = if (focused) {
                OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Fw.Brass, unfocusedLabelColor = Fw.Brass)
            } else {
                OutlinedTextFieldDefaults.colors()
            },
            modifier = fieldModifier.fillMaxWidth(),
        )
    }
}

/**
 * Wraps a text field so moving onto it with the D-pad only highlights it ([content] gets focused = true);
 * A, Enter or a tap starts typing and opens the keyboard. Done (call stop), B or moving away stops typing.
 * [content] must put the given modifier on its text field.
 */
@Composable
fun TypeOnA(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    content: @Composable (fieldModifier: Modifier, focused: Boolean, stop: () -> Unit) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    val stop = {
        editing = false
        keyboard?.hide()
        runCatching { focusRequester.requestFocus() }
        Unit
    }

    Box(
        modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { e ->
                val code = e.nativeKeyEvent.keyCode
                when {
                    !editing && e.type == KeyEventType.KeyDown &&
                        (code == AndroidKey.KEYCODE_BUTTON_A || code == AndroidKey.KEYCODE_DPAD_CENTER || code == AndroidKey.KEYCODE_ENTER) -> {
                        editing = true
                        true
                    }
                    editing && (code == AndroidKey.KEYCODE_BUTTON_B || code == AndroidKey.KEYCODE_BACK) -> {
                        if (e.type == KeyEventType.KeyDown) stop()
                        true
                    }
                    else -> false
                }
            }
            .focusable(interactionSource = interaction),
    ) {
        content(
            Modifier
                .focusRequester(field)
                .focusProperties { canFocus = editing }
                .onFocusChanged {
                    if (it.isFocused) {
                        hadFocus = true
                    } else if (hadFocus) {
                        hadFocus = false
                        editing = false
                    }
                },
            focused,
            stop,
        )
        // Taps start typing too. Not clickable, so the D-pad doesn't stop on it as a second item.
        if (!editing) Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { editing = true } })
    }
    LaunchedEffect(editing) {
        if (editing) {
            runCatching { field.requestFocus() }
            keyboard?.show()
        }
    }
}
