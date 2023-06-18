import androidx.compose.runtime.*
import java.io.File
import kotlin.system.exitProcess

class TopologyAnalyzer {
    val state = mutableStateOf(AnalyzerWindowState.Initialized)
    private val logFile: File? = null
}

enum class AnalyzerWindowState {
    Initialized, Analyzing, Analyzed
}

@Composable
fun TopologyAnalyzerWindow(analyzer: TopologyAnalyzer) {
    var progress by remember { mutableStateOf(0f) }
    var readingLine by remember { mutableStateOf("") }

    DropFileWindow(
        onCloseRequest = { exitProcess(0) }, title = "TopologyAnalyzer",
        onDroppedFile = {

        },
    ) {
        when(analyzer.state.value){
            AnalyzerWindowState.Initialized -> {

            }
            AnalyzerWindowState.Analyzing -> {

            }
            AnalyzerWindowState.Analyzed -> {

            }
        }
    }
}