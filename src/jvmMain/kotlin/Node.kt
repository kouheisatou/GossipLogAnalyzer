import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import edu.uci.ics.jung.layout.algorithms.FRLayoutAlgorithm
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
            Topology(Dimension(1920, 1080), 5, FRLayoutAlgorithm(), node, 2),
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
                Column {
                    Text(it.shortChannelId)
                    Row {
                        Text(it.node1?.id.toString())
                        Text(" - ", modifier = Modifier.weight(1f))
                        Text(it.node2?.id.toString())
                    }
                }
            },
            listTitle = "Channels",
            fetchLatestDetail = {
                channels.findChannelById(it.shortChannelId)
            },
            clipboardText = {
                node.id
            },
            findByText = {
                channels.findChannelById(it)
            }
        )
    }
}