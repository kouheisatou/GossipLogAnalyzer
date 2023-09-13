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
import network.*
import ui.FilePicker
import ui.MultipleFileLoadComponent
import ui.SelectableListComponent
import java.awt.Dimension
import java.awt.FileDialog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties


val estimatedNetwork = Network()
val channelUpdateAnalyzer = ChannelUpdateAnalyzer(estimatedNetwork)
val channelAnnouncementAnalyzer = ChannelAnnouncementAnalyzer(estimatedNetwork)

val groundTruthNetwork = Network()
val paymentsOutputAnalyzer = PaymentsOutputAnalyzer(groundTruthNetwork)
val nodesOutputAnalyzer = NodesOutputAnalyzer(groundTruthNetwork)
val edgesOutputAnalyzer = EdgesOutputAnalyzer(groundTruthNetwork)
val channelsOutputAnalyzer = ChannelsOutputAnalyzer(groundTruthNetwork)


val inputFilePathPropertyFile = File("input_files.properties")
val inputFilePathProperty = Properties()
val groundTruthFilePathPropertyFile = File("ground_truth_files.properties")
val groundTruthFilePathProperty = Properties()

val estimationWindowState = mutableStateOf(EstimationWindowState.Initialized)
val groundTruthWindowState = mutableStateOf(GroundTruthWindowState.Initialized)

enum class EstimationWindowState {
    Initialized, FilesReady, ChannelAnnouncementLogLoading, ChannelAnnouncementLogLoaded, ChannelUpdateLogLoading, ChannelUpdateLogLoaded,
}

