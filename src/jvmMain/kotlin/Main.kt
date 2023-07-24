import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import edu.uci.ics.jung.layout.algorithms.StaticLayoutAlgorithm
import java.awt.Dimension
import java.io.File


val channelUpdateAnalyzer = ChannelUpdateAnalyzer()
val channelAnnouncementAnalyzer = ChannelAnnouncementAnalyzer()

val channels = mutableMapOf<String, Channel>()
val nodes = mutableMapOf<String, Node>()

fun main() = application {

    if (channelUpdateAnalyzer.state.value == AnalyzerWindowState.Analyzed && channelAnnouncementAnalyzer.state.value == AnalyzerWindowState.Analyzed) {
        Window(
            onCloseRequest = {},
            title = "topology"
        ) {
            val topology by remember {
                mutableStateOf(
                    Topology(
                        Dimension(19200, 10800),
                        30,
                        StaticLayoutAlgorithm(),
                        channels
                    )
                )
            }
            TopologyComponent(topology)
        }
    }

    if (channelUpdateAnalyzer.state.value == AnalyzerWindowState.Analyzed) {

        CSVAnalyzerWindow(
            "NodeList",
            channelAnnouncementAnalyzer,
            "Drop channel_announcement log file here!",
            layoutOnAnalyzeCompleted = {
                SelectableListComponent(
                    channelAnnouncementAnalyzer.nodeListForDisplay.value ?: listOf(),
                    detailWindowTitle = { "Node ${it?.id}" },
                    detailWindowLayout = {
                        if (it != null) {
                            NodeDetailComponent(it)
                        }
                    },
                    listTopRowLayout = {
                        Row {
                            Text("NodeID")
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Channels")
                        }
                    },
                    listItemLayout = { node: Node ->
                        Row {
                            Text(node.id)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(node.channels.size.toString())
                        }
                    },
                    fetchLatestDetail = {
                        nodes[it.id]
                    },
                    findByText = {
                        nodes[it]
                    },
                    clipboardText = {
                        it.id
                    },
                )
            },
            onWindowInitialized = {
                val sampleLogFile = File("sample_channel_announcement_log.csv")
                if (sampleLogFile.exists()) {
                    it.load(sampleLogFile) { readingLine, progress ->
                        it.progress.value = progress
                        it.readingLine.value = readingLine ?: ""
                    }
                }
            }
        )
    }
    CSVAnalyzerWindow(
        "ChannelList",
        channelUpdateAnalyzer,
        "Drop channel_update log file here!",
        layoutOnAnalyzeCompleted = {
            SelectableListComponent(
                listDataForDisplay = channelUpdateAnalyzer.channelsForDisplay.value ?: listOf(),
                detailWindowTitle = { "Channel ${it?.shortChannelId}" },
                detailWindowLayout = { selected ->
                    if (selected != null) {
                        ChannelDetailComponent(selected)
                    }
                },
                listTopRowLayout = {
                    Row {
                        Text("ChannelID")
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Updates")
                    }
                },
                listItemLayout = { channel: Channel ->
                    Row {
                        Text(channel.shortChannelId)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(channel.channelUpdates.size.toString())
                    }
                },
                fetchLatestDetail = {
                    channels[it.shortChannelId]
                },
                findByText = {
                    channels[it]
                },
                clipboardText = {
                    it.shortChannelId
                },
            )
        },
        onWindowInitialized = {
            val sampleLogFile = File("sample_channel_update_log.csv")
            if (sampleLogFile.exists()) {
                it.load(sampleLogFile) { readingLine, progress ->
                    it.progress.value = progress
                    it.readingLine.value = readingLine ?: ""
                }
            }
        }
    )
}