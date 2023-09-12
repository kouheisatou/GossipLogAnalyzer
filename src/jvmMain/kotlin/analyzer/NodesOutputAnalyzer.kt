package analyzer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import edgesOutputAnalyzer
import model.ground_truth_simulator_outputs.EdgeOutput
import model.ground_truth_simulator_outputs.NodeOutput
import network.Edge
import network.Network
import network.Node

class NodesOutputAnalyzer(private val groundTruthNetwork: Network) : CSVAnalyzer() {
    val nodes = mutableMapOf<String, NodeOutput>()
    val nodesForDisplay = mutableStateOf<List<Node>?>(null)
    override fun analyzeCSVLine(csvElements: List<String>) {
        val openEdges = mutableListOf<EdgeOutput>()
        csvElements[1].split("-").forEach {
            openEdges.add(edgesOutputAnalyzer.edges[it] ?: return@forEach)
        }
        println(openEdges)
        println(csvElements[1])

        val nodeOutput = NodeOutput(
            csvElements[0],
            openEdges,
        )

        nodes[csvElements[0]] = nodeOutput

        groundTruthNetwork.nodes[nodeOutput.id] = Node(nodeOutput.id, groundTruthNetwork)
    }

    override fun onAnalyzingFinished() {
        val list = mutableListOf<Node>()
        groundTruthNetwork.nodes.toList().sortedByDescending { it.second.channels.size }.forEach {
            list.add(it.second)
        }
        nodesForDisplay.value = list
    }
}