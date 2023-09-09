package analyzer

import edgesOutputAnalyzer
import model.ground_truth_simulator_outputs.EdgesOutput
import model.ground_truth_simulator_outputs.NodesOutput
import network.Network

class NodesOutputAnalyzer(private val groundTruthNetwork: Network) : CSVAnalyzer() {
    val nodes = mutableMapOf<String, NodesOutput>()
    override fun analyzeCSVLine(csvElements: List<String>) {
        val openEdges = mutableListOf<EdgesOutput>()
        csvElements[1].split("-").forEach {
            openEdges.add(edgesOutputAnalyzer.edges[it] ?: return@forEach)
        }

        nodes[csvElements[0]] = NodesOutput(
            csvElements[0],
            openEdges,
        )
    }

    override fun onAnalyzingFinished() {
    }
}