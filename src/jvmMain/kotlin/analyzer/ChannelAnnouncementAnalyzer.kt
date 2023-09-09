package analyzer

import network.Channel
import network.Node
import androidx.compose.runtime.*
import gossip_msg.ChannelAnnouncement
import network.Network
import java.io.File

class ChannelAnnouncementAnalyzer(private val estimatedNetwork: Network) : CSVAnalyzer() {

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

        val node1 = estimatedNetwork.nodes[channelAnnouncement.nodeId1] ?: Node(channelAnnouncement.nodeId1, estimatedNetwork)
        val node2 = estimatedNetwork.nodes[channelAnnouncement.nodeId2] ?: Node(channelAnnouncement.nodeId2, estimatedNetwork)
        val channel = Channel(channelAnnouncement.shortChannelId, node1, node2, estimatedNetwork)
        node1.channels.add(channel)
        node2.channels.add(channel)
        estimatedNetwork.nodes[channelAnnouncement.nodeId1] = node1
        estimatedNetwork.nodes[channelAnnouncement.nodeId2] = node2
        estimatedNetwork.channels[channelAnnouncement.shortChannelId] = channel
    }

    override fun onAnalyzingFinished() {
        val list = mutableListOf<Node>()
        estimatedNetwork.nodes.toList().sortedByDescending { it.second.channels.size }.forEach {
            list.add(it.second)
        }
        nodeListForDisplay.value = list
    }

    override fun onLogFileLoaded(logFile: File): String? {
        return if (!logFile.name.startsWith("channel_announcement")) {
            "This file is not a channel_announcement log."
        } else {
            null
        }
    }
}