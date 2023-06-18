import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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


val gossipAnalyzer = GossipLogAnalyzer()
val topologyAnalyzer = TopologyAnalyzer()

val channels = ChannelHashSet()
val nodes = NodeHashSet()

fun main() = application {
    if (gossipAnalyzer.state.value == AnalyzerWindowState.Analyzed) {
        CSVAnalyzerWindow(
            "NodeList",
            topologyAnalyzer,
            "Drop channel_announcement log file here!",
            topologyAnalyzer.nodeListForDisplay.value ?: listOf(),
            detailWindowTitle = {
                it?.id ?: ""
            },
            detailWindowLayout = {
                LazyColumn {
                    items(it?.channels ?: listOf()) {
                        Text(it.toString())
                        Divider()
                    }
                }
            },
            listTopRowLayout = {
                Text("NodeID")
                Spacer(modifier = Modifier.weight(1f))
                Text("Channels")
            },
            listItemLayout = {
                Text(it.id)
                Spacer(modifier = Modifier.weight(1f))
                Text(it.channels.size.toString())
            }
        )
    }

    CSVAnalyzerWindow(
        "GossipLogAnalyzer",
        gossipAnalyzer,
        "Drop channel_update log file here!",
        listData = gossipAnalyzer.channelsForDisplay.value ?: listOf(),
        detailWindowTitle = { it?.shortChannelId.toString() },
        detailWindowLayout = { selected ->

            Column {

                val data = XYSeriesCollection()
                val htlcMaximumMsatSeries = XYSeries("htlcMaximumMsat", true)
                selected?.channelUpdates?.forEach {
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

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(selected?.channelUpdates ?: listOf()) {
                        Text(it.toString())
                        Divider()
                    }
                }
            }
        },
        listTopRowLayout = {
            Text("ChannelID")
            Spacer(modifier = Modifier.weight(1f))
            Text("Updates")
        },
        listItemLayout = {
            Text(it.shortChannelId)
            Spacer(modifier = Modifier.weight(1f))
            Text(it.channelUpdates.size.toString())
        }
    )
}