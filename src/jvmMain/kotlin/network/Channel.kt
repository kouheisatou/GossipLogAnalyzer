package network

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import ui.SelectableListComponent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import model.input_gossip_msg.ChannelUpdate
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYDataItem
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection

class Channel(
    val shortChannelId: String,
    val node1: Node,
    val node2: Node,
    val capacity: Long,
    val network: Network,
) {
    val edgeNode1ToNode2: Edge = Edge(this, Direction.Node1ToNode2)
    val edgeNode2ToNode1: Edge = Edge(this, Direction.Node2ToNode1)

    fun addChannelUpdate(channelUpdate: ChannelUpdate) {
        if (channelUpdate.direction == Direction.Node1ToNode2) {
            edgeNode1ToNode2
        } else {
            edgeNode2ToNode1
        }.channelUpdates.add(channelUpdate)
    }

    override fun equals(other: Any?): Boolean {
        return other is Channel && other.shortChannelId == this.shortChannelId
    }

    override fun hashCode(): Int {
        return shortChannelId.hashCode()
    }

    override fun toString(): String {
        return "Channel(shortChannelId='$shortChannelId', node1=${node1.id}, node2=${node2.id})"
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelDetailComponent(channel: Channel) {
    Column {
        val data = XYSeriesCollection()
        val node1ToNode2Series = XYSeries("node1 to node2", true)
        val node2ToNode1Series = XYSeries("node2 to node1", true)
        channel.edgeNode1ToNode2.channelUpdates.forEach {
            node1ToNode2Series.add(XYDataItem(it.timestamp, it.htlcMaximumMsat))
        }
        channel.edgeNode2ToNode1.channelUpdates.forEach {
            node2ToNode1Series.add(XYDataItem(it.timestamp, it.htlcMaximumMsat))
        }
        data.addSeries(node1ToNode2Series)
        data.addSeries(node2ToNode1Series)
        val chart = ChartFactory.createScatterPlot(
            null,
            "timestamp[ms]",
            "htlcMaximumMsat[msat]",
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

        var showNode1Window by remember { mutableStateOf(false) }
        var showNode2Window by remember { mutableStateOf(false) }
        Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text("Node1\n${channel.node1.id}", modifier = Modifier.weight(1f).clickable { showNode1Window = true })

            Column(modifier = Modifier.weight(3f), horizontalAlignment = Alignment.CenterHorizontally) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(channel.edgeNode1ToNode2.channelUpdates) {
                        Text(it.toString())
                        Divider()
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Divider(modifier = Modifier.height(1.dp).weight(1f), color = Color.Black)
                    Text("edgeCapacity=${channel.edgeNode1ToNode2.capacity}[msat]")
                    Divider(modifier = Modifier.height(1.dp).weight(1f), color = Color.Black)
                    Text(">")
                }
                Text("channelCapacity=${channel.capacity}[msat]")
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("<")
                    Divider(modifier = Modifier.height(1.dp).weight(1f), color = Color.Black)
                    Text("edgeCapacity=${channel.edgeNode2ToNode1.capacity}[msat]")
                    Divider(modifier = Modifier.height(1.dp).weight(1f), color = Color.Black)
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(channel.edgeNode2ToNode1.channelUpdates) {
                        Text(it.toString())
                        Divider()
                    }
                }
            }

            Text("Node2\n${channel.node2.id}", modifier = Modifier.weight(1f).clickable { showNode2Window = true })
        }

        if (showNode1Window) {
            Window(
                onCloseRequest = {
                    showNode1Window = false
                },
                title = "Node ${channel.node1.id}",
                onKeyEvent = {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        showNode1Window = false
                    }
                    false
                }
            ) {
                NodeDetailComponent(channel.node1)
            }
        }

        if (showNode2Window) {
            Window(
                onCloseRequest = {
                    showNode2Window = false
                },
                title = "Node ${channel.node2.id}",
                onKeyEvent = {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        showNode2Window = false
                    }
                    false
                }
            ) {
                NodeDetailComponent(channel.node2)
            }
        }
    }
}