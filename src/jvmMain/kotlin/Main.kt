import analyzer.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.IconButton
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties


val channelUpdateAnalyzer = ChannelUpdateAnalyzer()
val channelAnnouncementAnalyzer = ChannelAnnouncementAnalyzer()
val paymentsOutputAnalyzer = PaymentsOutputAnalyzer()

val channels = mutableMapOf<String, Channel>()
val nodes = mutableMapOf<String, Node>()

val inputFilePathPropertyFile = File("input_files.properties")
val inputFilePathProperty = Properties()

val mainWindowState = mutableStateOf(MainWindowState.Initialized)

enum class MainWindowState {
    Initialized, FilesReady, ChannelAnnouncementLogLoading, ChannelAnnouncementLogLoaded, ChannelUpdateLogLoading, ChannelUpdateLogLoaded,
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {

    // init file requirements
    val files by remember {
        mutableStateOf(
            mutableMapOf<String, File?>(
                "payments_output.csv" to null,
                "channels_output.csv" to null,
                "edges_output.csv" to null,
                "nodes_output.csv" to null,
                "channel_announcement_log.csv" to null,
                "channel_update_log.csv" to null,
            )
        )
    }

    // inflate file path from property file
    try {
        inputFilePathProperty.load(FileInputStream(inputFilePathPropertyFile))
        files.forEach { (filename, _) ->
            if(inputFilePathProperty.getProperty(filename) != null){
                files[filename] = File(inputFilePathProperty.getProperty(filename)!!)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    println(mainWindowState.value)
    when (mainWindowState.value) {
        MainWindowState.Initialized -> {
            Window(onCloseRequest = {}, title = "file load test") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                files.forEach { (_, file) ->
                                    if (file == null) {
                                        return@IconButton
                                    }
                                }

                                files.forEach { (_, file) ->
                                    inputFilePathProperty.setProperty(file!!.name, file.path)
                                }
                                inputFilePathProperty.store(FileOutputStream(inputFilePathPropertyFile), null)

                                mainWindowState.value = MainWindowState.FilesReady
                            }
                        ) {
                            Text("Next > ")
                        }
                    }
                    MultipleFileLoadComponent(files, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        MainWindowState.FilesReady -> {
            if (mainWindowState.value != MainWindowState.ChannelAnnouncementLogLoading) {
                mainWindowState.value = MainWindowState.ChannelAnnouncementLogLoading
                channelAnnouncementAnalyzer.load(
                    logFile = files["channel_announcement_log.csv"]!!,
                    onLoadCompleted = {
                        mainWindowState.value = MainWindowState.ChannelAnnouncementLogLoaded
                    }
                )
            }
        }

        MainWindowState.ChannelAnnouncementLogLoading -> {

        }

        MainWindowState.ChannelAnnouncementLogLoaded -> {
            if (mainWindowState.value != MainWindowState.ChannelUpdateLogLoading) {
                mainWindowState.value = MainWindowState.ChannelUpdateLogLoading
                channelUpdateAnalyzer.load(
                    logFile = files["channel_update_log.csv"]!!,
                    onLoadCompleted = {
                        mainWindowState.value = MainWindowState.ChannelUpdateLogLoaded
                    }
                )
            }
        }

        MainWindowState.ChannelUpdateLogLoading -> {

        }

        MainWindowState.ChannelUpdateLogLoaded -> {
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

    }


    CSVAnalyzerWindow(
        windowTitle = "PaymentsOutputAnalyzer",
        analyzer = paymentsOutputAnalyzer,
        layoutOnAnalyzeCompleted = {

        },
    )

    CSVAnalyzerWindow(
        "NodeList",
        channelAnnouncementAnalyzer,
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
    )

    CSVAnalyzerWindow(
        "ChannelList",
        channelUpdateAnalyzer,
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
    )
}