import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths
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
    listTopRowLayout: @Composable () -> Unit,
    listItemLayout: @Composable (listItem: T) -> Unit,
    findById: (searchText: String) -> T?,
    selectedItem: MutableState<T?> = mutableStateOf(null),
    fetchLatestDetail: (selectedItem: T) -> T?,
    clipboardText: (selectedItem: T?) -> String?,
) {
    var progress by remember { mutableStateOf(0f) }
    var readingLine by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
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
        content = {
            MenuBar {
                Menu("edit") {
                    Item(
                        "Copy",
                        onClick = {
                            // copy to clipboard
                            val text = clipboardText(selectedItem.value ?: return@Item) ?: return@Item
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            println(text)
                        },
                        shortcut = KeyShortcut(Key.C, meta = true)
                    )
                    Item(
                        "Find",
                        onClick = {
                            focusRequester.requestFocus()
                        },
                        shortcut = KeyShortcut(Key.F, meta = true)
                    )
                }
            }

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
                    SelectableListComponent(
                        listData,
                        detailWindowTitle,
                        detailWindowLayout,
                        listItemLayout,
                        listTopRowLayout = {
                            Column {

                                var searchText by remember { mutableStateOf("") }
                                var result by remember { mutableStateOf<T?>(null) }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextField(
                                            modifier = Modifier
                                                .onKeyEvent {
                                                    if (it.type == KeyEventType.KeyDown && it.key.keyCode == Key.Enter.keyCode) {
                                                        result = findById(searchText)
                                                        if (result != null) {
                                                            selectedItem.value = result
                                                        }
                                                    }
                                                    false
                                                }
                                                .focusRequester(focusRequester),
                                            value = searchText,
                                            onValueChange = {
                                                searchText = it
                                            },
                                            singleLine = true,
                                            isError = (searchText != "" && findById(searchText) == null),
                                        )
                                        IconButton(
                                            onClick = {
                                                result = findById(searchText)
                                                if (result != null) {
                                                    selectedItem.value = result
                                                }
                                            }
                                        ) {
                                            Text("🔍")
                                        }
                                    }
                                }

                                listTopRowLayout()
                            }
                        },
                        externalControlledSelectedItem = selectedItem,
                        fetchLatestDetail = fetchLatestDetail,
                        clipboardText = clipboardText,
                    )
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
    )
}