package analyzer

import model.ground_truth_simulator_outputs.EdgesOutput
import network.Network

class EdgesOutputAnalyzer(private val groundTruthNetwork: Network) : CSVAnalyzer() {

    val edges = mutableMapOf<String, EdgesOutput>()

    override fun analyzeCSVLine(csvElements: List<String>) {
        edges[csvElements[0]] = EdgesOutput(
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
            csvElements[11],
        )
    }

    override fun onAnalyzingFinished() {
    }
}