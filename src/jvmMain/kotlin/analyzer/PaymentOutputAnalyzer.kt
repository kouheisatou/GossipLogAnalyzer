package analyzer

import edgesOutputAnalyzer
import model.ground_truth_simulator_outputs.EdgesOutput
import model.ground_truth_simulator_outputs.PaymentOutput
import network.Network

class PaymentOutputAnalyzer(private val groundTruthNetwork: Network) : CSVAnalyzer() {
    val payments = mutableMapOf<String, PaymentOutput>()

    override fun analyzeCSVLine(csvElements: List<String>) {
        val route = mutableListOf<EdgesOutput>()
        csvElements[1].split("-").forEach {
            route.add(edgesOutputAnalyzer.edges[it] ?: return@forEach)
        }

        payments[csvElements[0]] = PaymentOutput(
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            route,
            csvElements[0],
        )
    }

    override fun onAnalyzingFinished() {
    }
}