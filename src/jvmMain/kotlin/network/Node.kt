package network

import ui.SelectableListComponent
import Topology
import TopologyComponent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.uci.ics.jung.layout.algorithms.FRLayoutAlgorithm
import java.awt.Dimension

class Node(val id: String, val network: Network) {
    val channels = mutableSetOf<Channel>()

    override fun equals(other: Any?): Boolean {
        return other is Node && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        val channelIds = mutableListOf<String>()
        channels.forEach {
            channelIds += it.shortChannelId
        }
        return "Node(id='$id', channels=$channelIds)"
    }
}

@Composable
fun NodeDetailComponent(node: Node) {
    Column {

        TopologyComponent(
            Topology(
                Dimension(1920, 1080),
                5,
                100,
                FRLayoutAlgorithm(),
                node,
                1
            ),
            modifier = Modifier.weight(1f),
        )

        Divider()

        SelectableListComponent(
            modifier = Modifier.weight(1f),
            listDataForDisplay = node.channels.toList(),
            detailWindowTitle = { "Channel ${it?.shortChannelId}" },
            detailWindowLayout = {
                if (it != null) {
                    ChannelDetailComponent(it)
                }
            },
            listItemLayout = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(it.shortChannelId, modifier = Modifier.weight(2f))
                    Divider(modifier = Modifier.width(3.dp).fillMaxHeight())
                    Text(it.node1.id, modifier = Modifier.weight(5f))
                    Divider(modifier = Modifier.weight(1f).height(3.dp))
                    Text(it.node2.id, modifier = Modifier.weight(5f))
                }
            },
            listTitle = "Channels",
            fetchLatestDetail = {
                node.network.channels[it.shortChannelId]
            },
            clipboardText = {
                it.shortChannelId
            },
            findByText = {
                node.network.channels[it]
            }
        )
    }
}