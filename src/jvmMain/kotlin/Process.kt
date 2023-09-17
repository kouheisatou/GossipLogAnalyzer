import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor


class Process {
    var progress = mutableStateOf(0f)
    var processName = mutableStateOf<String?>(null)
    var state = mutableStateOf(ProcessState.NotProcessing)
}

@Composable
fun ProgressBarComponent(progress: Float, processName: String){

    val textMeasurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .onSizeChanged { size = it }
            .drawBehind {
                drawText(
                    text = "${floor(progress * 10000) / 100}%",
                    textMeasurer = textMeasurer,
                    topLeft = Offset(x = size.width.toFloat() * progress, y = 0f),
                    maxLines = 1,
                )
            },
    ) {
        Column {

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                progress = progress
            )
            Text(processName)
        }
    }
}

enum class ProcessState { NotProcessing, Processing }