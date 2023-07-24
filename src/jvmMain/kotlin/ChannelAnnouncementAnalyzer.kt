import androidx.compose.runtime.*
import gossip_msg.ChannelAnnouncement
import java.io.File

class ChannelAnnouncementAnalyzer : CSVAnalyzer() {

    var nodeListForDisplay = mutableStateOf<List<Node>?>(null)

    override fun analyzeCSVLine(lineText: String?) {
        if (lineText == null) return
        val csvElement = lineText.split(",")
        val channelAnnouncement = ChannelAnnouncement(
            csvElement[0],
            csvElement[1],
            csvElement[2],
            csvElement[3],
            csvElement[4],
            csvElement[5],
            csvElement[6],
            csvElement[7],
            csvElement[8],
            csvElement[9],
            csvElement[10],
        )

        val channel = channels[channelAnnouncement.shortChannelId]

        if (nodes[channelAnnouncement.nodeId1] != null && channel != null) {
            nodes[channelAnnouncement.nodeId1]!!.channels.add(channel)
            channel.node1 = nodes[channelAnnouncement.nodeId1]!!
        } else {
            nodes[channelAnnouncement.nodeId1] = Node(channelAnnouncement.nodeId1).apply {
                if (channel != null) {
                    channels.add(channel)
                    channel.node1 = this
                }
            }
        }

        if (nodes[channelAnnouncement.nodeId2] != null && channel != null) {
            nodes[channelAnnouncement.nodeId2]!!.channels.add(channel)
            channel.node2 = nodes[channelAnnouncement.nodeId2]!!
        } else {
            nodes[channelAnnouncement.nodeId2] = Node(channelAnnouncement.nodeId2).apply {
                if (channel != null) {
                    channels.add(channel)
                    channel.node2 = this
                }
            }
        }
    }

    override fun onAnalyzingFinished() {
        val list = mutableListOf<Node>()
        nodes.toList().sortedByDescending { it.second.channels.size }.forEach {
            list.add(it.second)
        }
        nodeListForDisplay.value = list
    }

    override fun onLogFileLoaded(logFile: File): String? {
        return if (channelUpdateAnalyzer.state.value != AnalyzerWindowState.Analyzed) {
            "Analyze channel_update log first."
        } else if (!logFile.name.startsWith("channel_announcement")) {
            "This file is not a channel_announcement log."
        } else {
            null
        }
    }

    override fun reset() {
        nodes.clear()
        nodeListForDisplay.value = null
    }
}