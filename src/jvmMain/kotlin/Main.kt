import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.application


val gossipAnalyzer = GossipLogAnalyzer()
val topologyAnalyzer = TopologyAnalyzer()

val channels = ChannelHashSet()
val nodes = NodeHashSet()

@OptIn(ExperimentalMaterialApi::class)
fun main() = application {
    if (gossipAnalyzer.state.value == AnalyzerWindowState.Analyzed) {

        val selectedNode = mutableStateOf<Node?>(null)
        CSVAnalyzerWindow(
            "NodeList",
            topologyAnalyzer,
            "Drop channel_announcement log file here!",
            topologyAnalyzer.nodeListForDisplay.value ?: listOf(),
            detailWindowTitle = { "Node ${it?.id}" },
            detailWindowLayout = {
                if (it != null) {
                    NodeDetailComponent(it)
                }
            },
            listTopRowLayout = {
                Column {

                    var nodeId by remember { mutableStateOf("") }
                    var showDialog by remember { mutableStateOf(false) }
                    var result by remember { mutableStateOf<Node?>(null) }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = nodeId,
                                onValueChange = {
                                    nodeId = it
                                }
                            )
                            IconButton(
                                onClick = {
                                    result = nodes.find(Node(nodeId))
                                    showDialog = true
                                }
                            ) {
                                Text("🔍")
                            }
                        }
                    }
                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            buttons = {
                                Row {
                                    TextButton(onClick = { showDialog = false }) { Text("close") }
                                    if (result != null) {
                                        TextButton(onClick = {
                                            selectedNode.value = result
                                            showDialog = false
                                        }) {
                                            Text("open detail")
                                        }
                                    }
                                }
                            },
                            text = { Text(result?.id.toString()) }
                        )
                    }

                    Row {
                        Text("NodeID")
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Channels")
                    }
                }
            },
            listItemLayout = {
                Text(it.id)
                Spacer(modifier = Modifier.weight(1f))
                Text(it.channels.size.toString())
            },
            onItemSelected = {
                println(it)
            }
        )
    }

    val selectedChannel = mutableStateOf<Channel?>(null)
    CSVAnalyzerWindow(
        "GossipLogAnalyzer",
        gossipAnalyzer,
        "Drop channel_update log file here!",
        listData = gossipAnalyzer.channelsForDisplay.value ?: listOf(),
        detailWindowTitle = { "Channel ${it?.shortChannelId}" },
        detailWindowLayout = { selected ->
            if (selected != null) {
                ChannelDetailComponent(selected)
            }
        },
        selectedItem = selectedChannel,
        listTopRowLayout = {
            Column {

                var channelId by remember { mutableStateOf("") }
                var showDialog by remember { mutableStateOf(false) }
                var result by remember { mutableStateOf<Channel?>(null) }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = channelId,
                            onValueChange = {
                                channelId = it
                            }
                        )
                        IconButton(
                            onClick = {
                                result = channels.findChannelById(channelId)
                                showDialog = true
                            }
                        ) {
                            Text("🔍")
                        }
                    }
                }
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        buttons = {
                            Row {
                                TextButton(onClick = { showDialog = false }) { Text("close") }
                                if (result != null) {
                                    TextButton(onClick = {
                                        selectedChannel.value = result
                                        showDialog = false
                                    }) {
                                        Text("open detail")
                                    }
                                }
                            }
                        },
                        text = { Text(result?.shortChannelId.toString()) }
                    )
                }

                Row {
                    Text("ChannelID")
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Updates")
                }
            }
        },
        listItemLayout = {
            Text(it.shortChannelId)
            Spacer(modifier = Modifier.weight(1f))
            Text(it.channelUpdates.size.toString())
        },
        onItemSelected = {
            println(it)
        }
    )
}