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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Channel(
    val shortChannelId: String,
) {
    var node1: Node? = null
    var node2: Node? = null
    val channelUpdates: MutableList<ChannelUpdate> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        return other is Channel && other.shortChannelId == this.shortChannelId
    }

    override fun hashCode(): Int {
        return shortChannelId.hashCode()
    }

    override fun toString(): String {
        return "Channel(shortChannelId='$shortChannelId', node1=${node1?.id}, node2=${node2?.id})"
    }

}

@Composable
fun ChannelDetailComponent(channel: Channel) {
    Column {
        val data = XYSeriesCollection()
        val htlcMaximumMsatSeries = XYSeries("htlcMaximumMsat", true)
        channel.channelUpdates.forEach {
            val timeInt = LocalDateTime.parse(
                it.timestamp,
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            ).toEpochSecond(ZoneOffset.UTC)

            htlcMaximumMsatSeries.add(XYDataItem(timeInt, it.htlcMaximumMsat))
        }
        data.addSeries(htlcMaximumMsatSeries)
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
                    Text(it?.id.toString())
                },
                fetchLatestDetail = {
                    nodes.findByNodeId(it?.id)
                },
                clipboardText = {
                    channel.shortChannelId
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