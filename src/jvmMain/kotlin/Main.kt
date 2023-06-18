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

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    if (gossipAnalyzer.state.value == AnalyzerWindowState.Analyzed) {
        CSVAnalyzerWindow(
            topologyAnalyzer,
            "Drop channel_announcement log file here!",
        ) {

            Column {
                Row {
                    Text("NodeID")
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Channels")
                }
                Divider(modifier = Modifier.fillMaxWidth())

                Row {
                    val listState = rememberLazyListState()
                    LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                        items(topologyAnalyzer.nodeListForDisplay ?: listOf()) {
                            Row {
                                Text(it.id)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(it.channels.size.toString())
                            }
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier.fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(listState),
                    )

                }
            }
        }
    }

    CSVAnalyzerWindow(
        gossipAnalyzer,
        "Drop channel_update log file here!",
    ) {

        var selectedChannel by remember { mutableStateOf<Channel?>(null) }

        Column {
            Row {
                Text("ChannelID")
                Spacer(modifier = Modifier.weight(1f))
                Text("Updates")
            }
            Divider(modifier = Modifier.fillMaxWidth())
            Row {
                val listState = rememberLazyListState()
                LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                    items(gossipAnalyzer.channelsForDisplay ?: listOf()) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    selectedChannel = it
                                }.background(
                                    if (it.shortChannelId == selectedChannel?.shortChannelId) {
                                        Color.LightGray
                                    } else {
                                        Color.White
                                    }
                                )
                        ) {
                            Text(it.shortChannelId)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(it.channelUpdates.size.toString())
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(listState),
                )
            }
        }
        if (selectedChannel != null) {

            Window(
                onCloseRequest = { selectedChannel = null },
                title = selectedChannel?.shortChannelId.toString(),
                onKeyEvent = {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key.keyCode) {
                            Key.Escape.keyCode -> {
                                selectedChannel = null
                            }

                            Key.DirectionDown.keyCode -> {
                                val index = gossipAnalyzer.channelsForDisplay?.indexOf(selectedChannel)?.plus(1)
                                if (index != null && gossipAnalyzer.channelsForDisplay != null) {
                                    if (index in 0 until gossipAnalyzer.channelsForDisplay!!.size) {
                                        selectedChannel = gossipAnalyzer.channelsForDisplay!![index]
                                    }
                                }
                            }

                            Key.DirectionUp.keyCode -> {
                                val index = gossipAnalyzer.channelsForDisplay?.indexOf(selectedChannel)?.minus(1)
                                if (index != null && gossipAnalyzer.channelsForDisplay != null) {
                                    if (index in 0 until gossipAnalyzer.channelsForDisplay!!.size) {
                                        selectedChannel = gossipAnalyzer.channelsForDisplay!![index]
                                    }
                                }
                            }
                        }
                    }
                    false
                }
            ) {

                Column {

                    val data = XYSeriesCollection()
                    val htlcMaximumMsatSeries = XYSeries("htlcMaximumMsat", true)
                    selectedChannel?.channelUpdates?.forEach {
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
                        items(selectedChannel?.channelUpdates ?: listOf()) {
                            Text(it.toString())
                        }
                    }
                }
            }
        }
    }
}