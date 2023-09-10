package analyzer

import model.ground_truth_simulator_outputs.ChannelOutput
import model.input_gossip_msg.ChannelUpdate
import network.Channel
import network.Direction
import network.Network
import network.Node

class ChannelsOutputAnalyzer(private val groundTruthNetwork: Network) : CSVAnalyzer() {
    val channels = mutableMapOf<String, ChannelOutput>()
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

        groundTruthNetwork.channels[channelOutput.id] = Channel(
            channelOutput.id,
            groundTruthNetwork.nodes[channelOutput.node1] ?: return,
            groundTruthNetwork.nodes[channelOutput.node2] ?: return,
            groundTruthNetwork,
        )
    }

    override fun onAnalyzingFinished() {
    }
}