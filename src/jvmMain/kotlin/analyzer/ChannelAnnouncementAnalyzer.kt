package analyzer

import network.Channel
import network.Node
import androidx.compose.runtime.*
import model.input_gossip_msg.ChannelAnnouncement
import network.Network
import java.io.File

class ChannelAnnouncementAnalyzer(private val estimatedNetwork: Network) : CSVAnalyzer() {

    var nodeListForDisplay = mutableStateOf<List<Node>?>(null)

    override fun analyzeCSVLine(csvElements: List<String>) {
        val channelAnnouncement = ChannelAnnouncement(
            csvElements[0],
            csvElements[1],
            csvElements[2],
            csvElements[3],
            csvElements[4],
            csvElements[5],
            csvElements[6],
            csvElements[7],
            csvElements[8],
            csvElements[9],
            csvElements[10],
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