package analyzer

import model.ground_truth_simulator_outputs.ChannelsOutput
import network.Channel
import network.Network

class ChannelsOutputAnalyzer(private val groundTruthNetwork: Network) : CSVAnalyzer() {
    val channels = mutableMapOf<String, ChannelsOutput>()
    override fun analyzeCSVLine(csvElements: List<String>) {
        val channelOutput = ChannelsOutput(
            csvElements[0],
            csvElements[1],
            csvElements[2],
            csvElements[3],
            csvElements[4],
            csvElements[5],
            csvElements[6],
        )

        channels[csvElements[0]] = channelOutput
    }

    override fun onAnalyzingFinished() {
    }
}