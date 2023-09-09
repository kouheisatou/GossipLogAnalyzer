package network

import ui.SelectableListComponent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import gossip_msg.ChannelUpdate
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
    val network: Network
) {
    val channelUpdates: MutableList<ChannelUpdate> = mutableListOf()

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

@Composable
fun ChannelDetailComponent(channel: Channel) {
    Column {
        val data = XYSeriesCollection()
        val node1ToNode2Series = XYSeries("node1 to node2", true)
        val node2ToNode1Series = XYSeries("node2 to node1", true)
        channel.channelUpdates.forEach {
            if (it.direction == Direction.Node1ToNode2) {
                node1ToNode2Series.add(XYDataItem(it.timestamp, it.htlcMaximumMsat))
            } else {
                node2ToNode1Series.add(XYDataItem(it.timestamp, it.htlcMaximumMsat))
            }
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

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            SelectableListComponent(
                listTitle = "Nodes",
                modifier = Modifier.weight(1f),
                listDataForDisplay = listOf(channel.node1, channel.node2),
                detailWindowTitle = { "Node ${it?.id}" },
                detailWindowLayout = {
                    if (it != null) {
                        NodeDetailComponent(it)
                    }
                },
                listItemLayout = {
                    Text(it.id)
                },
                fetchLatestDetail = {
                    channel.network.nodes[it.id]
                },
                clipboardText = {
                    it.id
                },
            )

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(channel.channelUpdates) {
                    Text(it.toString())
                    Divider()
                }
            }
        }
    }
}