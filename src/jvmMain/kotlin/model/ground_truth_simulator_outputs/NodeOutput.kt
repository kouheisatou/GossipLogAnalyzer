package model.ground_truth_simulator_outputs

import network.Edge

data class NodeOutput(
    val id: String, val openEdges: List<EdgeOutput>
)