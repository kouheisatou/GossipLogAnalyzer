import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

class GossipLogAnalyzer {
    val state = mutableStateOf(AnalyzerWindowState.Initialized)
    val errorMsg = mutableStateOf<String?>(null)
    private var logFile: File? = null
    private var maxLine: Long? = null
    private var lineCount = 0

    private val channelHashSet = ChannelHashSet()
    var channels: List<Channel>? = null

    fun analyze(
        logFile: File,
        onFinished: () -> Unit,
        processPerLine: (readingLine: String?, progress: Float) -> Unit
    ) {
        this.logFile = logFile
        this.maxLine = Files.lines(Paths.get(logFile.path)).count()
        state.value = AnalyzerWindowState.Analyzing

        CoroutineScope(Dispatchers.Default).launch {
            BufferedReader(FileReader(logFile)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    try {
                        val csvElements = line?.split(",") ?: listOf()
                        val channelUpdate = ChannelUpdate(
                            csvElements[0],
                            csvElements[1],
                            csvElements[2],
                            csvElements[3],
                            csvElements[4],
                            csvElements[5],
                            csvElements[6],
                            csvElements[7].toFloat(),
                            csvElements[8],
                            csvElements[9],
                            csvElements[10].toFloat(),
                        )
                        channelHashSet.add(channelUpdate)
                    } catch (e: Exception) {
                        errorMsg.value = e.message
                        state.value = AnalyzerWindowState.Initialized
                        return@launch
                    }

                    processPerLine(line, lineCount.toFloat() / maxLine!!)
                    lineCount++
                }

                channels = channelHashSet.toList()
                onFinished()
                state.value = AnalyzerWindowState.Analyzed
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
fun GossipLogAnalyzerWindow(analyzer: GossipLogAnalyzer) {
    var progress by remember { mutableStateOf(0f) }
    var readingLine by remember { mutableStateOf("") }
    DropFileWindow(
        onCloseRequest = { exitProcess(0) },
        title = "GossipLogAnalyzer",
        onDroppedFile = {

            if (!it.path.endsWith(".csv")) {
                analyzer.errorMsg.value = "File extension is not CSV."
                return@DropFileWindow
            }

            try {
                analyzer.analyze(
                    it,
                    onFinished = {
                        progress = 0f
                        readingLine = ""
                    },
                    processPerLine = { r, p ->
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
                Text("Drop log file here!", modifier = Modifier.fillMaxSize(), textAlign = TextAlign.Center)
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


                var selectedChannel by remember { mutableStateOf<Channel?>(null) }

                Column {
                    Row {
                        Text("ChannelID")
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Updates")
                    }
                    Divider(modifier = Modifier.fillMaxWidth())
                    Row {
                        val listState = rememberLazyListState()
                        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                            items(analyzer.channels ?: listOf()) {
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            selectedChannel = it
                                        }.background(
                                            if (it.shortChannelId == selectedChannel?.shortChannelId) {
                                                Color.LightGray
                                            } else {
                                                Color.White
                                            }
                                        )
                                ) {
                                    Text(it.shortChannelId)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(it.channelUpdates.size.toString())
                                }
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier.fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(listState),
                        )
                    }
                }
                if (selectedChannel != null) {

                    Window(
                        onCloseRequest = { selectedChannel = null },
                        title = selectedChannel?.shortChannelId.toString(),
                        onKeyEvent = {
                            if (it.type == KeyEventType.KeyDown) {
                                when (it.key.keyCode) {
                                    Key.Escape.keyCode -> {
                                        selectedChannel = null
                                    }

                                    Key.DirectionDown.keyCode -> {
                                        val index = analyzer.channels?.indexOf(selectedChannel)?.plus(1)
                                        if (index != null && analyzer.channels != null) {
                                            if (index in 0 until analyzer.channels!!.size) {
                                                selectedChannel = analyzer.channels!![index]
                                            }
                                        }
                                    }

                                    Key.DirectionUp.keyCode -> {
                                        val index = analyzer.channels?.indexOf(selectedChannel)?.minus(1)
                                        if (index != null && analyzer.channels != null) {
                                            if (index in 0 until analyzer.channels!!.size) {
                                                selectedChannel = analyzer.channels!![index]
                                            }
                                        }
                                    }
                                }
                            }
                            false
                        }
                    ) {

                        Column {

                            val data = XYSeriesCollection()
                            val htlcMaximumMsatSeries = XYSeries("htlcMaximumMsat", true)
                            selectedChannel?.channelUpdates?.forEach {
                                val timeInt = LocalDateTime.parse(
                                    it.timestamp,
                                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                                ).toEpochSecond(ZoneOffset.UTC)

                                htlcMaximumMsatSeries.add(XYDataItem(timeInt, it.htlcMaximumMsat))
                            }
                            data.addSeries(htlcMaximumMsatSeries)
                            val chart = ChartFactory.createScatterPlot(
                                null,
                                "timestamp[ms]",
                                "htlcMaximumMsat[BTC]",
                                data,
                            )
                            (chart.plot as XYPlot).renderer =
                                XYLineAndShapeRenderer().apply { setSeriesLinesVisible(0, true) }
                            val chartPane = ChartPanel(chart)

                            SwingPanel(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                factory = {
                                    chartPane
                                },
                                update = {
                                    chartPane.chart = chart
                                }
                            )

                            Divider(modifier = Modifier.fillMaxWidth())

                            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                items(selectedChannel?.channelUpdates ?: listOf()) {
                                    Text(it.toString())
                                }
                            }
                        }
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