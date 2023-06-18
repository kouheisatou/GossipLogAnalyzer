import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.application


val gossipAnalyzer = ChannelAnalyzer()
val nodeAnalyzer = NodeAnalyzer()

val channels = ChannelHashSet()
val nodes = NodeHashSet()

@OptIn(ExperimentalMaterialApi::class)
fun main() = application {
    if (gossipAnalyzer.state.value == AnalyzerWindowState.Analyzed) {

        CSVAnalyzerWindow(
            "NodeList",
            nodeAnalyzer,
            "Drop channel_announcement log file here!",
            nodeAnalyzer.nodeListForDisplay.value ?: listOf(),
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
                nodes.findByNodeId(it.id)
            },
            selectedItem = nodeAnalyzer.selectedNode,
            findById = {
                nodes.findByNodeId(it)
            },
            clipboardText = {
                it?.id
            }
        )
    }

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
        selectedItem = gossipAnalyzer.selectedChannel,
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
            channels.findChannelById(it.shortChannelId)
        },
        findById = {
            channels.findChannelById(it)
        },
        clipboardText = {
            it?.shortChannelId
        }
    )
}