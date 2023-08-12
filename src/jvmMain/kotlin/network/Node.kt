package network

import ui.SelectableListComponent
import Topology
import TopologyComponent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import channels
import edu.uci.ics.jung.layout.algorithms.FRLayoutAlgorithm
import nodes
import java.awt.Dimension

class Node(val id: String) {
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
                FRLayoutAlgorithm(),
                nodes,
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
                Row {
                    Text(it.shortChannelId)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(it.node1.id)
                    Text(" - ")
                    Text(it.node2.id)
                }
            },
            listTitle = "Channels",
            fetchLatestDetail = {
                channels[it.shortChannelId]
            },
            clipboardText = {
                it.shortChannelId
            },
            findByText = {
                channels[it]
            }
        )
    }
}