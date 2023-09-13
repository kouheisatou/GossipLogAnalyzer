package analyzer

import androidx.compose.runtime.mutableStateOf
import model.ground_truth_simulator_outputs.ChannelOutput
import model.input_gossip_msg.ChannelUpdate
import network.Channel
import network.Direction
import network.Network
import network.Node

class ChannelsOutputAnalyzer(private val groundTruthNetwork: Network) : CSVAnalyzer() {
    val channels = mutableMapOf<String, ChannelOutput>()
    val channelsForDisplay = mutableStateOf<List<Channel>?>(null)
    override fun analyzeCSVLine(csvElements: List<String>) {
        val channelOutput = ChannelOutput(
            csvElements[0],
            csvElements[1],
            csvElements[2],
            csvElements[3],
            csvElements[4],
            csvElements[5],
            csvElements[6],
        )

        channels[csvElements[0]] = channelOutput

        val node1 = groundTruthNetwork.nodes[channelOutput.node1] ?: return
        val node2 = groundTruthNetwork.nodes[channelOutput.node2] ?: return

        val channel = Channel(channelOutput.id, node1, node2, groundTruthNetwork)

        node1.channels.add(channel)
        node2.channels.add(channel)
        groundTruthNetwork.channels[channelOutput.id] = channel
    }

    override fun onAnalyzingFinished() {
        val list = mutableListOf<Channel>()
        groundTruthNetwork.channels.toList()
            .sortedByDescending { it.second.edgeNode1ToNode2.channelUpdates.size + it.second.edgeNode2ToNode1.channelUpdates.size }
            .forEach { list.add(it.second) }
        channelsForDisplay.value = list
    }
}