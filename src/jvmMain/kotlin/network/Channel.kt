package network

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
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
            "htlcMaximumMsat[BTC]",
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

        SelectableListComponent(
            listTitle = "Nodes",
            modifier = Modifier.fillMaxWidth().weight(1f),
            listDataForDisplay = listOf(channel.node1, channel.node2),
            detailWindowTitle = { "Node ${it?.id}" },
            detailWindowLayout = {
                if (it != null) {
                    NodeDetailComponent(it)
                }
            },
            listItemLayout = {
                Column {
                    if (it == channel.node1) {
                        var showChannelUpdate by remember { mutableStateOf(false) }
                        Row {
                            Text("Node1 : ${channel.node1.id}", color = Color.Red, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                showChannelUpdate = !showChannelUpdate
                            }) {
                                Text(
                                    if (showChannelUpdate) {
                                        "^"
                                    } else {
                                        "v"
                                    }
                                )
                            }
                        }
                        if (showChannelUpdate) {
                            Text("Channel Updates of ${channel.node1.id}(Node1) to ${channel.node2.id}(Node2)")
                            Divider()
                            Column {
                                channel.edgeNode1ToNode2.channelUpdates.forEach {
                                    Text(it.toString())
                                }
                                Divider()
                            }
                        }
                    } else {
                        var showChannelUpdate by remember { mutableStateOf(false) }
                        Row {
                            Text("Node2 : ${channel.node2.id}", color = Color.Blue, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                showChannelUpdate = !showChannelUpdate
                            }) {
                                Text(
                                    if (showChannelUpdate) {
                                        "^"
                                    } else {
                                        "v"
                                    }
                                )
                            }
                        }
                        if (showChannelUpdate) {
                            Text("Channel Updates of ${channel.node2.id}(Node2) to ${channel.node1.id}(Node1)")
                            Divider()
                            Column {
                                channel.edgeNode1ToNode2.channelUpdates.forEach {
                                    Text(it.toString())
                                }
                                Divider()
                            }
                        }
                    }
                }
            },
            fetchLatestDetail = {
                channel.network.nodes[it.id]
            },
            clipboardText = {
                it.id
            },
        )
    }
}