enum class GroundTruthWindowState {
    Initialized, FilesReady, NodesOutputLoading, NodesOutputLoaded, ChannelOutputLoading, ChannelOutputLoaded, EdgesOutputLoading, EdgesOutputLoaded, PaymentOutputLoading, PaymentsOutputLoaded,
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {

    // init file requirements
    val filesForEstimation by remember {
        mutableStateOf(
            mutableMapOf<String, File?>(
                "channel_announcement_log.csv" to null,
                "channel_update_log.csv" to null,
            )
        )
    }
    val filesForGroundTruth by remember {
        mutableStateOf(
            mutableMapOf<String, File?>(
                "payments_output.csv" to null,
                "channels_output.csv" to null,
                "edges_output.csv" to null,
                "nodes_output.csv" to null,
            )
        )
    }

    // inflate file path from property file
    try {
        inputFilePathProperty.load(FileInputStream(inputFilePathPropertyFile))
        filesForEstimation.forEach { (filename, _) ->
            if (inputFilePathProperty.getProperty(filename) != null) {
                filesForEstimation[filename] = File(inputFilePathProperty.getProperty(filename)!!)
            }
        }
        groundTruthFilePathProperty.load(FileInputStream(groundTruthFilePathPropertyFile))
        filesForGroundTruth.forEach { (filename, _) ->
            if (groundTruthFilePathProperty.getProperty(filename) != null) {
                filesForGroundTruth[filename] = File(groundTruthFilePathProperty.getProperty(filename)!!)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // ↓ estimation windows
    println(estimationWindowState.value)
    when (estimationWindowState.value) {
        EstimationWindowState.Initialized -> {
            Window(onCloseRequest = {}, title = "required files") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                filesForEstimation.forEach { (_, file) ->
                                    if (file == null) {
                                        return@IconButton
                                    }
                                }

                                filesForEstimation.forEach { (_, file) ->
                                    inputFilePathProperty.setProperty(file!!.name, file.path)
                                }
                                inputFilePathProperty.store(FileOutputStream(inputFilePathPropertyFile), null)

                                estimationWindowState.value = EstimationWindowState.FilesReady
                            }
                        ) {
                            Text("Next > ")
                        }
                    }
                    MultipleFileLoadComponent(filesForEstimation, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        EstimationWindowState.FilesReady -> {
            if (estimationWindowState.value != EstimationWindowState.ChannelAnnouncementLogLoading) {
                estimationWindowState.value = EstimationWindowState.ChannelAnnouncementLogLoading
                try {
                    channelAnnouncementAnalyzer.load(
                        logFile = filesForEstimation["channel_announcement_log.csv"]!!,
                        onLoadCompleted = {
                            estimationWindowState.value = EstimationWindowState.ChannelAnnouncementLogLoaded
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    estimationWindowState.value = EstimationWindowState.Initialized
                }
            }
        }

        EstimationWindowState.ChannelAnnouncementLogLoading -> {}

        EstimationWindowState.ChannelAnnouncementLogLoaded -> {
            if (estimationWindowState.value != EstimationWindowState.ChannelUpdateLogLoading) {
                estimationWindowState.value = EstimationWindowState.ChannelUpdateLogLoading
                try {
                    channelUpdateAnalyzer.load(
                        logFile = filesForEstimation["channel_update_log.csv"]!!,
                        onLoadCompleted = {
                            estimationWindowState.value = EstimationWindowState.ChannelUpdateLogLoaded
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    estimationWindowState.value = EstimationWindowState.Initialized
                }
            }
        }

        EstimationWindowState.ChannelUpdateLogLoading -> {}

        EstimationWindowState.ChannelUpdateLogLoaded -> {
            Window(
                onCloseRequest = {},
                title = "estimated topology"
            ) {
                val topology by remember {
                    mutableStateOf(
                        Topology(
                            Dimension(19200, 10800),
                            30,
                            StaticLayoutAlgorithm(),
                            estimatedNetwork,
                        )
                    )
                }
                TopologyComponent(topology)


                var isFileDialogOpened by remember { mutableStateOf(false) }
                if (isFileDialogOpened) {
                    FilePicker(
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
        "Nodes in Estimated Network",
        channelAnnouncementAnalyzer,
        layoutOnAnalyzeCompleted = {
            SelectableListComponent(
                channelAnnouncementAnalyzer.nodesForDisplay.value ?: listOf(),
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
                    estimatedNetwork.nodes[it.id]
                },
                findByText = {
                    estimatedNetwork.nodes[it]
                },
                clipboardText = {
                    it.id
                },
            )
        },
    )

    CSVAnalyzerWindow(
        "Channels in Estimated Network",
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
                        Text("${channel.edgeNode1ToNode2.channelUpdates.size}|${channel.edgeNode2ToNode1.channelUpdates.size}")
                    }
                },
                fetchLatestDetail = {
                    estimatedNetwork.channels[it.shortChannelId]
                },
                findByText = {
                    estimatedNetwork.channels[it]
                },
                clipboardText = {
                    it.shortChannelId
                },
            )
        },
    )
    // ↑ estimation windows

    // ↓ ground truth windows
    println(groundTruthWindowState.value)
    when (groundTruthWindowState.value) {
        GroundTruthWindowState.Initialized -> {

            Window(onCloseRequest = {}, title = "required files") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                filesForGroundTruth.forEach { (_, file) ->
                                    if (file == null) {
                                        return@IconButton
                                    }
                                }

                                filesForGroundTruth.forEach { (_, file) ->
                                    groundTruthFilePathProperty.setProperty(file!!.name, file.path)
                                }
                                groundTruthFilePathProperty.store(
                                    FileOutputStream(groundTruthFilePathPropertyFile),
                                    null
                                )

                                groundTruthWindowState.value = GroundTruthWindowState.FilesReady
                            }
                        ) {
                            Text("Next > ")
                        }
                    }
                    MultipleFileLoadComponent(filesForGroundTruth, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        GroundTruthWindowState.FilesReady -> {

            if (groundTruthWindowState.value != GroundTruthWindowState.EdgesOutputLoading) {
                groundTruthWindowState.value = GroundTruthWindowState.EdgesOutputLoading
                try {
                    edgesOutputAnalyzer.load(
                        filesForGroundTruth["edges_output.csv"]!!,
                        onLoadCompleted = {
                            groundTruthWindowState.value = GroundTruthWindowState.EdgesOutputLoaded
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    groundTruthWindowState.value = GroundTruthWindowState.Initialized
                }
            }
        }

        GroundTruthWindowState.EdgesOutputLoading -> {}
        GroundTruthWindowState.EdgesOutputLoaded -> {
            if (groundTruthWindowState.value != GroundTruthWindowState.NodesOutputLoading) {
                groundTruthWindowState.value = GroundTruthWindowState.NodesOutputLoading
                try {
                    nodesOutputAnalyzer.load(
                        filesForGroundTruth["nodes_output.csv"]!!,
                        onLoadCompleted = {
                            groundTruthWindowState.value = GroundTruthWindowState.NodesOutputLoaded
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    groundTruthWindowState.value = GroundTruthWindowState.Initialized
                }
            }
        }

        GroundTruthWindowState.NodesOutputLoading -> {}
        GroundTruthWindowState.NodesOutputLoaded -> {

            if (groundTruthWindowState.value != GroundTruthWindowState.ChannelOutputLoading) {
                groundTruthWindowState.value = GroundTruthWindowState.ChannelOutputLoading
                try {
                    channelsOutputAnalyzer.load(
                        filesForGroundTruth["channels_output.csv"]!!,
                        onLoadCompleted = {
                            groundTruthWindowState.value = GroundTruthWindowState.ChannelOutputLoaded
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    groundTruthWindowState.value = GroundTruthWindowState.Initialized
                }
            }
        }

        GroundTruthWindowState.ChannelOutputLoading -> {}
        GroundTruthWindowState.ChannelOutputLoaded -> {

            if (groundTruthWindowState.value != GroundTruthWindowState.PaymentOutputLoading) {
                groundTruthWindowState.value = GroundTruthWindowState.PaymentOutputLoading
                try {
                    paymentsOutputAnalyzer.load(
                        filesForGroundTruth["payments_output.csv"]!!,
                        onLoadCompleted = {
                            groundTruthWindowState.value = GroundTruthWindowState.PaymentsOutputLoaded
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    groundTruthWindowState.value = GroundTruthWindowState.Initialized
                }
            }
        }


        GroundTruthWindowState.PaymentOutputLoading -> {}
        GroundTruthWindowState.PaymentsOutputLoaded -> {
            Window(
                onCloseRequest = {},
                title = "ground truth network"
            ) {
                val topology by remember {
                    mutableStateOf(
                        Topology(
                            Dimension(19200, 10800),
                            30,
                            StaticLayoutAlgorithm(),
                            groundTruthNetwork,
                        )
                    )
                }
                TopologyComponent(topology)
            }
        }
    }

    CSVAnalyzerWindow(
        "Nodes in Ground Truth Network",
        nodesOutputAnalyzer,
        layoutOnAnalyzeCompleted = {
            SelectableListComponent(
                nodesOutputAnalyzer.nodesForDisplay.value ?: listOf(),
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
                    groundTruthNetwork.nodes[it.id]
                },
                findByText = {
                    groundTruthNetwork.nodes[it]
                },
                clipboardText = {
                    it.id
                },
            )
        },
    )

    CSVAnalyzerWindow(
        "Channels in Ground Truth Network",
        channelsOutputAnalyzer,
        layoutOnAnalyzeCompleted = {
            SelectableListComponent(
                listDataForDisplay = channelsOutputAnalyzer.channelsForDisplay.value ?: listOf(),
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
                        Text("${channel.edgeNode1ToNode2.channelUpdates.size}|${channel.edgeNode2ToNode1.channelUpdates.size}")
                    }
                },
                fetchLatestDetail = {
                    groundTruthNetwork.channels[it.shortChannelId]
                },
                findByText = {
                    groundTruthNetwork.channels[it]
                },
                clipboardText = {
                    it.shortChannelId
                },
            )
        },
    )
    // ↑ ground truth windows
}