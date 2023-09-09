package analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths

enum class AnalyzerWindowState {
    Initialized, Analyzing, Analyzed
}

abstract class CSVAnalyzer {

    val state = mutableStateOf(AnalyzerWindowState.Initialized)
    val errorMsg = mutableStateOf<String?>(null)
    private var logFile: File? = null
    private var maxLine: Long? = null
    private var lineCount = 0
    var progress = mutableStateOf(0f)
    var readingLine = mutableStateOf("")


    fun load(
        logFile: File,
        onLoadCompleted: () -> Unit,
    ) {
        lineCount = 0
        this.logFile = logFile
        this.maxLine = Files.lines(Paths.get(logFile.path)).count()
        state.value = AnalyzerWindowState.Analyzing

        CoroutineScope(Dispatchers.Default).launch {
            BufferedReader(FileReader(logFile)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    try {
                        analyzeCSVLine(line)
                    } catch (e: Exception) {
                        if (lineCount != 0) {
                            e.printStackTrace()
                            errorMsg.value = e.message
                            state.value = AnalyzerWindowState.Initialized
                            return@launch
                        }
                    }

                    readingLine.value = line ?: ""
                    progress.value = lineCount.toFloat() / maxLine!!
                    lineCount++
                }

                onAnalyzingFinished()
                onLoadCompleted()
                state.value = AnalyzerWindowState.Analyzed
            }
        }
    }

    abstract fun analyzeCSVLine(lineText: String?)
    abstract fun onAnalyzingFinished()

    open fun onLogFileLoaded(logFile: File): String? {
        return null
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun CSVAnalyzerWindow(
    windowTitle: String,
    analyzer: CSVAnalyzer,
    layoutOnAnalyzeCompleted: @Composable () -> Unit,
) {
    Window(
        onCloseRequest = {},
        title = windowTitle,
    ) {
        when (analyzer.state.value) {

            AnalyzerWindowState.Initialized -> {
                analyzer.state.value = AnalyzerWindowState.Analyzing
            }

            AnalyzerWindowState.Analyzing -> {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    LinearProgressIndicator(
                        progress = analyzer.progress.value,
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    )
                    Text(analyzer.readingLine.value)
                }
            }

            AnalyzerWindowState.Analyzed -> {
                layoutOnAnalyzeCompleted()
            }
        }

        if (analyzer.errorMsg.value != null) {
            AlertDialog(
                onDismissRequest = {
                    analyzer.errorMsg.value = null
                },
                buttons = {
                    TextButton(
                        onClick = {
                            analyzer.errorMsg.value = null
                        }
                    ) {
                        Text("OK")
                    }
                },
                title = { Text("error") },
                text = { Text(analyzer.errorMsg.value ?: "") }
            )
        }
    }
}