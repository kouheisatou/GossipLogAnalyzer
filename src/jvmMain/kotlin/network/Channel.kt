package network

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
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
import movingAverage
import org.jfree.chart.*
import org.jfree.chart.entity.ChartEntity
import org.jfree.chart.entity.LegendItemEntity
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYDataItem
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class Channel(
    val shortChannelId: String,
    val node1: Node,
    val node2: Node,
    val capacity: Long,
    val network: Network,
) {
    val edgeNode1ToNode2: Edge = Edge(this, Direction.Node1ToNode2)
    val edgeNode2ToNode1: Edge = Edge(this, Direction.Node2ToNode1)

    var channelUpdateChart: JFreeChart? = null

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

    fun initChannelUpdateChart() {

        val data = XYSeriesCollection()

        val node1ToNode2MASeries = XYSeries("MA of Node1 to Node2", true)
        movingAverage(
            edgeNode1ToNode2.channelUpdates,
            horizontalAxisValue = { it.timestamp },
            verticalAxisValue = { it.htlcMaximumMsat },
            10000L
        ).forEach {
            node1ToNode2MASeries.add(it.first, it.second)
        }
        data.addSeries(node1ToNode2MASeries)

        val node2ToNode1MASeries = XYSeries("MA of Node2 to Node1", true)
        movingAverage(
            edgeNode2ToNode1.channelUpdates,
            horizontalAxisValue = { it.timestamp },
            verticalAxisValue = { it.htlcMaximumMsat },
            10000L
        ).forEach {
            node2ToNode1MASeries.add(it.first, it.second)
        }
        data.addSeries(node2ToNode1MASeries)

        val node1ToNode2Series = XYSeries("Node1 to Node2", true)
        edgeNode1ToNode2.channelUpdates.forEach {
            node1ToNode2Series.add(XYDataItem(it.timestamp, it.htlcMaximumMsat))
        }
        data.addSeries(node1ToNode2Series)

        val node2ToNode1Series = XYSeries("Node2 to Node1", true)
        edgeNode2ToNode1.channelUpdates.forEach {
            node2ToNode1Series.add(XYDataItem(it.timestamp, it.htlcMaximumMsat))
        }
        data.addSeries(node2ToNode1Series)

        val chart = ChartFactory.createScatterPlot(
            null,
            "timestamp[ms]",
            "htlc_maximum_msat[msat]",
            data,
        )
        (chart.plot as XYPlot).renderer = XYLineAndShapeRenderer().apply {
            setSeriesShapesVisible(0, false)
            setSeriesShapesVisible(1, false)
            setSeriesShapesVisible(2, true)
            setSeriesShapesVisible(3, true)
        }

        this.channelUpdateChart = chart
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelDetailComponent(channel: Channel) {
    val chartSeriesVisible by remember { mutableStateOf(mutableSetOf(0, 1, 2, 3)) }

    Column {
        val chartPane = ChartPanel(channel.channelUpdateChart).apply {
            addChartMouseListener(object : ChartMouseListener {
                override fun chartMouseClicked(event: ChartMouseEvent) {
                    if (event.entity is LegendItemEntity) {
                        val renderer = (channel.channelUpdateChart!!.plot as XYPlot).renderer as XYLineAndShapeRenderer
                        for (legendItemIndex in 0 until renderer.legendItems.itemCount) {
                            val legendItem = renderer.getLegendItem(0, legendItemIndex) as LegendItem
                            if (legendItem.seriesKey == (event.entity as LegendItemEntity).seriesKey) {
                                val visible = if (chartSeriesVisible.remove(legendItemIndex)) {
                                    false
                                } else {
                                    chartSeriesVisible.add(legendItemIndex)
                                    true
                                }
                                renderer.setSeriesLinesVisible(legendItemIndex, visible)
                                if (legendItemIndex >= 2) {
                                    renderer.setSeriesShapesVisible(legendItemIndex, visible)
                                }
                            }
                        }
                    }
                }

                override fun chartMouseMoved(event: ChartMouseEvent?) {}
            })
        }

        SwingPanel(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = {
                chartPane
            },
            update = {
                val renderer = (channel.channelUpdateChart!!.plot as XYPlot).renderer as XYLineAndShapeRenderer
                for (legendItemIndex in 0 until renderer.legendItems.itemCount) {
                    val visible = chartSeriesVisible.contains(legendItemIndex)
                    renderer.setSeriesLinesVisible(legendItemIndex, visible)
                    if (legendItemIndex >= 2) {
                        renderer.setSeriesShapesVisible(legendItemIndex, visible)
                    }
                }
                chartPane.chart = channel.channelUpdateChart
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