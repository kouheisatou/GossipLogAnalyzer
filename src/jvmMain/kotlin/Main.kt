import analyzer.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import edu.uci.ics.jung.layout.algorithms.StaticLayoutAlgorithm
import network.Channel
import network.ChannelDetailComponent
import network.Node
import network.NodeDetailComponent
import ui.FileDialog
import ui.MultipleFileLoadComponent
import ui.SelectableListComponent
import java.awt.Dimension
import java.awt.FileDialog
import java.io.File


val channelUpdateAnalyzer = ChannelUpdateAnalyzer()
val channelAnnouncementAnalyzer = ChannelAnnouncementAnalyzer()
val paymentsOutputAnalyzer = PaymentsOutputAnalyzer()

val channels = mutableMapOf<String, Channel>()
val nodes = mutableMapOf<String, Node>()

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {

    val files = mutableMapOf<String, File?>(
        "payments_output.csv" to null,
        "channels_output.csv" to null,
        "edges_output.csv" to null,
        "nodes_output.csv" to null
    )
    Window(onCloseRequest = {}, title = "file load test") {
        MultipleFileLoadComponent(files)
    }

    CSVAnalyzerWindow(
        windowTitle = "PaymentsOutputAnalyzer",
        analyzer = paymentsOutputAnalyzer,
        dropFileMsg = "Drop payments_output.csv here.",
        layoutOnAnalyzeCompleted = {

        },
        onWindowInitialized = {},
    )

    if (channelUpdateAnalyzer.state.value == AnalyzerWindowState.Analyzed && channelAnnouncementAnalyzer.state.value == AnalyzerWindowState.Analyzed) {
        Window(
            onCloseRequest = {},
            title = "topology"
        ) {
            val topology by remember {
                mutableStateOf(
                    Topology(
                        Dimension(19200, 10800),
                        30,
                        StaticLayoutAlgorithm(),
                        nodes, channels,
                    )
                )
            }
            TopologyComponent(topology)


            var isFileDialogOpened by remember { mutableStateOf(false) }
            if (isFileDialogOpened) {
                FileDialog(
                    mode = FileDialog.SAVE,
                    file = "estimated_demands.csv",
                    onCloseRequest = { directory, filename ->
                        println("$directory/$filename")
                        if (filename != null && directory != null) {
                            val file = File(directory, filename).apply { this.writeText("node_id,demand\n") }

                            topology.estimatedDemand.toList().sortedByDescending { it.second }.forEach {
                                file.appendText("${it.first.id},${it.second}\n")
                            }
                        }
                        isFileDialogOpened = false
                    }
                )
            }

            MenuBar {
                Menu("file") {
                    Item(
                        "Save Estimated Demand to CSV",
                        onClick = {
                            isFileDialogOpened = true
                        },
                        shortcut = KeyShortcut(Key.S, meta = true)
                    )
                }
            }
        }
    }


    CSVAnalyzerWindow(
        "NodeList",
        channelAnnouncementAnalyzer,
        "Drop channel_announcement log file here!",
        layoutOnAnalyzeCompleted = {
            SelectableListComponent(
                channelAnnouncementAnalyzer.nodeListForDisplay.value ?: listOf(),
                detailWindowTitle = { "Node ${it?.id}" },
                detailWindowLayout = {
                    if (it != null) {
                        NodeDetailComponent(it)
                    }
                },
                listTopRowLayout = {
                    Row {
                        Text("NodeID")
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Channels")
                    }
                },
                listItemLayout = { node: Node ->
                    Row {
                        Text(node.id)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(node.channels.size.toString())
                    }
                },
                fetchLatestDetail = {
                    nodes[it.id]
                },
                findByText = {
                    nodes[it]
                },
                clipboardText = {
                    it.id
                },
            )
        },
        onWindowInitialized = {
            /*val sampleLogFile = File("channel_announcement_log_sample.csv")
            if (sampleLogFile.exists()) {
                it.load(sampleLogFile) { readingLine, progress ->
                    it.progress.value = progress
                    it.readingLine.value = readingLine ?: ""
                }
            }*/
        }
    )

    if (channelAnnouncementAnalyzer.state.value == AnalyzerWindowState.Analyzed) {
        CSVAnalyzerWindow(
            "ChannelList",
            channelUpdateAnalyzer,
            "Drop channel_update log file here!",
            layoutOnAnalyzeCompleted = {
                SelectableListComponent(
                    listDataForDisplay = channelUpdateAnalyzer.channelsForDisplay.value ?: listOf(),
                    detailWindowTitle = { "Channel ${it?.shortChannelId}" },
                    detailWindowLayout = { selected ->
                        if (selected != null) {
                            ChannelDetailComponent(selected)
                        }
                    },
                    listTopRowLayout = {
                        Row {
                            Text("ChannelID")
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Updates")
                        }
                    },
                    listItemLayout = { channel: Channel ->
                        Row {
                            Text(channel.shortChannelId)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(channel.channelUpdates.size.toString())
                        }
                    },
                    fetchLatestDetail = {
                        channels[it.shortChannelId]
                    },
                    findByText = {
                        channels[it]
                    },
                    clipboardText = {
                        it.shortChannelId
                    },
                )
            },
            onWindowInitialized = {
                /*val sampleLogFile = File("channel_update_log_sample.csv")
                if (sampleLogFile.exists()) {
                    it.load(sampleLogFile) { readingLine, progress ->
                        it.progress.value = progress
                        it.readingLine.value = readingLine ?: ""
                    }
                }*/
            }
        )
    }
}