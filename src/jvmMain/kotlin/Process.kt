import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun ProgressBarComponent(progress: Float, processName: String) {

    val textMeasurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawText(
                        text = "${floor(progress * 10000) / 100}%",
                        textMeasurer = textMeasurer,
                        topLeft = Offset(x = size.width.toFloat() * progress, y = 0f),
                        maxLines = 1,
                    )
                }
                .padding(vertical = 10.dp)
                .onSizeChanged { size = it },
            progress = progress
        )
        Text(processName, maxLines = 1)
    }
}

enum class ProcessState { NotProcessing, Processing }