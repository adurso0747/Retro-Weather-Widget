package com.retroweather

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.retroweather.art.PixelRenderer
import com.retroweather.data.*

@Composable internal fun TextEditor(config: WidgetConfig, weather: Weather?, onApply: (String) -> Unit, onDismiss: () -> Unit) {
    var input by rememberSaveable { mutableStateOf(config.textTemplate) }
    val error = WidgetText.error(input)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Widget text") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val preview = remember(input, config, weather) {
                PixelRenderer.render(config.copy(showTemperature = true, textTemplate = input), weather, 560, 240)
            }
            Image(preview.asImageBitmap(), "Widget text preview", filterQuality = FilterQuality.None, modifier = Modifier.fillMaxWidth().height(120.dp))
            OutlinedTextField(input, { input = it }, label = { Text("Text template") }, isError = error != null,
                minLines = 2, maxLines = 4, supportingText = { Text(error ?: "${input.length}/160 characters") })
            Text("Add a field at the end, or type text and line breaks.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WidgetText.tokens.forEach { token ->
                    TextButton(onClick = { input += "{$token}" }) { Text(token) }
                }
            }
            Text("Text wraps to fit, up to four lines. Extra text ends with dots. The pixel font uses capitals; accents are simplified and unsupported characters appear as ?. Dates and times describe the weather observation in your device timezone (24-hour time).")
            Text("In Follow device mode, the location field reads Current location.")
            TextButton(onClick = { input = WidgetText.DEFAULT }) { Text("Reset to temperature") }
        }
    }, confirmButton = { TextButton(enabled = error == null, onClick = { onApply(input) }) { Text("Use text") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
