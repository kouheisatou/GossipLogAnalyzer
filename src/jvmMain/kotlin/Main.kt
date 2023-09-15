import analyzer.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.IconButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import edu.uci.ics.jung.layout.algorithms.StaticLayoutAlgorithm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.*
import ui.FilePicker
import ui.MultipleFileLoadComponent
import ui.SelectableListComponent
import java.awt.Color
import java.awt.Dimension
import java.awt.FileDialog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties


val estimatedNetwork = mutableStateOf<Network?>(null)
val groundTruthNetwork = mutableStateOf<Network?>(null)

val inputFilePathPropertyFile = File("input_files.properties")
val inputFilePathProperty = Properties()
val groundTruthFilePathPropertyFile = File("ground_truth_files.properties")
val groundTruthFilePathProperty = Properties()

fun main() = application {
    var showGroundTruthWindows by remember { mutableStateOf(false) }

    var nodesListForDisplay by remember { mutableStateOf<List<Node>?>(null) }
    var channelsListForDisplay by remember { mutableStateOf<List<Channel>?>(null) }
    if (estimatedNetwork.value == null) {

        val filesForEstimation by remember {
            mutableStateOf(
                mutableMapOf<String, File?>(
                    "describegraph.json" to null,
                    "channel_announcement_log.csv" to null,
                    "channel_update_log.csv" to null,
                )
            )
        }
        try {
            inputFilePathProperty.load(FileInputStream(inputFilePathPropertyFile))
            filesForEstimation.forEach { (filename, _) ->
                if (inputFilePathProperty.getProperty(filename) != null) {
                    filesForEstimation[filename] = File(inputFilePathProperty.getProperty(filename)!!)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Window(onCloseRequest = {}, title = "required files") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Compare with ground truth",
                    modifier = Modifier.clickable { showGroundTruthWindows = true },
                    textDecoration = TextDecoration.Underline,
                )
                Row {
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

                            CoroutineScope(Dispatchers.IO).launch {

                                val network = genNetworkFromLNDOutputs(
                                    filesForEstimation["describegraph.json"]!!,
                                    filesForEstimation["channel_announcement_log.csv"]!!,
                                    filesForEstimation["channel_update_log.csv"]!!,
                                )

                                channelsListForDisplay = network.channels
                                    .toList()
                                    .sortedByDescending { it.second.edgeNode1ToNode2.channelUpdates.size + it.second.edgeNode2ToNode1.channelUpdates.size }
                                    .let { list ->
                                        val result = mutableListOf<Channel>()
                                        list.forEach {
                                            result.add(it.second)
                                        }
                                        result
                                    }

                                nodesListForDisplay =
                                    network.nodes
                                        .toList()
                                        .sortedByDescending { network.demand[it.second] ?: 0L }
                                        .let { list ->
                                            val result = mutableListOf<Node>()
                                            list.forEach {
                                                result.add(it.second)
                                            }
                                            result
                                        }

                                estimatedNetwork.value = network
                            }
                        }
                    ) {
                        Text("Next > ")
                    }
                }
                MultipleFileLoadComponent(filesForEstimation, modifier = Modifier.fillMaxWidth())
            }
        }
    } else {

        Window(
            onCloseRequest = {},
            title = "estimated topology"
        ) {
            val topology by remember {
                mutableStateOf(
                    Topology(
                        Dimension(19200, 10800),
                        30,
                        100,
                        StaticLayoutAlgorithm(),
                        estimatedNetwork.value!!,
                    )
                )
            }
            TopologyComponent(topology)
        }

        Window(
            onCloseRequest = {},
            title = "Nodes in Estimated Network",
        ) {
            SelectableListComponent(
                nodesListForDisplay ?: listOf(),
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
                        Text("Demand")
                    }
                },
                listItemLayout = { node: Node ->
                    Row {
                        Text(node.id)
                        Spacer(modifier = Modifier.weight(1f))
                        Text((estimatedNetwork.value?.demand?.get(node) ?: 0L).toString())
                    }
                },
                fetchLatestDetail = {
                    estimatedNetwork.value!!.nodes[it.id]
                },
                findByText = {
                    estimatedNetwork.value!!.nodes[it]
                },
                clipboardText = {
                    it.id
                },
            )
        }

        Window(
            onCloseRequest = {},
            title = "Channels in Estimated Network",
        ) {
            SelectableListComponent(
                listDataForDisplay = channelsListForDisplay ?: listOf(),
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
                    estimatedNetwork.value!!.channels[it.shortChannelId]
                },
                findByText = {
                    estimatedNetwork.value!!.channels[it]
                },
                clipboardText = {
                    it.shortChannelId
                },
            )
        }
    }

    // ↓ ground truth windows
    if (showGroundTruthWindows) {
        var groundTruthNodesListForDisplay by remember { mutableStateOf<List<Node>?>(null) }
        var groundTruthChannelsListForDisplay by remember { mutableStateOf<List<Channel>?>(null) }

        if (groundTruthNetwork.value == null) {
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
            try {
                groundTruthFilePathProperty.load(FileInputStream(groundTruthFilePathPropertyFile))
                filesForGroundTruth.forEach { (filename, _) ->
                    if (groundTruthFilePathProperty.getProperty(filename) != null) {
                        filesForGroundTruth[filename] = File(groundTruthFilePathProperty.getProperty(filename)!!)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Window(
                onCloseRequest = {
                    showGroundTruthWindows = false
                },
                title = "required files",
            ) {
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

                                CoroutineScope(Dispatchers.IO).launch {
                                    val network = genGroundTruthNetworkFromSimulatorOutput(
                                        filesForGroundTruth["edges_output.csv"]!!,
                                        filesForGroundTruth["nodes_output.csv"]!!,
                                        filesForGroundTruth["channels_output.csv"]!!,
                                        filesForGroundTruth["payments_output.csv"]!!,
                                    )

                                    groundTruthChannelsListForDisplay = network.channels
                                        .toList()
                                        .sortedByDescending { it.second.edgeNode1ToNode2.channelUpdates.size + it.second.edgeNode2ToNode1.channelUpdates.size }
                                        .let { list ->
                                            val result = mutableListOf<Channel>()
                                            list.forEach {
                                                result.add(it.second)
                                            }
                                            result
                                        }

                                    groundTruthNodesListForDisplay = network.nodes
                                        .toList()
                                        .sortedByDescending { network.demand[it.second] ?: 0L }
                                        .let { list ->
                                            val result = mutableListOf<Node>()
                                            list.forEach {
                                                result.add(it.second)
                                            }
                                            result
                                        }

                                    groundTruthNetwork.value = network
                                }
                            }
                        ) {
                            Text("Next > ")
                        }
                    }
                    MultipleFileLoadComponent(filesForGroundTruth, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            Window(
                onCloseRequest = {},
                title = "Ground Truth Topology"
            ) {
                val topology by remember {
                    mutableStateOf(
                        Topology(
                            Dimension(19200, 10800),
                            30,
                            100,
                            StaticLayoutAlgorithm(),
                            groundTruthNetwork.value!!,
                        )
                    )
                }
                TopologyComponent(topology)
            }

            Window(
                onCloseRequest = {},
                title = "Nodes in Ground Truth Network",
            ) {
                SelectableListComponent(
                    groundTruthNodesListForDisplay ?: listOf(),
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
                            Text("Demand")
                        }
                    },
                    listItemLayout = { node: Node ->
                        Row {
                            Text(node.id)
                            Spacer(modifier = Modifier.weight(1f))
                            Text((groundTruthNetwork.value?.demand?.get(node) ?: 0L).toString())
                        }
                    },
                    fetchLatestDetail = {
                        groundTruthNetwork.value!!.nodes[it.id]
                    },
                    findByText = {
                        groundTruthNetwork.value!!.nodes[it]
                    },
                    clipboardText = {
                        it.id
                    },
                )
            }

            Window(
                onCloseRequest = {},
                title = "Channels in Ground Truth Network",
            ) {
                SelectableListComponent(
                    listDataForDisplay = groundTruthChannelsListForDisplay ?: listOf(),
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
                        groundTruthNetwork.value!!.channels[it.shortChannelId]
                    },
                    findByText = {
                        groundTruthNetwork.value!!.channels[it]
                    },
                    clipboardText = {
                        it.shortChannelId
                    },
                )
            }
        }
    }
    // ↑ ground truth windows
}