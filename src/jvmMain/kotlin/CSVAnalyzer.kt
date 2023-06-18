import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYDataItem
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

enum class AnalyzerWindowState {
    Initialized, Analyzing, Analyzed
}

abstract class CSVAnalyzer {

    val state = mutableStateOf(AnalyzerWindowState.Initialized)
    val errorMsg = mutableStateOf<String?>(null)
    private var logFile: File? = null
    private var maxLine: Long? = null
    private var lineCount = 0


    fun analyze(
        logFile: File,
        progress: (readingLine: String?, progress: Float) -> Unit
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
                        errorMsg.value = e.message
                        state.value = AnalyzerWindowState.Initialized
                        return@launch
                    }

                    progress(line, lineCount.toFloat() / maxLine!!)
                    lineCount++
                }

                onAnalyzingFinished()
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
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
fun <T> CSVAnalyzerWindow(
    windowTitle: String,
    analyzer: CSVAnalyzer,
    dropFileMsg: String,
    listData: List<T>,
    detailWindowTitle: (selectedItem: T?) -> String,
    detailWindowLayout: @Composable FrameWindowScope.(selectedItem: T?) -> Unit,
    listTopRowLayout: @Composable RowScope.() -> Unit,
    listItemLayout: @Composable RowScope.(listItem: T) -> Unit,
) {
    var progress by remember { mutableStateOf(0f) }
    var readingLine by remember { mutableStateOf("") }
    DropFileWindow(
        onCloseRequest = { exitProcess(0) },
        title = windowTitle,
        onDroppedFile = {

            if (!it.path.endsWith(".csv")) {
                analyzer.errorMsg.value = "File extension is not CSV."
                return@DropFileWindow
            }

            val errorMsg = analyzer.onLogFileLoaded(it)
            if (errorMsg != null) {
                analyzer.errorMsg.value = errorMsg
                return@DropFileWindow
            }

            try {
                analyzer.analyze(
                    it,
                    progress = { r, p ->
                        progress = p
                        readingLine = r ?: ""
                    }
                )
            } catch (e: Exception) {
                analyzer.errorMsg.value = e.message
            }
        },
    ) {
        when (analyzer.state.value) {
            AnalyzerWindowState.Initialized -> {
                Text(dropFileMsg, modifier = Modifier.fillMaxSize(), textAlign = TextAlign.Center)
            }

            AnalyzerWindowState.Analyzing -> {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().padding(20.dp))
                    Text(readingLine)
                }
            }

            AnalyzerWindowState.Analyzed -> {


                var selected by remember { mutableStateOf<T?>(null) }

                Column {
                    Row {
                        listTopRowLayout()
                    }
                    Divider(modifier = Modifier.fillMaxWidth())
                    Row {
                        val listState = rememberLazyListState()
                        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                            items(listData) {
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            selected = it
                                        }
                                        .background(
                                            if (selected == it) {
                                                Color.LightGray
                                            } else {
                                                Color.White
                                            }
                                        )
                                ) {
                                    listItemLayout(it)
                                }
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier.fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(listState),
                        )
                    }
                }
                if (selected != null) {

                    Window(
                        onCloseRequest = { selected = null },
                        title = detailWindowTitle(selected),
                        onKeyEvent = {
                            if (it.type == KeyEventType.KeyDown) {
                                when (it.key.keyCode) {
                                    Key.Escape.keyCode -> {
                                        selected = null
                                    }

                                    Key.DirectionDown.keyCode -> {
                                        val index = listData.indexOf(selected) + 1
                                        if (index in listData.indices) {
                                            selected = listData[index]
                                        }
                                    }

                                    Key.DirectionUp.keyCode -> {
                                        val index = listData.indexOf(selected) - 1
                                        if (index in listData.indices) {
                                            selected = listData[index]
                                        }
                                    }
                                }
                            }
                            false
                        }
                    ) {
                        detailWindowLayout(selected)
                    }
                }
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